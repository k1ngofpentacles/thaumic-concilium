package dev.movingcastle.thaumicconcilium.world;

import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode;
import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.compat.DynamicTreesLandmarks;
import dev.movingcastle.thaumicconcilium.compat.DynamicTreesSaplings;
import dev.movingcastle.thaumicconcilium.entity.VesselCrater;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.fml.ModList;

/**
 * Phase 5d cycle 2c: what grows in a painted Concilium Grove, through {@link GrovePainter.Hooks}. Every pick comes from a hash of the job's
 * crater seed and the column, so a reload or a repaint chooses the same things.
 *
 * <ul>
 * <li>Per block column ({@link #onColumn}): Botania mystical flowers (two colours per 16-block cell), double flowers and shade mushrooms
 * (only when Botania is loaded; blocks by id), Thaumaturge's own meadow flowers, short grass and fern, and (tier 4) the density saplings.
 * Every block goes through {@code canSurvive}; flags 18.</li>
 * <li>Per fully painted chunk ({@link #onChunkPainted}): queues Thaumaturge's {@code magic_forest_flora} and {@code mana_pods} features
 * and, on a 15% roll, one wild node.</li>
 * <li>Landmark trees and the tier 3 saplings: spots planned from the job, queued when the painter reaches the column.</li>
 * </ul>
 * Nothing here ever lands on the pond reserve (the painter skips its columns; a cleanup pass removes what a chunk feature drifted onto it).
 * Features and trees are queued in {@link GrovePaintJob#decor} and run under the step budget, and only when every chunk they could touch is
 * loaded (a chunk is never force-loaded).
 */
public final class GroveLife implements GrovePainter.Hooks {
    public static final GroveLife INSTANCE = new GroveLife();

    // decor item kinds ({kind, a, b, c})
    public static final int FEATURES = 0;
    public static final int NODE = 1;
    public static final int TREE = 2;
    public static final int SAPLING = 3;

    /** Test switch: null = follow the mod list, otherwise forces the Botania path on or off. */
    @Nullable
    public static Boolean botaniaOverride;
    /** Test switch: false = never use Dynamic Trees even when it is loaded (the plain Thaumaturge sapling path). */
    public static boolean dynamicTreesAllowed = true;

    /** Times a landmark tree could not be grown and a sapling was planted instead (the tests and the notes read it). */
    public static int landmarkFallbacks;
    /** Landmark trees grown from our own shapes (LandmarkShapes) rather than Thaumaturge's feature. */
    public static int landmarkShapes;

    public static final String[] COLOURS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple",
            "blue", "brown", "green", "red", "black"};

    // cumulative shares of the one roll a block column makes (the rest of the column stays bare)
    public static final double DOUBLE_FLOWER = 0.01;
    public static final double FLOWER = DOUBLE_FLOWER + 0.05;
    public static final double MEADOW_FLOWER = FLOWER + 0.15;
    public static final double GRASS = MEADOW_FLOWER + 0.20;
    public static final double FERN = GRASS + 0.04;
    public static final double MUSHROOM_IN_SHADE = 0.06;
    public static final double GREATWOOD_SAPLING = 1.0 / 24.0;
    public static final double SILVERWOOD_SAPLING = 1.0 / 96.0;
    public static final int SAPLING_SPACING = 6;
    /** The chunk radius a tree's blocks can reach (greatwood radius 8, plus a spot's search offsets). */
    private static final int TREE_REACH = 13;
    private static final int PLACE_FLAGS = 18;
    private static final int[][] OFFSETS = {{0, 0}, {2, 0}, {-2, 0}, {0, 2}, {0, -2}, {2, 2}, {-2, -2}, {3, -3}};

    private static final TagKey<Block> DT_SAPLINGS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("dynamictrees", "saplings"));
    private static final TagKey<Block> MEADOW_FLOWERS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("thaumaturge", "magical_forest_flowers"));

    private GroveLife() {
    }

    // ---------------------------------------------------------------- pure rules
    public static boolean botania() {
        return botaniaOverride != null ? botaniaOverride : ModList.get().isLoaded("botania");
    }

    /** A hash of (seed, x, z, salt) in [0, 1). */
    public static double roll(long seed, int x, int z, int salt) {
        long v = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        v ^= v >>> 33;
        v *= 0xff51afd7ed558ccdL;
        v ^= v >>> 33;
        v *= 0xc4ceb9fe1a85ec53L;
        v ^= v >>> 33;
        return (v >>> 11) * 0x1.0p-53;
    }

    /** What a column's one roll {@code u} (0..1) makes of it: 0 bare, 1 double flower, 2 mystical flower, 3 meadow flower, 4 grass, 5 fern. */
    public static int pick(double u, boolean botania) {
        if (u < DOUBLE_FLOWER) {
            return botania ? 1 : 0;
        }
        if (u < FLOWER) {
            return botania ? 2 : 0;
        }
        if (u < MEADOW_FLOWER) {
            return 3;
        }
        if (u < GRASS) {
            return 4;
        }
        return u < FERN ? 5 : 0;
    }

    /** The shade rule: the vanilla mushroom light test (time independent), or within 3 blocks of a landmark trunk. */
    public static boolean shaded(int rawBrightness, double distanceToTrunk) {
        return rawBrightness < 13 || distanceToTrunk <= 3.0;
    }

    /** Whether a fully painted chunk gets a node: {@code chance} per chunk, from the job's seed and the chunk. At most one per chunk by construction. */
    public static boolean nodeRolls(long seed, long chunk, double chance) {
        return roll(seed, ChunkPos.getX(chunk), ChunkPos.getZ(chunk), 41) < chance;
    }

    /** The two colours of a 16-block cell (indices into {@link #COLOURS}, different). */
    public static int[] palette(long seed, int cellX, int cellZ) {
        int a = (int) (roll(seed, cellX, cellZ, 11) * 16) & 15;
        int b = (a + 1 + (int) (roll(seed, cellX, cellZ, 12) * 15)) & 15;
        return new int[] {a, b};
    }

    // ---------------------------------------------------------------- the plan: landmark and fixed sapling spots
    /** Spots for one job: {x, z, kind (TREE or SAPLING), species (0 greatwood, 1 silverwood)}. */
    static final class Plan {
        final List<int[]> spots = new ArrayList<>();
        final Map<Long, int[]> byColumn = new HashMap<>();
        final List<int[]> trunks = new ArrayList<>();
        List<Block> meadow;
    }

    /** The landmark and (tier 3) sapling spots of a job; pure, so the tests can read them. */
    public static List<int[]> spots(GrovePaintJob job) {
        return plan(job).spots;
    }

    static Plan plan(GrovePaintJob job) {
        if (job.plan instanceof Plan p) {
            return p;
        }
        Plan plan = new Plan();
        long seed = job.craterSeed;
        double cx = job.centre.getX() + 0.5;
        double cz = job.centre.getZ() + 0.5;
        if (job.tier < 4) {
            VesselCrater crater = new VesselCrater(job.centre, job.craterRadius, job.craterSeed);
            double angle = roll(seed, 1, 1, 31) * Mth.TWO_PI;
            double d = crater.rimAt(angle);
            add(plan, (int) Math.floor(cx + Math.cos(angle) * d), (int) Math.floor(cz + Math.sin(angle) * d), TREE, 0);
            // tier 3: exactly two greatwood and one silverwood sapling, spread around the hollow
            for (int k = 0; k < 3; k++) {
                double a = angle + Math.PI * (1 + 2 * k) / 3.0;
                double dist = job.radius * 0.6;
                add(plan, (int) Math.floor(cx + Math.cos(a) * dist), (int) Math.floor(cz + Math.sin(a) * dist), SAPLING, k == 2 ? 1 : 0);
            }
        } else {
            int n = Math.max(1, Math.round(job.radius / 8.0f));
            int silver = Math.min(n - 1, (int) (roll(seed, 2, 2, 32) * n));
            double phase = roll(seed, 3, 3, 33) * Mth.TWO_PI;
            for (int i = 0; i < n; i++) {
                double jitter = (roll(seed, i, 4, 34) - 0.5) * 0.5;
                double a = phase + Mth.TWO_PI * (i + jitter) / n;
                double dist = job.radius * (0.55 + 0.25 * roll(seed, i, 5, 35));
                add(plan, (int) Math.floor(cx + Math.cos(a) * dist), (int) Math.floor(cz + Math.sin(a) * dist), TREE, i == silver ? 1 : 0);
            }
        }
        job.plan = plan;
        return plan;
    }

    private static void add(Plan plan, int x, int z, int kind, int species) {
        int[] spot = {x, z, kind, species};
        plan.spots.add(spot);
        if (kind == TREE) {
            plan.byColumn.put(ChunkPos.asLong(x, z), spot);
            plan.trunks.add(spot);
        }
        // fixed saplings (tier 3) are queued by their landmark tree once it stands, so its canopy and roots can never overwrite one
    }

    // ---------------------------------------------------------------- hooks
    @Override
    public void onColumn(ServerLevel level, GrovePaintJob job, int x, int z, boolean hollow, long deadline) {
        Plan plan = plan(job);
        int[] spot = plan.byColumn.get(ChunkPos.asLong(x, z));
        if (spot != null) {
            job.decor.add(new long[] {spot[2], x, z, spot[3]});
        }
        int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos(x, h - 1, z);
        if (!level.getBlockState(probe).is(Blocks.GRASS_BLOCK)) {
            return;
        }
        BlockPos at = new BlockPos(x, h, z);
        if (!level.getBlockState(at).isAir()) {
            return;
        }
        long seed = job.craterSeed;
        if (job.tier >= 4 && !GrovePond.moundColumns(job).contains(ChunkPos.asLong(x, z)) && saplingAt(level, job, at, seed)) {
            return;
        }
        boolean botania = botania();
        if (botania && roll(seed, x, z, 23) < MUSHROOM_IN_SHADE && shaded(level.getRawBrightness(at, 0), distanceToTrunk(plan, x, z))) {
            int colour = (int) (roll(seed, x, z, 24) * 16) & 15;
            put(level, at, colour, "mushroom");
            return;
        }
        switch (pick(roll(seed, x, z, 25), botania)) {
            case 1 -> {
                int[] pal = palette(seed, x >> 4, z >> 4);
                put(level, at, pal[roll(seed, x, z, 26) < 0.5 ? 0 : 1], "double_flower");
            }
            case 2 -> {
                int[] pal = palette(seed, x >> 4, z >> 4);
                put(level, at, pal[roll(seed, x, z, 26) < 0.5 ? 0 : 1], "mystical_flower");
            }
            case 3 -> {
                List<Block> flowers = meadow(plan);
                if (!flowers.isEmpty()) {
                    set(level, at, flowers.get((int) (roll(seed, x, z, 27) * flowers.size()) % flowers.size()).defaultBlockState());
                }
            }
            case 4 -> set(level, at, Blocks.SHORT_GRASS.defaultBlockState());
            case 5 -> set(level, at, Blocks.FERN.defaultBlockState());
            default -> {
            }
        }
    }

    @Override
    public void onChunkPainted(ServerLevel level, GrovePaintJob job, LevelChunk chunk, long deadline) {
        long c = chunk.getPos().toLong();
        job.decor.add(new long[] {FEATURES, c, 0, 0});
        if (nodeRolls(job.craterSeed, c, TCConfig.GROVE_NODE_CHANCE.get())) {
            job.decor.add(new long[] {NODE, c, 0, 0});
        }
    }

    @Override
    public boolean onDecor(ServerLevel level, GrovePaintJob job, long[] item) {
        if (item[0] == FEATURES) {
            return chunkFeatures(level, job, item[1]);
        }
        if (item[0] == NODE) {
            return node(level, job, item[1]);
        }
        if (item[0] == TREE) {
            return tree(level, job, (int) item[1], (int) item[2], item[3] == 1);
        }
        if (item[0] == SAPLING) {
            return sapling(level, job, (int) item[1], (int) item[2], item[3] == 1);
        }
        return true;
    }

    // ---------------------------------------------------------------- per column helpers
    private static double distanceToTrunk(Plan plan, int x, int z) {
        double best = Double.MAX_VALUE;
        for (int[] t : plan.trunks) {
            best = Math.min(best, Math.sqrt((double) (t[0] - x) * (t[0] - x) + (double) (t[1] - z) * (t[1] - z)));
        }
        return best;
    }

    private static List<Block> meadow(Plan plan) {
        if (plan.meadow == null) {
            List<Block> list = new ArrayList<>();
            BuiltInRegistries.BLOCK.getTag(MEADOW_FLOWERS).ifPresent(set -> set.forEach(h -> list.add(h.value())));
            plan.meadow = list;
        }
        return plan.meadow;
    }

    private static boolean set(ServerLevel level, BlockPos at, BlockState state) {
        if (!level.getBlockState(at).isAir() || !state.canSurvive(level, at)) {
            return false;
        }
        level.setBlock(at, state, PLACE_FLAGS);
        return true;
    }

    /** A Botania block by colour and suffix; skipped when Botania is not there (the id is not registered). */
    private static void put(ServerLevel level, BlockPos at, int colour, String suffix) {
        Block block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("botania", COLOURS[colour] + "_" + suffix)).orElse(null);
        if (block == null) {
            return;
        }
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            if (!level.getBlockState(at).isAir() || !level.getBlockState(at.above()).isAir() || at.getY() + 1 >= level.getMaxBuildHeight() || !state.canSurvive(level, at)) {
                return;
            }
            DoublePlantBlock.placeAt(level, state, at, PLACE_FLAGS);
            return;
        }
        set(level, at, state);
    }

    private static boolean saplingAt(ServerLevel level, GrovePaintJob job, BlockPos at, long seed) {
        int x = at.getX();
        int z = at.getZ();
        boolean silver;
        if (roll(seed, x, z, 21) < GREATWOOD_SAPLING) {
            silver = false;
        } else if (roll(seed, x, z, 22) < SILVERWOOD_SAPLING) {
            silver = true;
        } else {
            return false;
        }
        if (nearSapling(level, at)) {
            return false;
        }
        return plantSapling(level, job, at, silver);
    }

    /** A block in {@code #minecraft:saplings} or {@code #dynamictrees:saplings}. */
    public static boolean isSapling(BlockState state) {
        return state.is(BlockTags.SAPLINGS) || state.is(DT_SAPLINGS);
    }

    /** Whether any sapling stands within {@link #SAPLING_SPACING} blocks (only loaded chunks are looked at). */
    private static boolean nearSapling(ServerLevel level, BlockPos at) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        if (dynamicTreesAllowed && DynamicTreesSaplings.available()) {
            // cycle 4e: nor within 6 blocks of a branch (a grown or growing tree's wood), so a sapling never roots in a tree's footprint
            for (int dx = -6; dx <= 6; dx++) {
                for (int dz = -6; dz <= 6; dz++) {
                    if (dx * dx + dz * dz > 36) {
                        continue;
                    }
                    LevelChunk chunk = level.getChunkSource().getChunkNow((at.getX() + dx) >> 4, (at.getZ() + dz) >> 4);
                    if (chunk == null) {
                        continue;
                    }
                    for (int dy = -1; dy <= 14; dy++) {
                        if (DynamicTreesSaplings.isBranch(chunk.getBlockState(p.set(at.getX() + dx, at.getY() + dy, at.getZ() + dz)))) {
                            return true;
                        }
                    }
                }
            }
        }
        for (int dx = -SAPLING_SPACING; dx <= SAPLING_SPACING; dx++) {
            for (int dz = -SAPLING_SPACING; dz <= SAPLING_SPACING; dz++) {
                if (dx * dx + dz * dz > SAPLING_SPACING * SAPLING_SPACING) {
                    continue;
                }
                LevelChunk chunk = level.getChunkSource().getChunkNow((at.getX() + dx) >> 4, (at.getZ() + dz) >> 4);
                if (chunk == null) {
                    continue;
                }
                for (int dy = -3; dy <= 3; dy++) {
                    if (isSapling(chunk.getBlockState(p.set(at.getX() + dx, at.getY() + dy, at.getZ() + dz)))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** As {@link #plantSapling(ServerLevel, BlockPos, boolean)}, and a Dynamic Trees sapling is listed for the job's accelerated growth (cycle 4e). */
    public static boolean plantSapling(ServerLevel level, GrovePaintJob job, BlockPos at, boolean silver) {
        if (!plantSapling(level, at, silver)) {
            return false;
        }
        if (dynamicTreesAllowed && DynamicTreesSaplings.available() && DynamicTreesSaplings.isSapling(level, at)) {
            GroveGrowth.addSapling(level, job, at, silver);
        }
        return true;
    }

    /** Plants a greatwood or silverwood sapling at an air block on grass: Dynamic Trees' own when it is loaded, else Thaumaturge's block. */
    public static boolean plantSapling(ServerLevel level, BlockPos at, boolean silver) {
        if (!level.getBlockState(at).isAir()) {
            return false;
        }
        if (dynamicTreesAllowed && DynamicTreesSaplings.available()) {
            return DynamicTreesSaplings.plant(level, at, silver);
        }
        Block block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("thaumaturge", silver ? "sapling_silverwood" : "sapling_greatwood")).orElse(null);
        return block != null && set(level, at, block.defaultBlockState());
    }

    // ---------------------------------------------------------------- queued decoration
    private static boolean loaded(ServerLevel level, int blockX, int blockZ, int reach) {
        for (int cx = (blockX - reach) >> 4; cx <= (blockX + reach) >> 4; cx++) {
            for (int cz = (blockZ - reach) >> 4; cz <= (blockZ + reach) >> 4; cz++) {
                if (level.getChunkSource().getChunkNow(cx, cz) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean chunkFeatures(ServerLevel level, GrovePaintJob job, long chunkLong) {
        int cx = ChunkPos.getX(chunkLong);
        int cz = ChunkPos.getZ(chunkLong);
        if (!loaded(level, cx * 16 + 8, cz * 16 + 8, 16 + 3)) {
            return false; // the pods wander up to 3 blocks over the edge: wait until the neighbours are loaded
        }
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        var placed = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        RandomSource random = RandomSource.create(job.craterSeed ^ (chunkLong * 0x9E3779B97F4A7C15L));
        BlockPos origin = new BlockPos(cx * 16, level.getHeight(Heightmap.Types.MOTION_BLOCKING, cx * 16, cz * 16), cz * 16);
        for (String id : new String[] {"magic_forest_flora", "mana_pods"}) {
            PlacedFeature feature = placed.get(ResourceLocation.fromNamespaceAndPath("thaumaturge", id));
            if (feature == null) {
                ThaumicConcilium.LOG.warn("Grove life: placed feature thaumaturge:{} is missing", id);
                continue;
            }
            feature.placeWithBiomeCheck(level, generator, random, origin);
        }
        clearPond(level, job, cx, cz);
        return true;
    }

    /** The pond reserve stays bare: whatever a chunk feature drifted onto its columns (plants, pods) goes. */
    private static void clearPond(ServerLevel level, GrovePaintJob job, int cx, int cz) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (long l : job.pond) {
            BlockPos base = BlockPos.of(l);
            if ((base.getX() >> 4) != cx || (base.getZ() >> 4) != cz) {
                continue;
            }
            for (int y = base.getY(); y <= base.getY() + 8; y++) {
                BlockState state = level.getBlockState(p.set(base.getX(), y, base.getZ()));
                if (!state.isAir() && state.getFluidState().isEmpty() && (state.canBeReplaced() || state.getBlock() instanceof BushBlock)) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), PLACE_FLAGS);
                }
            }
        }
    }

    /** One wild node (Thaumaturge's own {@code nodes_wild} feature, without its biome and rarity filters: the 15% is ours) in a chunk without one. */
    private static boolean node(ServerLevel level, GrovePaintJob job, long chunkLong) {
        int cx = ChunkPos.getX(chunkLong);
        int cz = ChunkPos.getZ(chunkLong);
        LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
        if (chunk == null) {
            return false;
        }
        if (chunk.getBlockEntities().values().stream().anyMatch(be -> be instanceof BlockEntityNode)) {
            return true; // at most one per chunk, an existing node counts
        }
        ConfiguredFeature<?, ?> feature = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(ResourceLocation.fromNamespaceAndPath("thaumaturge", "nodes_wild"));
        if (feature == null) {
            ThaumicConcilium.LOG.warn("Grove life: configured feature thaumaturge:nodes_wild is missing");
            return true;
        }
        RandomSource random = RandomSource.create(job.craterSeed ^ (chunkLong * 0xC2B2AE3D27D4EB4FL));
        for (int tries = 0; tries < 8; tries++) {
            int x = cx * 16 + 5 + random.nextInt(6);
            int z = cz * 16 + 5 + random.nextInt(6);
            if (Math.abs(x - job.centre.getX()) < 5 && Math.abs(z - job.centre.getZ()) < 5) {
                continue; // not over the pond
            }
            int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (feature.place(level, level.getChunkSource().getGenerator(), random, new BlockPos(x, h, z))) {
                return true;
            }
        }
        return true;
    }

    private static boolean tree(ServerLevel level, GrovePaintJob job, int x, int z, boolean silver) {
        if (!loaded(level, x, z, TREE_REACH)) {
            return false;
        }
        // our own shapes (veteran oak, ghost gum) as living Dynamic Trees trees; Thaumaturge's feature stays as the fallback
        LandmarkShapes.Pick pick = dynamicTreesAllowed && DynamicTreesSaplings.available() ? LandmarkShapes.pick(job.craterSeed, x, z, silver) : null;
        int[] why = new int[3]; // offsets turned down: not grove grass or pond, footprint not free, the tree did not stand
        if (pick != null) {
            for (int[] off : OFFSETS) {
                int tx = x + off[0];
                int tz = z + off[1];
                int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, tx, tz);
                BlockPos origin = new BlockPos(tx, h, tz);
                if (!GroveColumns.isGrove(level, origin) || !level.getBlockState(origin.below()).is(Blocks.GRASS_BLOCK) || isPond(job, tx, tz)) {
                    why[0]++;
                    continue;
                }
                if (!footprintFree(level, origin)) {
                    why[1]++;
                    continue;
                }
                if (GroveGrowth.plantLandmark(level, job, origin.below(), silver, pick)) {
                    landmarkShapes++;
                    queueFixedSaplings(job);
                    return true;
                }
                why[2]++;
            }
        }
        ConfiguredFeature<?, ?> feature = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(ResourceLocation.fromNamespaceAndPath("thaumaturge", silver ? "silverwood_tree" : "greatwood_tree"));
        RandomSource random = RandomSource.create(job.craterSeed ^ ((long) x * 31 + z) * 0x9E3779B97F4A7C15L);
        if (feature != null) {
            for (int[] off : OFFSETS) {
                int tx = x + off[0];
                int tz = z + off[1];
                int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, tx, tz);
                BlockPos origin = new BlockPos(tx, h, tz);
                if (!GroveColumns.isGrove(level, origin) || !level.getBlockState(origin.below()).is(Blocks.GRASS_BLOCK) || isPond(job, tx, tz)) {
                    continue;
                }
                if (feature.place(level, level.getChunkSource().getGenerator(), random, origin)) {
                    queueFixedSaplings(job);
                    return true;
                }
            }
        }
        landmarkFallbacks++;
        ThaumicConcilium.LOG.warn("Grove life: no {} tree could be grown near {} {} (offsets turned down: {} not grove grass or pond, {} footprint, {} tree failed; own shapes {}), a sapling instead", silver ? "silverwood" : "greatwood", x, z, why[0], why[1], why[2], pick != null);
        queueFixedSaplings(job);
        return sapling(level, job, x, z, silver);
    }

    /** The trunk's 3 x 3 columns stand on solid ground and have nothing solid in the first 28 blocks above it (plants are cleared by the tree). */
    private static boolean footprintFree(ServerLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (level.getBlockState(p.set(origin.getX() + dx, origin.getY() - 1, origin.getZ() + dz)).getCollisionShape(level, p).isEmpty()) {
                    return false;
                }
                for (int dy = 0; dy < 28; dy++) {
                    BlockState s = level.getBlockState(p.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz));
                    if (!s.isAir() && !s.canBeReplaced() && !(s.getBlock() instanceof BushBlock) && !s.is(BlockTags.LEAVES)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** Tier 3: the landmark tree is done, so the two greatwood and one silverwood saplings of the plan are queued now. */
    private static void queueFixedSaplings(GrovePaintJob job) {
        for (int[] spot : plan(job).spots) {
            if (spot[2] == SAPLING) {
                job.decor.add(new long[] {SAPLING, spot[0], spot[1], spot[3]});
            }
        }
    }

    /** A pond column, or one of its livingrock mound: nothing woody is planted there (the mound is built last and would bury it). */
    private static boolean isPond(GrovePaintJob job, int x, int z) {
        if (GrovePond.moundColumns(job).contains(ChunkPos.asLong(x, z))) {
            return true;
        }
        for (long l : job.pond) {
            BlockPos p = BlockPos.of(l);
            if (p.getX() == x && p.getZ() == z) {
                return true;
            }
        }
        return false;
    }

    /** Every column within 5 blocks of a sapling spot, nearest first (a fixed order, so a reload plants the same). */
    private static final int[][] SAPLING_SEARCH = saplingSearch();

    private static int[][] saplingSearch() {
        List<int[]> list = new ArrayList<>();
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                if (dx * dx + dz * dz <= 25) {
                    list.add(new int[] {dx, dz});
                }
            }
        }
        list.sort((a, b) -> {
            int c = Integer.compare(a[0] * a[0] + a[1] * a[1], b[0] * b[0] + b[1] * b[1]);
            return c != 0 ? c : a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]);
        });
        return list.toArray(new int[0][]);
    }

    /** Times a fixed sapling could not be planted anywhere near its spot (a WARN is logged each time; the tests read it). */
    public static int saplingFailures;

    /**
     * One fixed sapling near (x, z): the first column of the search order that is grove grass, not the pond, and where the sapling can stand. A
     * plant on the spot (the grove's own flowers and grass fill about half the columns) is cleared for it and put back when the sapling cannot
     * stand there after all. 2c tried 8 offsets and gave up silently when meadow plants filled them (the tier 3 flake).
     */
    private static boolean sapling(ServerLevel level, GrovePaintJob job, int x, int z, boolean silver) {
        if (!loaded(level, x, z, 5)) {
            return false;
        }
        for (int[] off : SAPLING_SEARCH) {
            int sx = x + off[0];
            int sz = z + off[1];
            int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz);
            BlockPos at = new BlockPos(sx, h, sz);
            if (!GroveColumns.isGrove(level, at) || !level.getBlockState(at.below()).is(Blocks.GRASS_BLOCK) || isPond(job, sx, sz)) {
                continue;
            }
            BlockState old = level.getBlockState(at);
            boolean cleared = false;
            if (!old.isAir()) {
                if (isSapling(old) || old.getBlock() instanceof DoublePlantBlock || !(old.getBlock() instanceof BushBlock || old.canBeReplaced())) {
                    continue;
                }
                level.setBlock(at, Blocks.AIR.defaultBlockState(), PLACE_FLAGS);
                cleared = true;
            }
            if (plantSapling(level, job, at, silver)) {
                return true;
            }
            if (cleared) {
                level.setBlock(at, old, PLACE_FLAGS);
            }
        }
        saplingFailures++;
        ThaumicConcilium.LOG.warn("Grove life: no {} sapling could be planted within 5 blocks of {} {}", silver ? "silverwood" : "greatwood", x, z);
        return true;
    }
}
