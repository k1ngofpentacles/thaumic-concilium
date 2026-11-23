package dev.movingcastle.thaumicconcilium.world;

import com.leclowndu93150.thaumaturge.content.entity.AbstractTaintSeed;
import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.GroveBind;
import dev.movingcastle.thaumicconcilium.entity.Vessel;
import dev.movingcastle.thaumicconcilium.entity.VesselRite;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Phase 5d cycle 4f: roots by capitals. Each Verdant Capital raises the chance (config, 0 / 25 / 50 / 75 / 100%) that the grove's strangling roots
 * (variant C, {@link GroveBind}) come up on a Taint Seed. Rolled once per seed, the roll marked on the seed:
 * <ul>
 * <li>during the fight: every Taint Seed that joins the level within the reach of a Vessel that is awake, waking or subdued (the earliest server point, an
 * entity join; no scans) rolls at the capitals standing at that moment. Seeds of an ending ({@link VesselRite#SCORE_KEY}) and seeds loaded from disk do not;
 * <li>at the ending (tiers 3 and 4): when the painter's rings reach a column holding a seed, it rolls at the job's final count (tier 4: every seed). Its roots
 * do not begin before the sight gap ({@code finale.seedGap}) after sight returns.
 * </ul>
 * No more than {@code rootOverlap} sequences play within 24 blocks of each other: a seed beyond that starts when the earliest of them ends.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class SeedRoots {
    public static final String ROLLED_KEY = "thaumicconcilium_roots_rolled";
    public static final String ROOTED_KEY = "thaumicconcilium_roots_rooted";
    private static final double NEAR = 24.0;

    /** Tests: seeds that rolled, and seeds that were rooted, since the game started. */
    public static long rolls;
    public static long rooted;

    private SeedRoots() {
    }

    // ---------------------------------------------------------------- pure rules
    /** The chance for a number of capitals (the last list entry stands in for a missing one). */
    public static double chance(int capitals) {
        return chance(TCConfig.RITE_ROOT_CHANCES.get(), capitals);
    }

    public static double chance(List<? extends Double> list, int capitals) {
        if (list.isEmpty()) {
            return 0.0;
        }
        return Mth.clamp(list.get(Mth.clamp(capitals, 0, list.size() - 1)), 0.0, 1.0);
    }

    /**
     * The game time a new sequence may start at: not before {@code notBefore}, and not while {@code overlap} others near it are still playing (then
     * when the earliest of them ends). {@code ends} are the end times of the sequences near.
     */
    public static long startTime(long now, long notBefore, List<Long> ends, int overlap) {
        long start = Math.max(now, notBefore);
        List<Long> sorted = new ArrayList<>();
        for (long e : ends) {
            if (e > start) {
                sorted.add(e);
            }
        }
        java.util.Collections.sort(sorted);
        if (sorted.size() >= Math.max(1, overlap)) {
            start = Math.max(start, sorted.get(sorted.size() - Math.max(1, overlap)));
        }
        return start;
    }

    public static boolean rolled(Entity seed) {
        return seed.getPersistentData().getBoolean(ROLLED_KEY);
    }

    // ---------------------------------------------------------------- rolling
    /**
     * Rolls a seed once at {@code capitals}; on success the roots start at {@code notBefore} or later. Returns whether it was rooted.
     */
    public static boolean roll(ServerLevel level, AbstractTaintSeed seed, int capitals, long notBefore) {
        if (rolled(seed) || !seed.isAlive()) {
            return false;
        }
        seed.getPersistentData().putBoolean(ROLLED_KEY, true);
        rolls++;
        if (level.random.nextDouble() >= chance(capitals)) {
            return false;
        }
        seed.getPersistentData().putBoolean(ROOTED_KEY, true);
        rooted++;
        List<Long> ends = new ArrayList<>();
        for (GroveBind b : level.getEntitiesOfClass(GroveBind.class, new AABB(seed.blockPosition()).inflate(NEAR))) {
            ends.add(b.start() + GroveSeedDeath.END);
        }
        long start = startTime(level.getGameTime(), notBefore, ends, TCConfig.RITE_ROOT_OVERLAP.get());
        GroveSeedDeath.begin(level, seed, start);
        return true;
    }

    // ---------------------------------------------------------------- during the fight
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.loadedFromDisk() || !(event.getEntity() instanceof AbstractTaintSeed seed)) {
            return;
        }
        if (rolled(seed) || seed.getPersistentData().contains(VesselRite.SCORE_KEY)) {
            return; // an ending's own seed, or one already rolled
        }
        // the entity is not in the level yet: roll on the next server task
        level.getServer().execute(() -> {
            if (!seed.isAlive() || seed.level() != level || rolled(seed)) {
                return;
            }
            double reach = Math.max(TCConfig.RITE_RING_MAX.get(), TCConfig.PLAYER_RANGE.get()) + 8.0;
            Vessel nearest = null;
            double best = Double.MAX_VALUE;
            for (Vessel v : level.getEntitiesOfClass(Vessel.class, new AABB(seed.blockPosition()).inflate(reach), v -> !v.isRemoved()
                    && (v.state() == Vessel.ACTIVE || v.state() == Vessel.WAKING || v.state() == Vessel.SUBDUED))) {
                double d = v.distanceToSqr(seed);
                if (d < best) {
                    best = d;
                    nearest = v;
                }
            }
            if (nearest != null) {
                roll(level, seed, nearest.cappedCount(level), 0L);
            }
        });
    }

    /**
     * The finale has decided when sight returns ({@code finalSight}); roots rolled while it was still provisional ({@code provisional}) were timed from
     * that later guess, so every sequence timed after it moves earlier by the difference (the gaps between them stay).
     */
    public static void retime(ServerLevel level, GrovePaintJob job, long provisional, long finalSight) {
        long shift = provisional - finalSight;
        if (shift <= 0L || provisional < 0L) {
            return;
        }
        long gated = provisional + TCConfig.FINALE_SEED_GAP.get();
        double r = job.radius + 16.0;
        for (GroveBind b : level.getEntitiesOfClass(GroveBind.class, new AABB(job.centre).inflate(r))) {
            if (b.start() >= gated) {
                b.setStart(b.start() - shift);
            }
        }
    }

    // ---------------------------------------------------------------- at the ending
    /**
     * The painter has painted {@code columns} (quart columns {qx, qz}) of {@code chunk}: every unrolled Taint Seed standing in one of them rolls at the job's
     * final count. Called by {@link GrovePainter}; a chunk with no seed costs one entity query.
     */
    public static void reach(ServerLevel level, GrovePaintJob job, LevelChunk chunk, List<int[]> columns) {
        if (job.tier < 3) {
            return;
        }
        AABB box = new AABB(chunk.getPos().getMinBlockX(), level.getMinBuildHeight(), chunk.getPos().getMinBlockZ(), chunk.getPos().getMaxBlockX() + 1,
                level.getMaxBuildHeight(), chunk.getPos().getMaxBlockZ() + 1);
        for (AbstractTaintSeed seed : level.getEntitiesOfClass(AbstractTaintSeed.class, box)) {
            if (rolled(seed) || seed.getUUID().equals(job.seed)) {
                continue; // a legacy job's own doomed seed is killed by the painter's last ring, not rolled
            }
            int qx = seed.blockPosition().getX() >> 2;
            int qz = seed.blockPosition().getZ() >> 2;
            for (int[] c : columns) {
                if (c[0] == qx && c[1] == qz) {
                    long notBefore = job.sightAt >= 0L ? job.sightAt + TCConfig.FINALE_SEED_GAP.get() : 0L;
                    roll(level, seed, job.tier, notBefore);
                    break;
                }
            }
        }
    }
}
