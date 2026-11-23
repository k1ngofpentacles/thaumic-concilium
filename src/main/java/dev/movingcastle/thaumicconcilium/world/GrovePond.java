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
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

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
        if (job.pondPlaced || job.tier < 3 || !GroveLife.botania()) {
            return true;
        }
        for (long l : job.pond) {
            if (!level.hasChunkAt(BlockPos.of(l))) {
                return false;
            }
        }
        place(level, job);
        return true;
    }

    /** Fills the 13 reserved columns with the pond (one block deep, on the crater floor) and sets up the ledger. */
    public static void place(ServerLevel level, GrovePaintJob job) {
        BlockState pond = TCBlocks.MANA_POND.get().defaultBlockState();
        int placed = 0;
        for (long l : job.pond) {
            BlockPos at = BlockPos.of(l).above();
            BlockState old = level.getBlockState(at);
            if (!old.hasBlockEntity() && (old.isAir() || old.canBeReplaced() || old.getCollisionShape(level, at).isEmpty())) {
                level.setBlock(at, pond, 18);
                placed++;
            }
        }
        job.pondPlaced = true;
        if (!job.ledgerReady) {
            int nodes = nodes(level, job, true).size();
            job.residual = initialResidual(chunksOf(job), nodes);
            job.overdrawn = 0;
            job.ledgerReady = true;
        }
        ThaumicConcilium.LOG.info("Grove pond at {}: {} of {} blocks placed, residual {} mana", job.centre, placed, job.pond.size(), job.residual);
        dirty(level);
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
