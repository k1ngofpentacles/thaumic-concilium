package dev.movingcastle.thaumicconcilium.world;

import dev.movingcastle.thaumicconcilium.entity.GroveBind;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

/**
 * How the grove kills the doomed Taint Seed (phase 5d cycle 4b; the user's pick, variant C of docs/design/5d-look.md): when the
 * painter's last ring closes, eight roots erupt round the seed and lock over it, it struggles (five lashes, the seed's own strike
 * animation), leaves burst through, it dies and the mound sinks. Twelve seconds in all.
 *
 * <p>The timeline is in game time from a saved start. The painter (which steps once a second, and only while a job is open) starts
 * it, waits for its end and then goes on to the seed's column and the pond. In between, a {@link GroveBind} entity of ours, which
 * exists only for these twelve seconds, fires the moments that need the exact tick and is what the client draws. Nothing here runs
 * outside a sequence.
 *
 * <p>The seed is Thaumaturge's entity; only public methods are used on it: {@code setNoAi}, {@code setInvulnerable},
 * {@code Level.broadcastEntityEvent(seed, 16)} (its own lash), {@code kill}.
 */
public final class GroveSeedDeath {
    /** Roots finish locking over the seed. */
    public static final int LOCKED = 60;
    /** The seed lashes against the roots at these ticks. */
    public static final int[] LASHES = {70, 95, 115, 130, 142};
    /** Leaf clusters burst through from here, one every {@link #BURST_EVERY} ticks, {@link #BURSTS} of them. */
    public static final int BURST_START = 150;
    public static final int BURST_EVERY = 8;
    public static final int BURSTS = 6;
    /** The seed dies; the flower crown opens and the mound starts to sink. */
    public static final int KILL = 200;
    /** The sequence is over. */
    public static final int END = 240;
    /** The seed's entity event for its strike animation (Thaumaturge's {@code AbstractRootedTaint.handleEntityEvent}). */
    public static final byte LASH_EVENT = 16;

    /** Tests: the sequence's length in ticks in place of {@link #END}; 0 kills the seed at once, as before cycle 4b. */
    @Nullable
    public static Integer durationOverride;

    private GroveSeedDeath() {
    }

    /** Whether the death may begin: not before {@code finale.seedGap} ticks after sight returned; a job the finale never touched has no gate. */
    public static boolean seedMayDie(GrovePaintJob job, long now) {
        return job.sightAt < 0L || now >= job.sightAt + dev.movingcastle.thaumicconcilium.TCConfig.FINALE_SEED_GAP.get();
    }

    public static int duration() {
        return durationOverride != null ? durationOverride : END;
    }

    /**
     * The painter's step once its rings have closed. {@code seed} is the doomed seed, or null when it is gone (its chunk is loaded).
     * Returns true when the seed is dead and the sequence is over, so the painter may go on.
     */
    public static boolean step(ServerLevel level, GrovePaintJob job, @Nullable Entity seed) {
        long now = level.getGameTime();
        boolean alive = seed != null && seed.isAlive();
        if (job.seedDeathStart < 0L) {
            if (!alive) {
                return true; // already dead or gone: nothing to show
            }
            if (!seedMayDie(job, now)) {
                return false; // sight has not been back long enough (cycle 4e: the finale's gap)
            }
            if (duration() <= 0) {
                seed.kill();
                return true;
            }
            job.seedDeathStart = now;
            begin(level, seed, now);
            return false;
        }
        if (now - job.seedDeathStart < duration()) {
            return false;
        }
        // the end; also where a sequence lands whose chunk was unloaded part of the way through
        if (alive) {
            seed.setInvulnerable(false);
            seed.kill();
        }
        AABB box = new AABB(job.centre).inflate(8.0);
        for (GroveBind bind : level.getEntitiesOfClass(GroveBind.class, box)) {
            bind.discard();
        }
        return true;
    }

    /** Starts the sequence on a live seed: it stops striking, cannot die early (a doomed seed starves), and the bind appears round it. */
    public static GroveBind begin(ServerLevel level, Entity seed, long now) {
        if (seed instanceof Mob mob) {
            mob.setNoAi(true);
        }
        seed.setInvulnerable(true);
        GroveBind bind = TCEntities.GROVE_BIND.get().create(level);
        bind.bind(seed, now);
        level.addFreshEntity(bind);
        return bind;
    }
}
