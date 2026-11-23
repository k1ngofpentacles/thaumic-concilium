package dev.movingcastle.thaumicconcilium.world;

import com.leclowndu93150.thaumaturge.api.taint.ITaintBlock;
import com.leclowndu93150.thaumaturge.api.taint.TaintApi;
import com.leclowndu93150.thaumaturge.content.entity.AbstractTaintSeed;
import com.leclowndu93150.thaumaturge.content.taint.spread.TaintFeeding;
import com.leclowndu93150.thaumaturge.content.taint.spread.TaintOrigins;
import com.leclowndu93150.thaumaturge.content.taint.spread.TaintSprouting;
import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.VesselCrater;
import dev.movingcastle.thaumicconcilium.entity.VesselEndings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Phase 5d cycle 2a: what a finished Vessel's seed is still doing. Per-level saved data holding one {@link Scar} per seed that needs a driver
 * (accelerated, bound, doomed; a starving seed needs none) and the driver itself: a beat every 20 ticks, nothing between, and nothing at all
 * (not even a lookup) when the level holds no scars and no open painting jobs. A scar is dropped when its seed is dead, or when its work is
 * done. Cycle 2b adds the grove painting jobs ({@link GrovePaintJob}, stepped by {@link GrovePainter}) beside the scars.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class VesselAftermath extends SavedData {
    public static final String NAME = "thaumicconcilium_vessel_aftermath";
    /** Ticks between beats. */
    public static final int BEAT = 20;
    /** Child seeds the accelerated ending plants. */
    public static final int CHILDREN = 3;

    /** One seed's record. {@code children} counts the child seeds already planted (tier 0). */
    public static final class Scar {
        public final UUID seed;
        public final VesselEndings.Kind kind;
        public final int tier;
        public final BlockPos centre;
        public final double craterRadius;
        public final long craterSeed;
        public final long born;
        public int children;

        public Scar(UUID seed, VesselEndings.Kind kind, int tier, BlockPos centre, double craterRadius, long craterSeed, long born) {
            this.seed = seed;
            this.kind = kind;
            this.tier = tier;
            this.centre = centre;
            this.craterRadius = craterRadius;
            this.craterSeed = craterSeed;
            this.born = born;
        }

        VesselCrater crater() {
            return new VesselCrater(this.centre, this.craterRadius, this.craterSeed);
        }
    }

    private final List<Scar> scars = new ArrayList<>();

    // ---------------------------------------------------------------- storage
    private static final Map<ResourceKey<Level>, VesselAftermath> CACHE = new HashMap<>();
    private static final Set<ResourceKey<Level>> CHECKED = new HashSet<>();

    public static SavedData.Factory<VesselAftermath> factory() {
        return new SavedData.Factory<>(VesselAftermath::new, VesselAftermath::load, null);
    }

    /** The level's record, created if there is none (used when a scar is added). */
    public static VesselAftermath get(ServerLevel level) {
        VesselAftermath data = level.getDataStorage().computeIfAbsent(factory(), NAME);
        CACHE.put(level.dimension(), data);
        CHECKED.add(level.dimension());
        return data;
    }

    /** The level's record if one exists: looked up in storage once, never created. */
    /** The level's record if one exists (never created, a lookup in storage once): for callers that must stay free when nothing exists. */
    @Nullable
    public static VesselAftermath peek(ServerLevel level) {
        return cached(level);
    }

    @Nullable
    private static VesselAftermath cached(ServerLevel level) {
        ResourceKey<Level> key = level.dimension();
        VesselAftermath data = CACHE.get(key);
        if (data == null && CHECKED.add(key)) {
            data = level.getDataStorage().get(factory(), NAME);
            if (data != null) {
                CACHE.put(key, data);
            }
        }
        return data;
    }

    public List<Scar> scars() {
        return this.scars;
    }

    public void add(Scar scar) {
        this.scars.add(scar);
        this.setDirty();
    }

    private final List<GrovePaintJob> jobs = new ArrayList<>();

    /** The grove painting jobs, open and finished (a finished one stays as the grove's record). */
    public List<GrovePaintJob> jobs() {
        return this.jobs;
    }

    public void addJob(GrovePaintJob job) {
        this.jobs.add(job);
        this.setDirty();
    }

    private boolean anyOpenJob() {
        for (GrovePaintJob j : this.jobs) {
            if (!j.done) {
                return true;
            }
        }
        return false;
    }

    public static VesselAftermath load(CompoundTag tag, HolderLookup.Provider registries) {
        VesselAftermath data = new VesselAftermath();
        ListTag list = tag.getList("scars", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            VesselEndings.Kind kind;
            try {
                kind = VesselEndings.Kind.valueOf(c.getString("kind"));
            } catch (IllegalArgumentException e) {
                continue; // a kind from a newer version: ignore the record
            }
            Scar s = new Scar(c.getUUID("seed"), kind, c.getInt("tier"), BlockPos.of(c.getLong("centre")), c.getDouble("radius"),
                    c.getLong("craterSeed"), c.getLong("born"));
            s.children = c.getInt("children");
            data.scars.add(s);
        }
        ListTag jobList = tag.getList("jobs", 10);
        for (int i = 0; i < jobList.size(); i++) {
            data.jobs.add(GrovePaintJob.load(jobList.getCompound(i)));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Scar s : this.scars) {
            CompoundTag c = new CompoundTag();
            c.putUUID("seed", s.seed);
            c.putString("kind", s.kind.name());
            c.putInt("tier", s.tier);
            c.putLong("centre", s.centre.asLong());
            c.putDouble("radius", s.craterRadius);
            c.putLong("craterSeed", s.craterSeed);
            c.putLong("born", s.born);
            c.putInt("children", s.children);
            list.add(c);
        }
        tag.put("scars", list);
        ListTag jobList = new ListTag();
        for (GrovePaintJob j : this.jobs) {
            jobList.add(j.save());
        }
        tag.put("jobs", jobList);
        return tag;
    }

    // ---------------------------------------------------------------- pure rules
    /** How far (0 to 1) a spread of {@code total} ticks has got after {@code age}. */
    public static double fraction(long age, int total) {
        return Mth.clamp(age / (double) Math.max(1, total), 0.0, 1.0);
    }

    /**
     * The distance of a child seed from the hex in a direction: two blocks outside the rim, but never inside Thaumaturge's own seed spacing
     * ({@code TaintSprouting.hasRoom} refuses a seed within 0.8 x taintSpreadArea of another) plus a margin, so the blight jumps beyond the
     * crater whatever the config says.
     */
    public static double childDistance(VesselCrater crater, double angle, double claimRadius, double margin) {
        // hasRoom tests a BOX (the seed's bounding box inflated by 0.8 x claim on every axis), not a disc: the parent clears a child when either
        // coordinate differs by more than that, so along a diagonal the child must stand farther out (found by the first run at reach 32)
        double axis = Math.max(Math.abs(Math.cos(angle)), Math.abs(Math.sin(angle)));
        return Math.max(crater.rimAt(angle) + 2.0, (SPACING_FACTOR * claimRadius + margin) / axis);
    }

    /** Thaumaturge's own factor in {@code TaintSprouting.hasRoom} (read from its source; not a config there). */
    public static final double SPACING_FACTOR = 0.8;

    /** Angle offsets (degrees) tried when a child's spot is refused: the planned direction first, then four nearby ones. */
    public static final double[] CHILD_TRIES = {0.0, 8.0, -8.0, 16.0, -16.0};

    /** The direction of the i-th child seed: 120 degrees apart, the first at an angle fixed by the crater's seed. */
    public static double childAngle(long craterSeed, int index) {
        return (craterSeed & 1023L) / 1023.0 * Mth.TWO_PI + index * Mth.TWO_PI / CHILDREN;
    }

    // ---------------------------------------------------------------- the driver
    /** Scar beats run since the game started (the tests read it to show that an empty level does no work). */
    public static long beatsRun;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            tick(level, level.getGameTime());
        }
    }

    /** The driver's whole tick: a beat on every {@link #BEAT}th tick when the level holds scars, otherwise nothing. */
    public static void tick(ServerLevel level, long now) {
        boolean beat = now % BEAT == 0;
        boolean growStep = now % TCConfig.GROW_STEP_TICKS.get() == 0;
        if (!beat && !growStep) {
            return;
        }
        VesselAftermath data = cached(level);
        if (data == null) {
            return;
        }
        if (growStep && !data.jobs.isEmpty()) {
            // cycle 4e: the trees growing on screen (a job sleeps until its earliest entry is due)
            boolean changed = false;
            for (GrovePaintJob job : data.jobs) {
                if (!job.growth.isEmpty()) {
                    changed |= GroveGrowth.step(level, job);
                }
            }
            if (changed) {
                data.setDirty();
            }
        }
        if (!beat) {
            return;
        }
        // the painter steps first, so a doomed seed killed by the last ring is already dead when its scar's beat looks
        if (!data.jobs.isEmpty() && data.anyOpenJob()) {
            GrovePainter.stepAll(level, data.jobs);
        }
        if (!data.scars.isEmpty()) {
            data.beatAll(level, now);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            CACHE.remove(level.dimension());
            CHECKED.remove(level.dimension());
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        CACHE.clear();
        CHECKED.clear();
    }

    /** One beat for every scar; the ones that are finished are dropped. */
    public void beatAll(ServerLevel level, long now) {
        boolean changed = false;
        Iterator<Scar> it = this.scars.iterator();
        while (it.hasNext()) {
            Scar s = it.next();
            beatsRun++;
            int before = s.children;
            if (!beat(level, s, now)) {
                it.remove();
                changed = true;
            } else if (s.children != before) {
                changed = true;
            }
        }
        if (changed) {
            this.setDirty();
        }
    }

    /** The scar's seed if it is loaded and alive. */
    @Nullable
    private static AbstractTaintSeed seedOf(ServerLevel level, Scar s) {
        Entity e = level.getEntity(s.seed);
        return e instanceof AbstractTaintSeed seed && seed.isAlive() ? seed : null;
    }

    /** One beat for one scar. Returns whether to keep the record. A seed whose chunk is not loaded is waited for, never forced. */
    public static boolean beat(ServerLevel level, Scar s, long now) {
        AbstractTaintSeed seed = seedOf(level, s);
        if (seed == null) {
            Entity e = level.getEntity(s.seed);
            if (e == null && !level.areEntitiesLoaded(ChunkPos.asLong(s.centre))) {
                return true; // its chunk is not loaded: it may still be there
            }
            return false; // dead or gone
        }
        long age = Math.max(0L, now - s.born);
        switch (s.kind) {
            case ACCELERATED -> {
                return accelerated(level, s, age);
            }
            case BOUND -> bound(level, s, seed, age, now);
            case DOOMED -> seed.setTaintReserve(0.0F); // it dies when the painter's last ring closes (GrovePainter.finishSeed), not by a clock
            default -> {
                return false; // plain and starving seeds need no driver
            }
        }
        return true;
    }

    private static BlockPos surface(ServerLevel level, int x, int z) {
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    /** Tier 0: a fibre storm in a disc growing from the rim outward, and three child seeds just outside the rim. */
    private static boolean accelerated(ServerLevel level, Scar s, long age) {
        int total = TCConfig.RITE_ACCEL_TICKS.get();
        if (age > total) {
            return false; // the storm is over (a child that never found room is given up)
        }
        VesselCrater crater = s.crater();
        double frac = fraction(age, total);
        double reach = TCConfig.RITE_ACCEL_REACH.get() * frac;
        for (int k = 0; k < TCConfig.RITE_ACCEL_SPREADS.get(); k++) {
            double angle = level.random.nextDouble() * Mth.TWO_PI;
            double disc = crater.rimAt(angle) + reach;
            double dist = Math.max(2.0, disc * Math.sqrt(level.random.nextDouble()));
            int x = s.centre.getX() + (int) Math.round(Math.cos(angle) * dist);
            int z = s.centre.getZ() + (int) Math.round(Math.sin(angle) * dist);
            if (level.hasChunkAt(new BlockPos(x, s.centre.getY(), z))) {
                TaintApi.spreadFibres(level, surface(level, x, z), true);
            }
        }
        if (s.children < CHILDREN && age >= TCConfig.RITE_ACCEL_CHILD_FIRST.get() + (long) TCConfig.RITE_ACCEL_CHILD_EVERY.get() * s.children) {
            double planned = childAngle(s.craterSeed, s.children);
            double claim = TaintOrigins.claimRadius();
            boolean placed = false;
            boolean waiting = false;
            for (double tryDeg : CHILD_TRIES) {
                double angle = planned + Math.toRadians(tryDeg);
                double dist = childDistance(crater, angle, claim, TCConfig.RITE_ACCEL_CHILD_MARGIN.get());
                int x = s.centre.getX() + (int) Math.round(Math.cos(angle) * dist);
                int z = s.centre.getZ() + (int) Math.round(Math.sin(angle) * dist);
                if (!level.hasChunkAt(new BlockPos(x, s.centre.getY(), z))) {
                    waiting = true; // not loaded: try again on a later beat, nothing is given up
                    break;
                }
                if (TaintSprouting.plant(level, surface(level, x, z), TaintFeeding.reserveCap() / 2.0F)) {
                    placed = true;
                    break;
                }
            }
            if (placed) {
                s.children++;
            } else if (!waiting) {
                ThaumicConcilium.LOG.warn("Vessel ending at {}: child seed {} was refused at every one of {} spots, skipped", s.centre, s.children + 1, CHILD_TRIES.length);
                s.children++;
            }
        }
        return true;
    }

    /** Taint blocks decayed by the tier 1 hold since the game started (the tests read it). */
    public static long heldDecays;
    /** Line-holding passes run since the game started (the tests read it). */
    public static long holdPasses;

    /**
     * Decays every {@code ITaintBlock} (surface +- depth) in a few random quart columns (4x4 blocks, the biome grid) of the annulus from the rim to
     * the seed's claim radius, counting only blocks more than one block beyond the rim, so the fibres we spread inside are never touched.
     */
    private static void holdLine(ServerLevel level, Scar s, VesselCrater crater) {
        holdPasses++;
        double claim = TaintOrigins.claimRadius();
        int depth = TCConfig.RITE_BOUND_HOLD_DEPTH.get();
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (int k = 0; k < TCConfig.RITE_BOUND_HOLD_COLUMNS.get(); k++) {
            double angle = level.random.nextDouble() * Mth.TWO_PI;
            double rim = crater.rimAt(angle);
            double dist = rim + level.random.nextDouble() * Math.max(0.0, claim - rim);
            int cx = s.centre.getX() + (int) Math.round(Math.cos(angle) * dist);
            int cz = s.centre.getZ() + (int) Math.round(Math.sin(angle) * dist);
            if (!level.hasChunkAt(new BlockPos(cx, s.centre.getY(), cz))) {
                continue;
            }
            int qx = cx >> 2;
            int qz = cz >> 2;
            for (int bx = 0; bx < 4; bx++) {
                for (int bz = 0; bz < 4; bz++) {
                    int x = qx * 4 + bx;
                    int z = qz * 4 + bz;
                    double dx = x - s.centre.getX();
                    double dz = z - s.centre.getZ();
                    if (Math.sqrt(dx * dx + dz * dz) <= crater.rimAt(Math.atan2(dz, dx)) + 1.0) {
                        continue;
                    }
                    int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    for (int y = Math.max(level.getMinBuildHeight(), h - depth); y <= Math.min(level.getMaxBuildHeight() - 1, h + depth); y++) {
                        BlockState state = level.getBlockState(probe.set(x, y, z));
                        if (state.getBlock() instanceof ITaintBlock taint) {
                            taint.decay(level, probe.immutable(), state);
                            heldDecays++;
                        }
                    }
                }
            }
        }
    }

    /** Tier 1: fibres spread slowly out to the rim and no further; the seed's reserve is held low so it grows only once a beat. */
    private static void bound(ServerLevel level, Scar s, AbstractTaintSeed seed, long age, long now) {
        float max = (float) (TCConfig.RITE_BOUND_RESERVE.get() * TaintFeeding.reserveCap());
        if (seed.getTaintReserve() > max) {
            seed.setTaintReserve(max);
        }
        int total = TCConfig.RITE_BOUND_SPREAD_TICKS.get();
        VesselCrater crater = s.crater();
        // holding the line: every few beats, taint blocks beyond the rim and within the seed's reach are decayed, so the blight stays inside the crater
        if (now % TCConfig.RITE_BOUND_HOLD_TICKS.get() == 0) {
            holdLine(level, s, crater);
        }
        if (age > total) {
            return; // the fibres have reached the rim; the hold above goes on while the seed lives
        }
        double frac = fraction(age, total);
        for (int k = 0; k < TCConfig.RITE_BOUND_SPREAD_PER_BEAT.get(); k++) {
            double angle = level.random.nextDouble() * Mth.TWO_PI;
            // a band at the front, never beyond two blocks inside the rim (a fibre may land one block from the point)
            double limit = crater.rimAt(angle) - 2.0;
            double dist = Mth.clamp(limit * frac - level.random.nextDouble() * 4.0, 2.0, Math.max(2.0, limit));
            int x = s.centre.getX() + (int) Math.round(Math.cos(angle) * dist);
            int z = s.centre.getZ() + (int) Math.round(Math.sin(angle) * dist);
            if (level.hasChunkAt(new BlockPos(x, s.centre.getY(), z))) {
                TaintApi.spreadFibres(level, surface(level, x, z), true);
            }
        }
    }
}
