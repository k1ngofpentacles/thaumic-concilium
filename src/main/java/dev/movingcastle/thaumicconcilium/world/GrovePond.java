package dev.movingcastle.thaumicconcilium.world;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode;
import com.leclowndu93150.thaumaturge.content.aura.node.NodeHostBlock;
import com.leclowndu93150.thaumaturge.content.aura.node.NodeLocationIndex;
import com.leclowndu93150.thaumaturge.content.entity.EntityFluxRift;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.registry.TCBlocks;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import dev.movingcastle.thaumicconcilium.entity.VesselCrater;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * Phase 5d cycle 3: the grove's mana pond, with no Botania types (those are in {@code compat/BotaniaPond}). The pond blocks are placed when a
 * tier 3 or 4 job finishes (only with Botania); a spreader touching one draws from the job's ledger. The ledger starts at a residual sized by the
 * grove, never refills by itself, and when a draw finds it short drains vis from the aura nodes inside the grove (1 vis = a configured amount
 * of mana, fullest node first). When every node is empty, each further {@code overdrawStep} mana drawn breaks the weakest node into a
 * flux rift. Everything happens inside a draw: there is no ticker.
 */
public final class GrovePond {
    private GrovePond() {
    }

    /** The grove's node positions, re-read at most every {@code nodeRefreshSeconds}. */
    public static final class NodeCache {
        public final List<BlockPos> positions = new ArrayList<>();
        public long refreshedAt = Long.MIN_VALUE;
    }

    /** Node-list re-reads, vis drained and nodes broken since the game started (the tests read them to show that nothing runs without draws). */
    public static long refreshes;
    public static long draws;
    public static long breaksDone;

    // ---------------------------------------------------------------- pure rules
    /** The residual a new pond starts with. */
    public static long initialResidual(int chunks, int nodes) {
        return (long) chunks * TCConfig.POND_MANA_PER_CHUNK.get() + (long) nodes * TCConfig.POND_MANA_PER_NODE.get();
    }

    /** Painted grove chunks of a job: its painted quart columns, 16 to a chunk, rounded. */
    public static int chunksOf(GrovePaintJob job) {
        return (job.painted + 8) / 16;
    }

    /** The flux rift a broken node becomes: linear in the node's capacity (total of its base aspects), clamped to the configured range. */
    public static int riftSizeFor(int capacity) {
        int size = (int) Math.round(capacity * TCConfig.POND_RIFT_PER_CAPACITY.get());
        return Math.max(TCConfig.POND_RIFT_MIN.get(), Math.min(Math.max(TCConfig.POND_RIFT_MIN.get(), TCConfig.POND_RIFT_MAX.get()), size));
    }

    public static boolean isPondColumn(GrovePaintJob job, int x, int z) {
        for (long l : job.pond) {
            BlockPos p = BlockPos.of(l);
            if (p.getX() == x && p.getZ() == z) {
                return true;
            }
        }
        return false;
    }

    /** The open or finished job whose pond has a block in this column, or null. A lookup that creates nothing. */
    @Nullable
    public static GrovePaintJob jobAt(ServerLevel level, BlockPos pos) {
        VesselAftermath data = VesselAftermath.peek(level);
        if (data == null) {
            return null;
        }
        for (GrovePaintJob j : data.jobs()) {
            if (j.ledgerReady && isPondColumn(j, pos.getX(), pos.getZ())) {
                return j;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- placing the pond
    /**
     * Called by the painter when everything else of a job is done. Returns whether the job may be marked done: at once without Botania (no
     * pond), once the pond stands otherwise (a pond whose chunk is not loaded waits for a later beat).
     */
    public static boolean finish(ServerLevel level, GrovePaintJob job) {
        if (job.tier < 3 || !GroveLife.botania()) {
            return true;
        }
        if (job.pondPlaced) {
            initLedger(level, job); // placed early (the finale): only the ledger waits for the whole grove
            return true;
        }
        if (!chunksLoaded(level, job)) {
            return false;
        }
        place(level, job);
        return true;
    }

    /** The pond's and the mound's chunks are all loaded. */
    private static boolean chunksLoaded(ServerLevel level, GrovePaintJob job) {
        for (long l : job.pond) {
            if (!level.hasChunkAt(BlockPos.of(l))) {
                return false;
            }
        }
        for (long column : moundColumns(job)) {
            if (!level.hasChunkAt(new BlockPos(ChunkPos.getX(column), job.centre.getY(), ChunkPos.getZ(column)))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Cycle 4e (the finale): the painter calls this as soon as the crater's rings are done, so the pond (and its mound) stand under the swell before
     * the light rises from it. Only the blocks: the ledger is sized at the job's end, when the grove's chunks and nodes are known, and until then
     * yields nothing. False when it is not time or a chunk is not loaded (tried again next step).
     */
    public static boolean placeEarly(ServerLevel level, GrovePaintJob job) {
        if (job.pondPlaced || job.tier < 3 || !GroveLife.botania() || !chunksLoaded(level, job)) {
            return false;
        }
        placePondAndMound(level, job);
        return true;
    }

    /** Blocks the last pond placement changed besides the pond blocks themselves: beds filled, columns cut down, banks raised (for the tests and the log). */
    public static int lastFilled;
    public static int lastCut;
    public static int lastBanked;

    /**
     * Sets the 13 pond blocks, all at one height (cycle 4d: the pool lies IN the crater floor, see {@link PondShape}), and sets up the ledger.
     * Only what a level pool needs is changed: the pond block takes the place of the floor block; a column whose floor is lower gets a bed
     * of dirt under it; a column whose floor is higher loses the blocks above the pond block (up to where the crater left its floor); and a
     * neighbour column that would leave a side of the pool open is raised to the pool's level with the ground it already has. Block
     * entities, anchors and blast-proof blocks are never touched (a pond block that would replace one is skipped).
     */
    public static void place(ServerLevel level, GrovePaintJob job) {
        placePondAndMound(level, job);
        initLedger(level, job);
    }

    /**
     * The one place the pond's water, its basin and its livingrock mound are set (a swap point: a later round of the mound rule replaces what this
     * calls, {@code placeMound}). Does not touch the ledger.
     */
    public static void placePondAndMound(ServerLevel level, GrovePaintJob job) {
        BlockState pond = TCBlocks.MANA_POND.get().defaultBlockState();
        int placed = 0;
        lastFilled = 0;
        lastCut = 0;
        lastBanked = 0;
        VesselCrater crater = new VesselCrater(job.centre, job.craterRadius, job.craterSeed);
        Block rock = mounded(job) ? botaniaBlock("livingrock") : null;
        lastMound = rock == null ? 0 : placeMound(level, job, crater, rock);
        for (long l : job.pond) {
            BlockPos bed = BlockPos.of(l);
            BlockPos at = bed.above();
            BlockState old = level.getBlockState(at);
            if (!fixed(level, at, old)) {
                level.setBlock(at, pond, 18);
                placed++;
            }
            BlockState under = level.getBlockState(bed);
            if (rock != null) {
                // the basin: livingrock under the water, and whatever lies under that made solid
                if (!under.is(rock) && !fixed(level, bed, under)) {
                    lastFilled += under.isCollisionShapeFullBlock(level, bed) ? 0 : 1;
                    level.setBlock(bed, rock.defaultBlockState(), 18);
                }
                BlockPos floor = bed.below();
                BlockState ground = level.getBlockState(floor);
                if (!ground.isCollisionShapeFullBlock(level, floor) && !fixed(level, floor, ground)) {
                    level.setBlock(floor, Blocks.DIRT.defaultBlockState(), 18);
                    lastFilled++;
                } else if (ground.is(Blocks.GRASS_BLOCK)) {
                    level.setBlock(floor, Blocks.DIRT.defaultBlockState(), 18);
                }
            } else if (!under.isCollisionShapeFullBlock(level, bed) && !fixed(level, bed, under)) {
                level.setBlock(bed, Blocks.DIRT.defaultBlockState(), 18);
                lastFilled++;
            }
            // a shallower column: open the pool to the sky, up to the floor the crater left there
            int floor = job.centre.getY() - crater.cut(bed.getX() - job.centre.getX(), bed.getZ() - job.centre.getZ());
            for (int y = at.getY() + 1; y <= floor; y++) {
                BlockPos p = new BlockPos(at.getX(), y, at.getZ());
                BlockState s = level.getBlockState(p);
                if (!s.isAir() && !fixed(level, p, s)) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 18);
                    lastCut++;
                }
            }
        }
        // the banks: every column beside the pool holds it in
        for (long l : job.pond) {
            BlockPos at = BlockPos.of(l).above();
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos bank = at.relative(d);
                BlockState s = level.getBlockState(bank);
                if (isPondColumn(job, bank.getX(), bank.getZ()) || s.isCollisionShapeFullBlock(level, bank) || fixed(level, bank, s)) {
                    continue;
                }
                BlockPos below = bank.below();
                BlockState ground = level.getBlockState(below);
                BlockState with = ground.isCollisionShapeFullBlock(level, below) && !ground.hasBlockEntity() ? ground : Blocks.DIRT.defaultBlockState();
                level.setBlock(bank, with, 18);
                if (with.is(Blocks.GRASS_BLOCK)) {
                    level.setBlock(below, Blocks.DIRT.defaultBlockState(), 18); // the turf moves up a block
                }
                BlockPos over = bank.above();
                BlockState plant = level.getBlockState(over);
                if (!plant.isAir() && !plant.canSurvive(level, over)) {
                    level.setBlock(over, Blocks.AIR.defaultBlockState(), 18); // whatever stood half in the fill (a double plant's top)
                }
                lastBanked++;
            }
        }
        job.pondPlaced = true;
        ThaumicConcilium.LOG.info("Grove pond at {}: {} of {} blocks placed ({} mound blocks, {} bed blocks filled, {} cut away, {} bank blocks raised)",
                job.centre, placed, job.pond.size(), lastMound, lastFilled, lastCut, lastBanked);
        dirty(level);
    }

    /** Sizes the pond's ledger (once): the grove's chunks and nodes are known only when the job finishes. Until then {@code ledgerReady} is false and nothing is drawn. */
    public static void initLedger(ServerLevel level, GrovePaintJob job) {
        if (!job.ledgerReady) {
            int nodes = nodes(level, job, true).size();
            job.residual = initialResidual(chunksOf(job), nodes);
            job.overdrawn = 0;
            job.ledgerReady = true;
            ThaumicConcilium.LOG.info("Grove pond ledger at {}: residual {} mana", job.centre, job.residual);
            dirty(level);
        }
    }

    // ---------------------------------------------------------------- the livingrock mound (cycle 4e)
    /** Blocks the last placement set for the mound (rock, bricks, slabs, turf and its fill; not the flora on it). */
    public static int lastMound;

    /** Whether a job's pond gets the mound: the pond's own gate (tier 3 or 4, Botania). */
    public static boolean mounded(GrovePaintJob job) {
        return job.tier >= 3 && GroveLife.botania();
    }

    /** The pool and the mound of a job, from its crater alone (nothing is saved: the rule is pure). */
    public static PondShape poolOf(VesselCrater crater) {
        return PondShape.choose(crater::cut, crater.seed());
    }

    public static List<PondMound.Cell> moundOf(VesselCrater crater) {
        return PondMound.plan(crater::cut, crater.seed(), poolOf(crater));
    }

    /**
     * The mound's block columns, packed as {@code ChunkPos.asLong(x, z)} like {@code GrovePainter.pondColumns}; empty when the job gets no
     * mound. Cached on the job. Saplings and trees keep off them as they keep off the pond.
     */
    public static Set<Long> moundColumns(GrovePaintJob job) {
        if (job.moundColumns == null) {
            Set<Long> set = new HashSet<>();
            if (mounded(job)) {
                for (PondMound.Cell c : moundOf(new VesselCrater(job.centre, job.craterRadius, job.craterSeed))) {
                    set.add(ChunkPos.asLong(job.centre.getX() + c.dx(), job.centre.getZ() + c.dz()));
                }
            }
            job.moundColumns = set;
        }
        return job.moundColumns;
    }

    @Nullable
    private static Block botaniaBlock(String path) {
        return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("botania", path)).orElse(null);
    }

    /**
     * Builds the mound, once, before the pond blocks are set (no ticker; about 50 blocks). The pond's bed is the layer under the mound's
     * top, so the top layer is the pond blocks' own. A column buried under solid ground is skipped (the ground holds the water there), and
     * block entities, anchors and blast-proof blocks are never replaced. Returns the blocks set.
     */
    private static int placeMound(ServerLevel level, GrovePaintJob job, VesselCrater crater, Block rock) {
        if (job.pond.isEmpty()) {
            return 0;
        }
        int floorY = BlockPos.of(job.pond.get(0)).getY() - (PondMound.TOP - 1); // F: the bed is F (PondMound.TOP = 1), the pond blocks and the mound's top layer F+1
        Block mossy = botaniaBlock("mossy_livingrock_bricks");
        Block cracked = botaniaBlock("cracked_livingrock_bricks");
        Block slab = botaniaBlock("livingrock_slab");
        Block stairs = botaniaBlock("livingrock_stairs");
        long seed = crater.seed();
        int set = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (PondMound.Cell c : moundOf(crater)) {
            int x = job.centre.getX() + c.dx();
            int z = job.centre.getZ() + c.dz();
            if (c.kind() == PondMound.Kind.MOSS || c.kind() == PondMound.Kind.FLORA) {
                set += placeApron(level, job, crater, c, x, z);
                continue;
            }
            int topY = floorY + c.top();
            if (!level.hasChunkAt(p.set(x, topY, z))) {
                continue;
            }
            BlockState over = level.getBlockState(p.set(x, topY + 1, z));
            if (over.isCollisionShapeFullBlock(level, p)) {
                continue; // buried: the ground stands higher here than the crater's rule says
            }
            BlockState top = switch (c.kind()) {
                case MOSSY -> (mossy == null ? rock : mossy).defaultBlockState();
                case CRACKED -> (cracked == null ? rock : cracked).defaultBlockState();
                case SLAB -> (slab == null ? rock : slab).defaultBlockState();
                case STAIR -> stairs == null ? rock.defaultBlockState() : stairState(stairs, c.low());
                case TURF -> Blocks.GRASS_BLOCK.defaultBlockState();
                default -> rock.defaultBlockState();
            };
            BlockState fill = c.kind() == PondMound.Kind.TURF ? Blocks.DIRT.defaultBlockState() : rock.defaultBlockState();
            BlockState old = level.getBlockState(p.set(x, topY, z));
            if (fixed(level, p, old)) {
                continue;
            }
            level.setBlock(p, top, 18);
            set++;
            // under the top: the ring beside the water is rock down to the bed's layer whatever was there; the rest is filled down to the ground
            for (int y = topY - 1; y >= floorY - 3; y--) {
                BlockState s = level.getBlockState(p.set(x, y, z));
                if (fixed(level, p, s)) {
                    break;
                }
                boolean ring = c.touches() && y > floorY;
                if (!ring && s.isCollisionShapeFullBlock(level, p)) {
                    if (s.is(Blocks.GRASS_BLOCK)) {
                        level.setBlock(p, Blocks.DIRT.defaultBlockState(), 18); // turf under rock
                    }
                    break;
                }
                if (!s.is(fill.getBlock())) {
                    level.setBlock(p, fill, 18);
                    set++;
                }
            }
            // what grows on it; a plant the rock cut in half goes
            p.set(x, topY + 1, z);
            if (!over.isAir() && (!over.canSurvive(level, p) || over.canBeReplaced())) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 18);
                over = Blocks.AIR.defaultBlockState();
            }
            if (!over.isAir()) {
                continue;
            }
            if (c.carpet()) {
                level.setBlock(p, Blocks.MOSS_CARPET.defaultBlockState(), 18);
            } else if (c.flora()) {
                plant(level, p, seed, c);
            }
        }
        return set;
    }

    /** A stair of the mound: its low step points {@code low} (e, w, s, n), away from the pool; the block's facing is the opposite. */
    public static BlockState stairState(Block stairs, char low) {
        Direction lowDir = switch (low) {
            case 'e' -> Direction.EAST;
            case 'w' -> Direction.WEST;
            case 's' -> Direction.SOUTH;
            default -> Direction.NORTH;
        };
        return stairs.defaultBlockState().setValue(StairBlock.FACING, lowDir.getOpposite()).setValue(StairBlock.HALF, Half.BOTTOM);
    }

    /** A small plant at {@code p} if it is air and the block under will carry it: short grass, a fern or a small flower, by the column's seeded roll. */
    private static void plant(ServerLevel level, BlockPos.MutableBlockPos p, long seed, PondMound.Cell c) {
        if (!level.getBlockState(p).isAir()) {
            return;
        }
        double roll = PondMound.unit(c.dx(), c.dz(), seed, 18);
        Block b = roll < 0.35 ? Blocks.SHORT_GRASS : roll < 0.55 ? Blocks.FERN : roll < 0.7 ? Blocks.DANDELION : roll < 0.82 ? Blocks.POPPY
                : roll < 0.92 ? Blocks.AZURE_BLUET : Blocks.CORNFLOWER;
        BlockState s = b.defaultBlockState();
        if (s.canSurvive(level, p)) {
            level.setBlock(p, s, 18);
        }
    }

    /**
     * The apron beside the stone (a MOSS or FLORA cell): finds the column's real top soil (grass, dirt, moss: the {@code dirt} tag) within two
     * blocks of where the crater's rule puts the ground, and turns it to a moss block (MOSS) and/or plants a small plant on it. Block entities
     * and anchors are left alone; a column of the pond or the mound's own plan is never touched. Returns blocks set (the moss block).
     */
    private static int placeApron(ServerLevel level, GrovePaintJob job, VesselCrater crater, PondMound.Cell c, int x, int z) {
        int groundY = job.centre.getY() - crater.cut(c.dx(), c.dz());
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        if (!level.hasChunkAt(p.set(x, groundY, z)) || isPondColumn(job, x, z)) {
            return 0;
        }
        for (int y = groundY + 2; y >= groundY - 2; y--) {
            BlockState s = level.getBlockState(p.set(x, y, z));
            if (!s.is(BlockTags.DIRT) || fixed(level, p, s)) {
                continue;
            }
            BlockState over = level.getBlockState(p.set(x, y + 1, z));
            if (!over.isAir() && !over.canBeReplaced()) {
                return 0; // something solid or a tree stands on it
            }
            int set = 0;
            if (c.kind() == PondMound.Kind.MOSS) {
                level.setBlock(p.set(x, y, z), Blocks.MOSS_BLOCK.defaultBlockState(), 18);
                set = 1;
            }
            if (c.flora()) {
                p.set(x, y + 1, z);
                if (over.canBeReplaced() && !over.isAir() && !over.is(Blocks.MOSS_CARPET)) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 18); // the painter's own small plant makes room for this one
                }
                plant(level, p, crater.seed(), c);
            }
            return set;
        }
        return 0;
    }

    /** What the pond never replaces: a block entity, an anchor shard, anything blast-proof (the crater's own rule). */
    private static boolean fixed(ServerLevel level, BlockPos pos, BlockState state) {
        return state.hasBlockEntity() || (!state.isAir() && VesselCrater.spared(state));
    }

    private static void dirty(ServerLevel level) {
        VesselAftermath data = VesselAftermath.peek(level);
        if (data != null) {
            data.setDirty();
        }
    }

    // ---------------------------------------------------------------- the nodes inside the grove
    /**
     * The aura nodes inside the grove that are loaded and still standing. The list is re-read from Thaumaturge's {@code NodeLocationIndex}
     * (its saved form: the index has no public listing) at most every {@code nodeRefreshSeconds}, or at once when forced.
     */
    public static List<BlockPos> nodes(ServerLevel level, GrovePaintJob job, boolean force) {
        NodeCache cache = job.nodeCache;
        if (cache == null) {
            cache = new NodeCache();
            job.nodeCache = cache;
        }
        long now = level.getGameTime();
        if (force || cache.refreshedAt == Long.MIN_VALUE || now - cache.refreshedAt >= TCConfig.POND_NODE_REFRESH_SECONDS.get() * 20L || now < cache.refreshedAt) {
            refreshes++;
            cache.refreshedAt = now;
            cache.positions.clear();
            CompoundTag saved = NodeLocationIndex.get(level).save(new CompoundTag(), level.registryAccess());
            ListTag list = saved.getList("nodes", 10);
            double reach = job.radius + 24.0;
            for (int i = 0; i < list.size(); i++) {
                CompoundTag c = list.getCompound(i);
                BlockPos pos = BlockPos.CODEC.parse(NbtOps.INSTANCE, c.get("pos")).result().orElse(null);
                if (pos == null || pos.distSqr(job.centre) > reach * reach || !GroveColumns.isGrove(level, pos)) {
                    continue;
                }
                if (!cache.positions.contains(pos)) {
                    cache.positions.add(pos);
                }
            }
        }
        // a node that is gone or whose chunk has unloaded since the read is dropped from the answer, not from the cache's next read
        List<BlockPos> live = new ArrayList<>();
        for (BlockPos p : cache.positions) {
            if (nodeAt(level, p) != null) {
                live.add(p);
            }
        }
        return live;
    }

    @Nullable
    private static BlockEntityNode nodeAt(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof BlockEntityNode n ? n : null;
    }

    private static int visOf(BlockEntityNode n) {
        return n.getAspects().totalAmount();
    }

    /** Drains one vis (one point of one aspect) from the loaded grove node holding the most; false when every one is empty. */
    private static boolean drainOneVis(ServerLevel level, GrovePaintJob job) {
        BlockEntityNode best = null;
        int most = 0;
        for (BlockPos p : nodes(level, job, false)) {
            BlockEntityNode n = nodeAt(level, p);
            if (n != null && visOf(n) > most) {
                most = visOf(n);
                best = n;
            }
        }
        if (best == null) {
            return false;
        }
        AspectInstance richest = null;
        for (AspectInstance a : best.getAspects().entries()) {
            if (a.amount() > 0 && (richest == null || a.amount() > richest.amount())) {
                richest = a;
            }
        }
        if (richest != null && best.drain(richest.aspect(), 1)) {
            job.visDrained++;
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- the ledger
    /** What a spreader sees as the pond's mana: the residual, topped up by one vis when empty, or the overdraw offer when every node is empty. */
    public static int offer(ServerLevel level, GrovePaintJob job) {
        if (!job.ledgerReady) {
            return 0;
        }
        if (job.residual <= 0 && drainOneVis(level, job)) {
            job.residual += TCConfig.POND_MANA_PER_VIS.get();
            dirty(level);
        }
        if (job.residual > 0) {
            return (int) Math.min(Integer.MAX_VALUE, job.residual);
        }
        return nodes(level, job, false).isEmpty() ? 0 : TCConfig.POND_OVERDRAW_OFFER.get();
    }

    /**
     * Draws mana from the pond and returns what was given (less than asked only when the residual is gone and no node is left to feed or
     * break). The residual goes first; then node vis refills it; with every node empty the rest is overdraw.
     */
    public static long draw(ServerLevel level, GrovePaintJob job, long want) {
        if (!job.ledgerReady || want <= 0) {
            return 0;
        }
        draws++;
        long perVis = TCConfig.POND_MANA_PER_VIS.get();
        long take = Math.min(job.residual, want);
        job.residual -= take;
        long short_ = want - take;
        while (short_ > 0) {
            long needVis = (short_ + perVis - 1) / perVis;
            long got = 0;
            while (got < needVis && drainOneVis(level, job)) {
                got++;
            }
            if (got > 0) {
                long mana = got * perVis;
                long used = Math.min(mana, short_);
                short_ -= used;
                job.residual += mana - used;
                continue;
            }
            // every grove node is empty: the draw goes on, and each overdrawStep of it breaks the weakest node
            if (nodes(level, job, false).isEmpty()) {
                break; // nothing left to give
            }
            job.overdrawn += short_;
            short_ = 0;
            while (job.overdrawn >= TCConfig.POND_OVERDRAW_STEP.get()) {
                job.overdrawn -= TCConfig.POND_OVERDRAW_STEP.get();
                if (!breakWeakest(level, job)) {
                    job.overdrawn = 0;
                    break;
                }
            }
        }
        long given = want - short_;
        job.drawn += given;
        dirty(level);
        return given;
    }

    /**
     * Breaks the grove's weakest node (smallest total capacity, then the lowest position) into a flux rift: the rift is made through
     * Thaumaturge's own entity, the node block is removed through its own path (the host block's depleted state for a silverwood log). The
     * rift's taint repaints grove columns by Thaumaturge's rules; nothing here repaints.
     */
    public static boolean breakWeakest(ServerLevel level, GrovePaintJob job) {
        BlockPos weakest = null;
        int weakestBase = Integer.MAX_VALUE;
        for (BlockPos p : nodes(level, job, true)) {
            BlockEntityNode n = nodeAt(level, p);
            if (n == null) {
                continue;
            }
            int base = n.getAspectsBase().totalAmount();
            if (base < weakestBase || base == weakestBase && p.asLong() < weakest.asLong()) {
                weakestBase = base;
                weakest = p;
            }
        }
        if (weakest == null) {
            return false;
        }
        BlockState state = level.getBlockState(weakest);
        EntityFluxRift rift = TTEntities.FLUX_RIFT.get().create(level);
        if (rift != null) {
            rift.reseed(level.random.nextInt());
            rift.moveTo(weakest.getX() + 0.5, weakest.getY() + 0.5, weakest.getZ() + 0.5, level.random.nextInt(360), 0.0F);
            rift.resize(riftSizeFor(weakestBase));
            level.addFreshEntity(rift);
        }
        if (state.getBlock() instanceof NodeHostBlock host) {
            level.setBlock(weakest, host.depletedState(state), 3);
        } else {
            level.removeBlock(weakest, false);
        }
        job.breaks++;
        breaksDone++;
        if (job.nodeCache != null) {
            job.nodeCache.positions.remove(weakest);
        }
        ThaumicConcilium.LOG.warn("Grove pond at {} overdrawn: node at {} (capacity {}) broken into a flux rift ({} so far)", job.centre, weakest, weakestBase, job.breaks);
        dirty(level);
        return true;
    }
}
