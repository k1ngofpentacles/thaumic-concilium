package dev.movingcastle.thaumicconcilium.world;

import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.compat.DynamicTreesLandmarks;
import dev.movingcastle.thaumicconcilium.compat.DynamicTreesSaplings;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

/**
 * Phase 5d cycle 4e (memo {@code docs/design/5d-finale-growth.md} sections 2 and 3): the grove's trees grow in front of the player. No grove tree is
 * placed whole: a landmark is planted at stage 1 of 8 ({@link LandmarkShapes#stage}) and a Thaumaturge sapling is a Dynamic Trees sapling that is
 * turned into its young tree and given bounded accelerated growth pulses. How far each goes falls off exponentially with the distance from the hex
 * ({@link #g}): an apex forest, saplings at the edge. A landmark left young keeps maturing one stage per game day until whole.
 *
 * <p>The list lives on the job ({@link GrovePaintJob#growth}, saved). Nothing here runs when no job has an entry: the driver
 * ({@code VesselAftermath.tick}) looks at the jobs once every {@code growth.stepTicks} ticks and each job sleeps until its earliest entry is due.
 * Only {@code compat/DynamicTrees*} name Dynamic Trees; these entries exist only when it is loaded.
 */
public final class GroveGrowth {
    public static final int LANDMARK = 0;
    public static final int SAPLING = 1;
    /** Entry fields. */
    public static final int KIND = 0;
    public static final int POS = 1;
    public static final int DONE = 2;
    public static final int TARGET = 3;
    public static final int PACKED = 4;
    public static final int NEXT = 5;

    private static final int TRANSITIONED = 1 << 16;
    private static final int FAILS_SHIFT = 20;

    /** Tests: growth steps that did work, and landmark stages and sapling pulses applied since the game started. */
    public static long stepsRun;
    public static long stagesApplied;
    public static long pulsesApplied;

    private GroveGrowth() {
    }

    // ---------------------------------------------------------------- pure rules
    /** The fraction of its full growth a tree at distance {@code d} from the hex gets, for a grove of radius {@code radius}: exp(-falloff * d / R). */
    public static double g(double d, double radius) {
        return Math.exp(-TCConfig.GROW_FALLOFF.get() * Math.max(0.0, d) / Math.max(1.0, radius));
    }

    /** A landmark's target stage (1 to 8) for the fraction {@code g}. */
    public static int targetStage(double g) {
        return Math.max(1, Math.min(LandmarkShapes.STAGES, (int) Math.round(LandmarkShapes.STAGES * g)));
    }

    /** A sapling's target pulses for the fraction {@code g}. */
    public static int targetPulses(double g) {
        return Math.max(1, (int) Math.round(TCConfig.GROW_APEX_PULSES.get() * g));
    }

    public static double distance(GrovePaintJob job, int x, int z) {
        double dx = x + 0.5 - (job.centre.getX() + 0.5);
        double dz = z + 0.5 - (job.centre.getZ() + 0.5);
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static long packLandmark(boolean silver, int variant, Direction facing) {
        return (silver ? 1L : 0L) | ((long) variant << 1) | ((long) facing.get2DDataValue() << 8);
    }

    // ---------------------------------------------------------------- entries
    /**
     * Plants the landmark of {@code pick} on {@code soil} at stage 1 and lists it for its later stages. False when the first branch did not stand
     * (nothing is listed).
     */
    public static boolean plantLandmark(ServerLevel level, GrovePaintJob job, BlockPos soil, boolean silver, LandmarkShapes.Pick pick) {
        if (!DynamicTreesLandmarks.growStage(level, soil, silver, pick.shape().code(), pick.facing(), pick.shape().trunkCap(), 1)) {
            return false;
        }
        double g = g(distance(job, soil.getX(), soil.getZ()), job.radius);
        long now = level.getGameTime();
        job.growth.add(new long[] {LANDMARK, soil.asLong(), 1L, targetStage(g), packLandmark(silver, pick.variant(), pick.facing()), now + TCConfig.GROW_STAGE_TICKS.get()});
        job.growthWake = 0L;
        return true;
    }

    /** Lists a Dynamic Trees sapling the grove planted for its pulses. */
    public static void addSapling(ServerLevel level, GrovePaintJob job, BlockPos pos, boolean silver) {
        double g = g(distance(job, pos.getX(), pos.getZ()), job.radius);
        job.growth.add(new long[] {SAPLING, pos.asLong(), 0L, targetPulses(g), silver ? 1L : 0L, level.getGameTime()});
        job.growthWake = 0L;
    }

    // ---------------------------------------------------------------- the driver
    /** Whether any job in the list has entries (the driver's cheap test). */
    public static boolean any(List<GrovePaintJob> jobs) {
        for (GrovePaintJob j : jobs) {
            if (!j.growth.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * One growth step of one job: every entry that is due gets one stage or one pulse, round robin, within {@code growth.budgetMs}. The job as a
     * whole waits for {@code growth.startDelay} ticks after sight returns. Returns whether anything changed (the record is then dirty).
     */
    public static boolean step(ServerLevel level, GrovePaintJob job) {
        if (job.growth.isEmpty()) {
            return false;
        }
        long now = level.getGameTime();
        if (now < job.growthWake) {
            return false;
        }
        if (job.sightAt >= 0L && now < job.sightAt + TCConfig.GROW_START_DELAY.get()) {
            job.growthWake = job.sightAt + TCConfig.GROW_START_DELAY.get();
            return false;
        }
        stepsRun++;
        long deadline = System.nanoTime() + TCConfig.GROW_BUDGET_MS.get() * 1_000_000L;
        int n = job.growth.size();
        int start = Math.floorMod(job.growthCursor, n);
        List<long[]> gone = new ArrayList<>();
        long wake = Long.MAX_VALUE;
        boolean changed = false;
        boolean out = false;
        for (int k = 0; k < n; k++) {
            long[] e = job.growth.get((start + k) % n);
            if (out) {
                wake = now; // not reached this step: next step
                continue;
            }
            if (e[NEXT] <= now) {
                if (k > 0 && System.nanoTime() > deadline) {
                    out = true;
                    job.growthCursor = (start + k) % n;
                    wake = now;
                    continue;
                }
                int result = e[KIND] == LANDMARK ? landmark(level, job, e, now) : sapling(level, job, e, now);
                changed = true;
                if (result < 0) {
                    gone.add(e);
                    continue;
                }
            }
            wake = Math.min(wake, e[NEXT]);
        }
        if (!out) {
            job.growthCursor = 0;
        }
        job.growth.removeAll(gone);
        job.growthWake = job.growth.isEmpty() ? 0L : Math.max(wake, now + 1);
        return changed;
    }

    private static boolean loaded(ServerLevel level, BlockPos pos, int reach) {
        for (int cx = (pos.getX() - reach) >> 4; cx <= (pos.getX() + reach) >> 4; cx++) {
            for (int cz = (pos.getZ() - reach) >> 4; cz <= (pos.getZ() + reach) >> 4; cz++) {
                if (level.getChunkSource().getChunkNow(cx, cz) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /** A player stands in the trunk's column (where the next wood goes): the stage waits for them to step out. */
    private static boolean playerInTrunk(ServerLevel level, BlockPos soil) {
        AABB box = new AABB(soil.getX() - 1.5, soil.getY(), soil.getZ() - 1.5, soil.getX() + 2.5, soil.getY() + 32.0, soil.getZ() + 2.5);
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && p.getBoundingBox().intersects(box)) {
                return true;
            }
        }
        return false;
    }

    /** One landmark visit: the next stage. Returns -1 to drop the entry. */
    private static int landmark(ServerLevel level, GrovePaintJob job, long[] e, long now) {
        BlockPos soil = BlockPos.of(e[POS]);
        int packed = (int) e[PACKED];
        boolean silver = (packed & 1) != 0;
        List<LandmarkShapes.Shape> shapes = LandmarkShapes.shapes(silver);
        int variant = (packed >> 1) & 63;
        if (variant >= shapes.size()) {
            return -1;
        }
        if (!loaded(level, soil, 13)) {
            e[NEXT] = now + 200;
            return 0;
        }
        if (!DynamicTreesLandmarks.stands(level, soil, silver)) {
            return -1; // cut down or never stood
        }
        if (playerInTrunk(level, soil)) {
            e[NEXT] = now + 10;
            return 0;
        }
        LandmarkShapes.Shape shape = shapes.get(variant);
        int stage = (int) e[DONE] + 1;
        if (!DynamicTreesLandmarks.growStage(level, soil, silver, shape.code(), Direction.from2DDataValue((packed >> 8) & 3), shape.trunkCap(), stage)) {
            return -1;
        }
        stagesApplied++;
        e[DONE] = stage;
        if (stage >= LandmarkShapes.STAGES) {
            return -1; // whole
        }
        if (stage >= e[TARGET]) {
            e[TARGET] = stage + 1; // left young by the falloff: one more stage a game day
            e[NEXT] = now + TCConfig.GROW_DAY_TICKS.get();
        } else {
            e[NEXT] = now + TCConfig.GROW_STAGE_TICKS.get();
        }
        return 0;
    }

    /** One sapling visit: the transition into its tree, then one pulse a visit up to the target. Returns -1 to drop the entry. */
    private static int sapling(ServerLevel level, GrovePaintJob job, long[] e, long now) {
        BlockPos pos = BlockPos.of(e[POS]);
        int packed = (int) e[PACKED];
        if (!loaded(level, pos, 8)) {
            e[NEXT] = now + 200;
            return 0;
        }
        if ((packed & TRANSITIONED) == 0) {
            if (!DynamicTreesSaplings.isSapling(level, pos)) {
                return -1; // picked, cut, or grown by itself already
            }
            if (!DynamicTreesSaplings.transition(level, pos)) {
                int fails = (packed >> FAILS_SHIFT) + 1;
                if (fails >= 5) {
                    return -1;
                }
                e[PACKED] = (packed & ((1 << FAILS_SHIFT) - 1)) | ((long) fails << FAILS_SHIFT);
                e[NEXT] = now + 100;
                return 0;
            }
            e[PACKED] = packed | TRANSITIONED;
            e[NEXT] = now + 1;
            return 0;
        }
        boolean viable = DynamicTreesSaplings.pulse(level, pos.below(), e[DONE] % 8 == 7);
        e[DONE]++;
        pulsesApplied++;
        if (!viable || e[DONE] >= e[TARGET]) {
            return -1; // an ordinary tree from here on
        }
        e[NEXT] = now + 1;
        return 0;
    }
}
