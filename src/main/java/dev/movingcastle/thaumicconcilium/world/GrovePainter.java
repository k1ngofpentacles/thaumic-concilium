package dev.movingcastle.thaumicconcilium.world;

import com.leclowndu93150.thaumaturge.api.taint.ITaintBlock;
import com.leclowndu93150.thaumaturge.content.taint.ecology.TaintBiomeManager;
import com.leclowndu93150.thaumaturge.content.world.space.LevelSpaces;
import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.VesselCrater;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Phase 5d cycle 2b: the grove painter. A {@link GrovePaintJob} (kept in {@link VesselAftermath}) paints Concilium Grove outward from a
 * finished Vessel's hex, one step per beat. The unit is the quart column (4x4 blocks, the biome grid); ring k holds the columns whose
 * centre is 4k to 4k+4 blocks from the hex; a step takes every column of the current ring that lies in one chunk (at most 16). A chunk
 * that is not loaded is put on a waiting list and painted when it loads; nothing is ever force-loaded. Every column action is idempotent
 * (the grove bit says "done"), so a reload resumes anywhere.
 *
 * <p>Per column, in order: taint blocks decay (their own restore may hand the biome back first), the biome becomes grove (a batched
 * rewrite and one resend per chunk), soil, then the 2c hooks. Plot (Sable sub-level) chunks are skipped. The doomed seed's own column
 * is left until the last ring has closed; then the seed is killed and that column is painted last.
 */
public final class GrovePainter {
    private GrovePainter() {
    }

    /** Steps run since the game started (the tests read it to show an idle level does no work). */
    public static long stepsRun;

    // ---------------------------------------------------------------- pure rules
    /** The grove's radius in blocks: tier 3 the crater and its mound; tier 4 {@code min + 4 * round(steps * clamp((burden - low*T) / (span*T)))}. */
    public static int radiusFor(int tier, double maxRim, int burden, int threshold) {
        if (tier < 4) {
            return (int) Math.ceil(maxRim + VesselCrater.MOUND_OUT);
        }
        int lo = TCConfig.PAINT_RADIUS_MIN.get();
        int hi = Math.max(lo, TCConfig.PAINT_RADIUS_MAX.get());
        double span = TCConfig.PAINT_BURDEN_SPAN.get() * threshold;
        double frac = Mth.clamp((burden - TCConfig.PAINT_BURDEN_LOW.get() * threshold) / span, 0.0, 1.0);
        // 5d trees C: the hill reaches at least growth.hill.pastLip past the widest lip (user, 2026-11-23)
        int byBurden = Math.min(hi, lo + 4 * (int) Math.round((hi - lo) * frac / 4.0)); // 4-block steps from lo, reaching hi exactly
        return Math.max(byBurden, GroveHill.minRadiusForRim(maxRim));
    }

    /** The farthest the rim reaches in any direction. */
    public static double maxRim(VesselCrater crater) {
        double max = 0.0;
        for (int i = 0; i < 720; i++) {
            max = Math.max(max, crater.rimAt(i * Math.PI / 360.0));
        }
        return max;
    }

    private static double columnDistance(BlockPos centre, int quartX, int quartZ) {
        double dx = quartX * 4 + 2 - (centre.getX() + 0.5);
        double dz = quartZ * 4 + 2 - (centre.getZ() + 0.5);
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** The ring a quart column belongs to, or -1 when it lies outside the grove. */
    public static int ringOf(GrovePaintJob job, int quartX, int quartZ) {
        double d = columnDistance(job.centre, quartX, quartZ);
        return d < job.radius ? (int) (d / 4.0) : -1;
    }

    /** One chunk's share of a ring. */
    public record Group(long chunk, List<int[]> columns) {
    }

    /** The ring's columns grouped by chunk, chunks in angle order around the hex (deterministic, so a cursor can resume). */
    @SuppressWarnings("unchecked")
    public static List<Group> groups(GrovePaintJob job, int ring) {
        if (job.groupsRing == ring && job.groupsCache instanceof List<?> cached) {
            return (List<Group>) cached;
        }
        List<Group> built = buildGroups(job, ring);
        job.groupsRing = ring;
        job.groupsCache = built;
        return built;
    }

    private static List<Group> buildGroups(GrovePaintJob job, int ring) {
        int reach = job.radius / 4 + 2;
        int qx0 = job.centre.getX() >> 2;
        int qz0 = job.centre.getZ() >> 2;
        Map<Long, List<int[]>> byChunk = new LinkedHashMap<>();
        for (int qx = qx0 - reach; qx <= qx0 + reach; qx++) {
            for (int qz = qz0 - reach; qz <= qz0 + reach; qz++) {
                if (ringOf(job, qx, qz) == ring) {
                    byChunk.computeIfAbsent(ChunkPos.asLong(qx >> 2, qz >> 2), k -> new ArrayList<>()).add(new int[] {qx, qz});
                }
            }
        }
        List<Group> out = new ArrayList<>();
        for (Map.Entry<Long, List<int[]>> e : byChunk.entrySet()) {
            out.add(new Group(e.getKey(), e.getValue()));
        }
        out.sort(Comparator.<Group>comparingDouble(g -> angle(job, g)).thenComparingLong(Group::chunk));
        return out;
    }

    private static double angle(GrovePaintJob job, Group g) {
        double sx = 0.0;
        double sz = 0.0;
        for (int[] c : g.columns()) {
            sx += c[0] * 4 + 2 - (job.centre.getX() + 0.5);
            sz += c[1] * 4 + 2 - (job.centre.getZ() + 0.5);
        }
        return Math.atan2(sz, sx);
    }

    private static int[] chunkOf(long chunk) {
        return new int[] {ChunkPos.getX(chunk), ChunkPos.getZ(chunk)};
    }

    // ---------------------------------------------------------------- opening a job
    /**
     * Opens the painting job for a finished Vessel's hex (tiers 3 and 4). Returns null, and paints nothing, when the hex is in a plot chunk
     * (a Sable sub-level). {@code seed} is the doomed seed that dies when the last ring closes (null for none).
     */
    @Nullable
    public static GrovePaintJob open(ServerLevel level, BlockPos centre, VesselCrater crater, int tier, int burden, @Nullable UUID seed) {
        if (LevelSpaces.isPlot(level, centre)) {
            ThaumicConcilium.LOG.warn("Grove painter: the hex at {} is in a sub-level plot, no grove is painted", centre);
            return null;
        }
        GrovePaintJob job = openWithRadius(level, centre, crater, tier, radiusFor(tier, maxRim(crater), burden, TCConfig.VESSEL_THRESHOLD.get()), seed);
        if (job != null) {
            job.burden = burden;
        }
        return job;
    }

    /** As above with the radius given (the QA command's grove). */
    @Nullable
    public static GrovePaintJob openWithRadius(ServerLevel level, BlockPos centre, VesselCrater crater, int tier, int radius, @Nullable UUID seed) {
        if (LevelSpaces.isPlot(level, centre)) {
            return null;
        }
        GrovePaintJob job = new GrovePaintJob(centre, crater.radius(), crater.seed(), tier, radius, seed);
        // the pond reserve (cycles 4d, 4e): 13 columns read from the crater floor, one level; each entry is the block UNDER its pond block.
        // The floor block there is the livingrock bed; the pond block lies one above it, in the top layer of its mound (PondMound.TOP = 1)
        PondShape shape = PondShape.choose(crater::cut, crater.seed());
        for (int[] c : shape.cells()) {
            job.pond.add(new BlockPos(centre.getX() + c[0], centre.getY() - shape.level() + PondMound.TOP - 1, centre.getZ() + c[1]).asLong());
        }
        VesselAftermath.get(level).addJob(job);
        return job;
    }

    // ---------------------------------------------------------------- the finale (cycle 4e)
    /** Chunk groups one step takes: {@code finale.paintPace} while the swell hides the crater and it is not yet done, else one. */
    public static int pace(GrovePaintJob job, long now) {
        int swell = job.darkUntil > now && job.craterDoneAt < 0L ? Math.max(1, TCConfig.FINALE_PAINT_PACE.get()) : 1;
        // the ripple (user, 2026-11-23): every ring in grove.ringSeconds, so the meadow spreads at one speed however wide the ring
        int groups = job.ring < job.ringCount() ? groups(job, job.ring).size() : 1;
        int ripple = (int) Math.ceil(groups * (double) VesselAftermath.BEAT / (TCConfig.RING_SECONDS.get() * 20.0));
        return Math.max(swell, Math.max(1, ripple));
    }

    /**
     * The crater (everything within its farthest rim) is painted: rings 0 to {@code (int) (maxRim / 4)} have closed and no waiting chunk
     * (not loaded when its ring passed) touches the disc of radius maxRim round the hex.
     */
    public static boolean craterDone(GrovePaintJob job, VesselCrater crater) {
        if (Double.isNaN(job.craterRim)) {
            job.craterRim = maxRim(crater);
        }
        double rim = job.craterRim;
        if (job.ring <= (int) (rim / 4.0) && job.ring < job.ringCount()) {
            return false;
        }
        for (long chunk : job.waiting) {
            int[] cp = chunkOf(chunk);
            double dx = Math.max(Math.max(cp[0] * 16 - (job.centre.getX() + 0.5), (job.centre.getX() + 0.5) - (cp[0] * 16 + 16)), 0.0);
            double dz = Math.max(Math.max(cp[1] * 16 - (job.centre.getZ() + 0.5), (job.centre.getZ() + 0.5) - (cp[1] * 16 + 16)), 0.0);
            if (dx * dx + dz * dz <= rim * rim) {
                return false;
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- the driver's entry points
    /** One step for every open job of the level. */
    public static void stepAll(ServerLevel level, List<GrovePaintJob> jobs) {
        boolean changed = false;
        for (GrovePaintJob job : jobs) {
            if (!job.done) {
                step(level, job);
                changed = true;
            }
        }
        if (changed) {
            VesselAftermath.get(level).setDirty();
        }
    }

    /** One step of one job: the next chunk group of the current ring, up to a few waiting chunks, and the seed's end once the rings close. */
    public static void step(ServerLevel level, GrovePaintJob job) {
        if (job.done) {
            return;
        }
        stepsRun++;
        long t0 = System.nanoTime();
        long deadline = t0 + TCConfig.PAINT_BUDGET_MS.get() * 1_000_000L;
        VesselCrater crater = new VesselCrater(job.centre, job.craterRadius, job.craterSeed);
        int rings = job.ringCount();
        int pace = pace(job, level.getGameTime());
        long tPaint = System.nanoTime();
        for (int n = 0; n < pace && job.ring < rings; n++) {
            List<Group> groups = groups(job, job.ring);
            while (job.cursor >= groups.size()) {
                job.ring++;
                job.cursor = 0;
                if (job.ring >= rings) {
                    break;
                }
                groups = groups(job, job.ring);
            }
            if (job.ring < rings) {
                Group g = groups.get(job.cursor);
                job.cursor++;
                paintOrWait(level, job, crater, g.chunk(), g.columns(), deadline, true);
            }
        }
        long tWait = System.nanoTime();
        resolveWaiting(level, job, crater, deadline);
        long tAfterWait = System.nanoTime();
        if (job.darkUntil >= 0L && job.craterDoneAt < 0L && craterDone(job, crater)) {
            job.craterDoneAt = level.getGameTime();
        }
        if (job.craterDoneAt >= 0L && !job.pondPlaced) {
            GrovePond.placeEarly(level, job); // the finale: the pond stands under the swell, before the light rises from it
        }
        long tDecor = System.nanoTime();
        processDecor(level, job, deadline);
        long tTail = System.nanoTime();
        if (job.ring >= rings) {
            if (!job.seedDone) {
                finishSeed(level, job, crater, deadline);
            }
            // the pond is the last thing: the job is done only once it stands (at once without Botania, which has no pond)
            if (job.seedDone && job.waiting.isEmpty() && job.decor.isEmpty() && GrovePond.finish(level, job)) {
                job.done = true;
            }
        }
        long took = System.nanoTime() - t0;
        job.steps++;
        job.totalNanos += took;
        job.maxStepNanos = Math.max(job.maxStepNanos, took);
        if (took > slowestStep) { // tests and tuning: where the slowest step's time went
            slowestStep = took;
            slowestParts = new long[] {tWait - tPaint, tAfterWait - tWait, tTail - tDecor, System.nanoTime() - tTail, tDecor - tAfterWait};
            slowestRing = job.ring;
        }
    }

    /** Tests: the slowest painter step since the counters were last cleared, in nanoseconds: total, and {paint, waiting, decor, tail, pond}. */
    public static long slowestStep;
    public static long[] slowestParts = new long[5];
    public static int slowestRing;
    /** Tests: the slowest decor item of each kind (the hooks' item[0]), in nanoseconds. */
    public static final long[] DECOR_MAX = new long[8];
    public static final long[] DECOR_SUM = new long[8];
    public static final long[] DECOR_COUNT = new long[8];

    private static void paintOrWait(ServerLevel level, GrovePaintJob job, VesselCrater crater, long chunkLong, List<int[]> columns, long deadline,
            boolean mayWait) {
        int[] cp = chunkOf(chunkLong);
        if (LevelSpaces.isPlotChunk(level, new ChunkPos(cp[0], cp[1]))) {
            return; // a sub-level's plot: never painted
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(cp[0], cp[1]);
        if (chunk == null) {
            if (mayWait && !job.waiting.contains(chunkLong)) {
                job.waiting.add(chunkLong);
            }
            return;
        }
        job.painted += paintColumns(level, job, crater, chunk, columns, deadline);
    }

    /** Up to a few chunks that were not loaded when their ring passed: painted now if they have loaded, up to the current ring. */
    private static void resolveWaiting(ServerLevel level, GrovePaintJob job, VesselCrater crater, long deadline) {
        // 5d trees (Fable consult, 2026-11-23): look at up to waitingChecks entries, rotating unloaded ones to the back so they never block
        // a loaded chunk behind them, and paint at most one waiting chunk a step, none once the step's budget is spent (a whole chunk is
        // 4 to 16 times a normal group)
        int checks = Math.min(TCConfig.PAINT_WAITING_CHECKS.get(), job.waiting.size());
        boolean painted = false;
        for (int k = 0; k < checks && !job.waiting.isEmpty(); k++) {
            if (painted || System.nanoTime() > deadline) {
                return;
            }
            long chunkLong = job.waiting.remove(0);
            int[] cp = chunkOf(chunkLong);
            LevelChunk chunk = level.getChunkSource().getChunkNow(cp[0], cp[1]);
            if (chunk == null) {
                job.waiting.add(chunkLong); // still unloaded: to the back
                continue;
            }
            painted = true;
            List<int[]> due = new ArrayList<>();
            for (int lx = 0; lx < 4; lx++) {
                for (int lz = 0; lz < 4; lz++) {
                    int qx = cp[0] * 4 + lx;
                    int qz = cp[1] * 4 + lz;
                    int r = ringOf(job, qx, qz);
                    if (r >= 0 && r <= job.ring) {
                        due.add(new int[] {qx, qz});
                    }
                }
            }
            job.painted += paintColumns(level, job, crater, chunk, due, deadline);
        }
    }

    /**
     * The queued decoration (cycle 2c): at least one item per step, then more until the step budget {@code deadline} has passed. An item the
     * hooks cannot run yet (their chunks are not loaded) goes to the back of the queue; each item is tried once per step at most.
     */
    private static void processDecor(ServerLevel level, GrovePaintJob job, long deadline) {
        int n = job.decor.size();
        // 5d trees: one item is guaranteed only every second step when the painting already spent the budget, so the queue still moves
        boolean guaranteed = System.nanoTime() <= deadline || (job.steps & 1) == 0;
        for (int i = 0; i < n && !job.decor.isEmpty(); i++) {
            if ((i > 0 || !guaranteed) && System.nanoTime() > deadline) {
                break;
            }
            long[] item = job.decor.remove(0);
            long t = System.nanoTime();
            if (!hooks().onDecor(level, job, item)) {
                job.decor.add(item);
            }
            int kind = (int) Math.max(0, Math.min(DECOR_MAX.length - 1, item[0]));
            DECOR_MAX[kind] = Math.max(DECOR_MAX[kind], System.nanoTime() - t);
            DECOR_SUM[kind] += System.nanoTime() - t;
            DECOR_COUNT[kind]++;
        }
    }

    /**
     * The last ring is closed: the doomed seed dies (cycle 4b: over the twelve seconds of {@link GroveSeedDeath}, during which this
     * waits), then its own column is decayed and painted, last of all.
     */
    private static void finishSeed(ServerLevel level, GrovePaintJob job, VesselCrater crater, long deadline) {
        if (job.seed != null) {
            Entity e = level.getEntity(job.seed);
            if (e == null && !level.areEntitiesLoaded(ChunkPos.asLong(job.centre))) {
                return; // its chunk is not loaded: it may still be there, wait
            }
            if (!GroveSeedDeath.step(level, job, e)) {
                return; // the grove is still killing it
            }
        }
        job.seedDone = true;
        List<int[]> col = List.<int[]>of(new int[] {job.centre.getX() >> 2, job.centre.getZ() >> 2});
        paintOrWait(level, job, crater, ChunkPos.asLong(col.get(0)[0] >> 2, col.get(0)[1] >> 2), col, deadline, true);
    }

    // ---------------------------------------------------------------- one chunk's columns
    private static boolean isSeedColumn(GrovePaintJob job, int quartX, int quartZ) {
        return !job.seedDone && quartX == (job.centre.getX() >> 2) && quartZ == (job.centre.getZ() >> 2);
    }

    /** Paints the given quart columns (all inside {@code chunk}); returns how many were newly painted. */
    public static int paintColumns(ServerLevel level, GrovePaintJob job, VesselCrater crater, LevelChunk chunk, List<int[]> columns, long deadline) {
        GroveColumns grove = chunk.getData(GroveColumns.ATTACHMENT.get());
        int before = grove.mask();
        List<int[]> todo = new ArrayList<>();
        for (int[] c : columns) {
            if (!grove.has(c[0], c[1]) && !isSeedColumn(job, c[0], c[1])) {
                todo.add(c);
            }
        }
        if (todo.isEmpty()) {
            return 0;
        }
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        // (a) taint decays first: soil back to dirt, rock to porous stone, fibres restore their own biome
        for (int[] c : todo) {
            for (int bx = 0; bx < 4; bx++) {
                for (int bz = 0; bz < 4; bz++) {
                    int x = c[0] * 4 + bx;
                    int z = c[1] * 4 + bz;
                    int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    for (int y = Math.max(minY, h - 8); y <= Math.min(maxY - 1, h + 8); y++) {
                        BlockState state = chunk.getBlockState(probe.set(x, y, z));
                        if (state.getBlock() instanceof ITaintBlock taint) {
                            taint.decay(level, probe.immutable(), state);
                        }
                    }
                }
            }
        }
        // (b) the biome: tainted or taint-changed columns through Thaumaturge's own call (it clears its flag), the rest in one batch
        Holder<Biome> groveBiome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(GroveColumns.KEY);
        LongSet plain = new LongOpenHashSet();
        for (int[] c : todo) {
            int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c[0] * 4, c[1] * 4);
            BlockPos pos = new BlockPos(c[0] * 4, h, c[1] * 4);
            if (TaintBiomeManager.isChangedColumn(level, pos) || TaintBiomeManager.isTainted(level, pos)) {
                TaintBiomeManager.replaceColumn(level, pos, GroveColumns.KEY);
            } else {
                plain.add(ChunkPos.asLong(c[0], c[1]));
            }
        }
        if (!plain.isEmpty()) {
            rewrite(level, chunk, plain, groveBiome);
        }
        for (int[] c : todo) {
            grove.set(c[0], c[1]);
        }
        chunk.setUnsaved(true);
        // (c) soil, and (d) the hooks for 2c
        Set<Long> pond = pondColumns(job);
        for (int[] c : todo) {
            for (int bx = 0; bx < 4; bx++) {
                for (int bz = 0; bz < 4; bz++) {
                    int x = c[0] * 4 + bx;
                    int z = c[1] * 4 + bz;
                    if (pond.contains(ChunkPos.asLong(x, z))) {
                        continue; // the pond reserve stays bare
                    }
                    boolean hollow = isHollow(job, crater, x, z);
                    soil(level, probe, x, z, hollow);
                    hooks().onColumn(level, job, x, z, hollow, deadline);
                }
            }
        }
        if (grove.full() && before != GroveColumns.ALL) {
            hooks().onChunkPainted(level, job, chunk, deadline);
        }
        SeedRoots.reach(level, job, chunk, columns); // cycle 4f: a Taint Seed standing in a column the rings have reached rolls for roots
        return todo.size();
    }

    /** Whether a block column is part of the green hollow (tiers 3 and 4): the bowl, anywhere the crater cuts, and its mound. */
    public static boolean isHollow(GrovePaintJob job, VesselCrater crater, int x, int z) {
        int dx = x - job.centre.getX();
        int dz = z - job.centre.getZ();
        return crater.inBowl(dx, dz) || crater.cut(dx, dz) > 0 || crater.mound(dx, dz) > 0;
    }

    /** The pond reserve's block columns, packed as {@code ChunkPos.asLong(x, z)} (not chunks: the same packing for two ints). */
    public static Set<Long> pondColumns(GrovePaintJob job) {
        Set<Long> set = new HashSet<>();
        for (long l : job.pond) {
            BlockPos p = BlockPos.of(l);
            set.add(ChunkPos.asLong(p.getX(), p.getZ()));
        }
        return set;
    }

    private static void soil(ServerLevel level, BlockPos.MutableBlockPos probe, int x, int z, boolean hollow) {
        int minY = level.getMinBuildHeight();
        int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (!hollow) {
            int y = h - 1;
            if (y < minY) {
                return;
            }
            BlockState state = level.getBlockState(probe.set(x, y, z));
            if (state.is(BlockTags.DIRT) && !state.is(Blocks.GRASS_BLOCK)) {
                level.setBlock(probe, Blocks.GRASS_BLOCK.defaultBlockState(), 18);
            }
            return;
        }
        // the hollow: the top full-cube block becomes grass and the one under it dirt, whatever they were (but never a block with an inventory
        // or one as blast-proof as obsidian)
        for (int y = h - 1; y >= Math.max(minY, h - 9); y--) {
            BlockState state = level.getBlockState(probe.set(x, y, z));
            if (state.isAir() || !state.getFluidState().isEmpty() || state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)
                    || !state.isCollisionShapeFullBlock(level, probe)) {
                continue;
            }
            if (state.hasBlockEntity() || VesselCrater.spared(state)) {
                return;
            }
            if (!state.is(Blocks.GRASS_BLOCK)) {
                level.setBlock(probe, Blocks.GRASS_BLOCK.defaultBlockState(), 18);
            }
            if (y - 1 >= minY) {
                BlockState below = level.getBlockState(probe.set(x, y - 1, z));
                if (!below.isAir() && !below.hasBlockEntity() && !VesselCrater.spared(below) && !below.is(Blocks.DIRT) && below.getFluidState().isEmpty()) {
                    level.setBlock(probe, Blocks.DIRT.defaultBlockState(), 18);
                }
            }
            return;
        }
    }

    /** Rewrites the chunk's biome grid with the grove in the given quart columns, leaves the rest as it was, and resends the chunk once. */
    private static void rewrite(ServerLevel level, LevelChunk chunk, LongSet groveColumns, Holder<Biome> groveBiome) {
        int minQuartY = QuartPos.fromBlock(level.getMinBuildHeight());
        int quartHeight = QuartPos.fromBlock(level.getMaxBuildHeight() - 1) - minQuartY + 1;
        int baseX = QuartPos.fromSection(chunk.getPos().x);
        int baseZ = QuartPos.fromSection(chunk.getPos().z);
        @SuppressWarnings("unchecked")
        Holder<Biome>[] snap = (Holder<Biome>[]) new Holder[quartHeight * 16];
        for (int y = 0; y < quartHeight; y++) {
            for (int z = 0; z < 4; z++) {
                for (int x = 0; x < 4; x++) {
                    snap[(y * 4 + z) * 4 + x] = chunk.getNoiseBiome(baseX + x, minQuartY + y, baseZ + z);
                }
            }
        }
        var sampler = level.getChunkSource().randomState().sampler();
        chunk.fillBiomesFromNoise((qx, qy, qz, s) -> {
            if (groveColumns.contains(ChunkPos.asLong(qx, qz))) {
                return groveBiome;
            }
            int y = Mth.clamp(qy - minQuartY, 0, quartHeight - 1);
            return snap[(y * 4 + (qz - baseZ)) * 4 + (qx - baseX)];
        }, sampler);
        chunk.setUnsaved(true);
        level.getChunkSource().chunkMap.resendBiomesForChunks(List.of(chunk));
    }

    // ---------------------------------------------------------------- cycle 2c's hooks
    /** What {@link GroveLife} fills in: per painted block column, per fully painted chunk and per queued decoration item. */
    public interface Hooks {
        /** A block column was painted: {@code hollow} says whether it is in the green hollow. Stop at {@code deadline} (System.nanoTime). */
        void onColumn(ServerLevel level, GrovePaintJob job, int x, int z, boolean hollow, long deadline);

        /** All 16 quart columns of a chunk are painted (once per chunk): where the chunk's features and a node go. */
        void onChunkPainted(ServerLevel level, GrovePaintJob job, LevelChunk chunk, long deadline);

        /** Runs one queued {@link GrovePaintJob#decor} item; false = cannot run yet (chunks not loaded), keep it for a later step. */
        default boolean onDecor(ServerLevel level, GrovePaintJob job, long[] item) {
            return true;
        }
    }

    private static final Hooks NONE = new Hooks() {
        @Override
        public void onColumn(ServerLevel level, GrovePaintJob job, int x, int z, boolean hollow, long deadline) {
        }

        @Override
        public void onChunkPainted(ServerLevel level, GrovePaintJob job, LevelChunk chunk, long deadline) {
        }
    };

    private static Hooks hooks = NONE;

    public static Hooks hooks() {
        return hooks;
    }

    /** The mod installs {@link GroveLife} here at startup; null means no decoration at all; the tests install a counter to see the hooks fire. */
    public static void setHooks(@Nullable Hooks h) {
        hooks = h == null ? NONE : h;
    }
}
