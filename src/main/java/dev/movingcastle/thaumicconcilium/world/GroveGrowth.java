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
    /** 5d trees C: one of the grove's own trees (a hero or a finale tree) grown on its template in small steps. */
    public static final int TEMPLATE = 2;
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
    public static long templatesPlanted;
    public static long templateSteps;
    public static long templateNanos;

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

    // ---------------------------------------------------------------- 5d trees C: the grove's own trees, small steps
    /** Template entry PACKED: kind (0 hero, 1 finale) | silver << 2 | yLimit << 3 (7 bits) | step depth << 10 (4 bits) | interval << 14 (26 bits) | wave started << 40. */
    static long packTemplate(GroveHill.Kind kind, boolean silver, int yLimit, int stepDepth, int interval) {
        return (kind == GroveHill.Kind.HERO ? 0L : 1L) | (silver ? 4L : 0L) | ((long) (yLimit & 127) << 3) | ((long) (stepDepth & 15) << 10) | ((long) interval << 14);
    }

    /**
     * Plants one of the grove's own trees on {@code soil} (the block under the trunk): its template is {@code DynamicTreesGrove.pick} at this
     * column (the same one natural growth follows), cut at the canopy {@code canopy}. The first small step is grown at once (so the spacing
     * rules see wood here); the rest follow every {@code interval} ticks from when sight returns, all finishing in {@code growth.finaleSeconds}.
     * Heroes step one path length at a time, the rest two. False when the first branch did not stand (nothing is listed).
     */
    public static boolean plantTemplate(ServerLevel level, GrovePaintJob job, BlockPos soil, GroveHill.Kind kind, boolean silver, int canopy) {
        dev.movingcastle.thaumicconcilium.compat.DynamicTreesGrove.Pick pick = dev.movingcastle.thaumicconcilium.compat.DynamicTreesGrove.pick(kind, silver, soil.getX(), soil.getZ());
        if (pick == null) {
            return false;
        }
        int yLimit = GroveHill.branchLimit(canopy);
        int target = LandmarkShapes.depthUnder(LandmarkShapes.decode(pick.code()), yLimit);
        int stepDepth = kind == GroveHill.Kind.HERO ? 1 : 2;
        int steps = Math.max(1, (target + stepDepth - 1) / stepDepth);
        int interval = Math.max(TCConfig.GROW_STEP_TICKS.get(), TCConfig.FINALE_GROW_SECONDS.get() * 20 / steps);
        if (kind != GroveHill.Kind.HERO) {
            // a finale tree's first step waits for the growth driver (its time budget), not the painter's step: listed with nothing grown yet
            templatesPlanted++;
            job.growth.add(new long[] {TEMPLATE, soil.asLong(), 0L, target, packTemplate(kind, silver, yLimit, stepDepth, interval), level.getGameTime()});
            job.growthWake = 0L;
            return true;
        }
        int first = Math.min(target, stepDepth);
        if (!DynamicTreesLandmarks.growCut(level, soil, dev.movingcastle.thaumicconcilium.compat.DynamicTreesGrove.species(kind, silver), silver, pick.code(),
                pick.facing(), pick.trunkCap(), first, yLimit, true, first >= target)) {
            return false;
        }
        templatesPlanted++;
        if (first >= target) {
            DynamicTreesLandmarks.setFertility(level, soil, 15);
            return true;
        }
        job.growth.add(new long[] {TEMPLATE, soil.asLong(), first, target, packTemplate(kind, silver, yLimit, stepDepth, interval), level.getGameTime() + interval});
        job.growthWake = 0L;
        return true;
    }

    private static final long WAVE_STARTED = 1L << 40;

    /**
     * Ticks after the wave's clock (sight returning) a tree starts: heroes and the crater's inside at once; beyond the heroes' ring, the
     * meadow's ripple time to its ring ({@code grove.ringSeconds} per 4 blocks) plus {@code growth.waveLagSeconds}.
     */
    public static int waveDelay(GrovePaintJob job, BlockPos soil, GroveHill.Kind kind) {
        if (kind == GroveHill.Kind.HERO) {
            return 0;
        }
        double past = distance(job, soil.getX(), soil.getZ()) - GroveHill.params(job).ring();
        if (past <= 0) {
            return 0;
        }
        return (int) Math.round(past / 4.0 * TCConfig.RING_SECONDS.get() * 20) + TCConfig.WAVE_LAG_SECONDS.get() * 20;
    }

    /** Tests: null = look for players; true or false = every tree is (not) watched. */
    public static Boolean watchOverride;

    /** Whether any player is near enough to watch this tree grow ({@code growth.watchRange}, 0 = always). */
    static boolean watched(ServerLevel level, BlockPos soil) {
        if (watchOverride != null) {
            return watchOverride;
        }
        int range = TCConfig.WATCH_RANGE.get();
        if (range == 0) {
            return true;
        }
        if (range < 0) {
            range = level.getServer().getPlayerList().getViewDistance() * 16; // whatever a player could see
        }
        for (ServerPlayer p : level.players()) {
            double dx = p.getX() - soil.getX();
            double dz = p.getZ() - soil.getZ();
            if (!p.isSpectator() && dx * dx + dz * dz <= (double) range * range) {
                return true;
            }
        }
        return false;
    }

    /** Whether a tree listed on this job but not grown yet stands within {@code reach} of {@code at} (the spacing rules must see it). */
    public static boolean plannedNear(GrovePaintJob job, BlockPos at, int reach) {
        for (long[] e : job.growth) {
            if (e[KIND] == TEMPLATE && e[DONE] == 0) {
                BlockPos p = BlockPos.of(e[POS]);
                int dx = p.getX() - at.getX();
                int dz = p.getZ() - at.getZ();
                if (dx * dx + dz * dz <= reach * reach) {
                    return true;
                }
            }
        }
        return false;
    }

    /** One template visit: the next small step. Returns -1 to drop the entry. */
    private static int template(ServerLevel level, GrovePaintJob job, long[] e, long now) {
        BlockPos soil = BlockPos.of(e[POS]);
        long packed = e[PACKED];
        GroveHill.Kind kind = (packed & 3) == 0 ? GroveHill.Kind.HERO : GroveHill.Kind.FINALE;
        boolean silver = (packed & 4) != 0;
        int yLimit = (int) ((packed >> 3) & 127);
        int stepDepth = (int) ((packed >> 10) & 15);
        int interval = (int) ((packed >> 14) & ((1L << 26) - 1)); // bits 14-39 only: bit 40 is WAVE_STARTED
        if (!loaded(level, soil, 13)) {
            e[NEXT] = now + 200;
            return 0;
        }
        if ((packed & WAVE_STARTED) == 0) {
            // the ripple (user, 2026-11-23): one clock from sight returning; a tree starts a fixed lag behind the meadow's ring at its distance
            e[PACKED] = packed | WAVE_STARTED;
            long base = job.sightAt >= 0L ? job.sightAt + TCConfig.GROW_START_DELAY.get() : (job.waveStart < 0L ? (job.waveStart = now) : job.waveStart);
            long start = base + waveDelay(job, soil, kind);
            if (start > now) {
                e[NEXT] = start;
                return 0;
            }
        }
        dev.movingcastle.thaumicconcilium.compat.DynamicTreesGrove.Pick pick = dev.movingcastle.thaumicconcilium.compat.DynamicTreesGrove.pick(kind, silver, soil.getX(), soil.getZ());
        var species = dev.movingcastle.thaumicconcilium.compat.DynamicTreesGrove.species(kind, silver);
        boolean first = e[DONE] == 0;
        if (pick == null || !first && !DynamicTreesLandmarks.stands(level, soil, species)) {
            return -1; // cut down or never stood
        }
        if (first && !level.getBlockState(soil.above()).canBeReplaced()) {
            return -1; // something took the spot before the tree's first step
        }
        if (playerInTrunk(level, soil)) {
            e[NEXT] = now + 10;
            return 0;
        }
        // nobody near: nothing to animate, so the tree takes its height in one step (the add-on the user chose with the ripple)
        int limit = watched(level, soil) ? (int) Math.min(e[TARGET], e[DONE] + stepDepth) : (int) e[TARGET];
        boolean last = limit >= e[TARGET];
        long t0 = System.nanoTime();
        if (!DynamicTreesLandmarks.growCut(level, soil, species, silver, pick.code(), pick.facing(), pick.trunkCap(), limit, yLimit, first, last)) {
            return -1;
        }
        templateNanos += System.nanoTime() - t0;
        templateSteps++;
        e[DONE] = limit;
        if (last) {
            DynamicTreesLandmarks.setFertility(level, soil, 15); // natural growth (TemplateGrowthLogic) takes it on to its cap
            return -1;
        }
        e[NEXT] = now + interval;
        return 0;
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
        int budget = TCConfig.GROW_BUDGET_MS.get();
        for (long[] e : job.growth) {
            if (e[KIND] == TEMPLATE) {
                budget = Math.max(budget, TCConfig.FINALE_BUDGET_MS.get());
                break;
            }
        }
        long deadline = System.nanoTime() + budget * 1_000_000L;
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
                int result = e[KIND] == LANDMARK ? landmark(level, job, e, now) : e[KIND] == TEMPLATE ? template(level, job, e, now) : sapling(level, job, e, now);
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
        AABB box = new AABB(soil.getX() - 1.5, soil.getY(), soil.getZ() - 1.5, soil.getX() + 2.5, soil.getY() + 44.0, soil.getZ() + 2.5);
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
