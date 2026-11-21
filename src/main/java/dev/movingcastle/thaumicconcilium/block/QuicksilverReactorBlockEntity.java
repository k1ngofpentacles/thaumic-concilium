package dev.movingcastle.thaumicconcilium.block;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.content.crucible.BlockEntityCrucible;
import com.leclowndu93150.thaumaturge.content.entity.EntitySpecialItem;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import dev.movingcastle.thaumicconcilium.mixin.CrucibleAccessor;
import dev.movingcastle.thaumicconcilium.network.QuicksilverFlowPayload;
import dev.movingcastle.thaumicconcilium.registry.TCBlocks;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Quicksilver Reactor state (original: {@code QuicksilverCrucibleTile}): up to 64 permutatio. Thrown quicksilver adds 2
 * each. While it holds any, isn't powered and its 20 tick cooldown is over, it looks for a Thaumaturge crucible within
 * 8 blocks whose last two aspects combine into a compound, and turns one of each into one of the compound for a point
 * of permutatio. An empty reactor does nothing at all each tick beyond one comparison.
 */
public class QuicksilverReactorBlockEntity extends BlockEntity {
    public static final int MAX = 64;
    private static final int RANGE = 8;
    private static final int COOLDOWN = 20;

    private int permutatio;
    private long nextAt;
    /** Game time of the last burn of a creature standing in it (not saved: a pause of at most 10 ticks). */
    long lastHurt;

    public QuicksilverReactorBlockEntity(BlockPos pos, BlockState state) {
        super(TCBlocks.QUICKSILVER_REACTOR_BLOCK_ENTITY.get(), pos, state);
    }

    public int permutatio() {
        return this.permutatio;
    }

    public void setPermutatio(int amount) {
        int old = this.permutatio;
        this.permutatio = Math.max(0, Math.min(MAX, amount));
        if (old != this.permutatio) {
            this.setChanged();
            if (this.level != null) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
                this.level.updateNeighbourForOutputSignal(this.worldPosition, this.getBlockState().getBlock());
            }
        }
    }

    // ---------------------------------------------------------------- quicksilver in
    /** Original {@code attemptSmelt}: each Thaumaturge quicksilver adds 2 (it only eats what fits). */
    public void attemptSmelt(ItemEntity entity) {
        ItemStack stack = entity.getItem();
        if (!stack.is(TTItems.QUICKSILVER.get()) || this.permutatio >= MAX || this.level == null) {
            return;
        }
        int take = Math.min(stack.getCount(), (MAX - this.permutatio + 1) / 2);
        this.setPermutatio(this.permutatio + 2 * take);
        if (take >= stack.getCount()) {
            entity.discard();
        } else {
            entity.setItem(stack.copyWithCount(stack.getCount() - take));
        }
        this.level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), TTSounds.BUBBLE.get(), SoundSource.BLOCKS, 0.2F,
                1.0F + this.level.random.nextFloat() * 0.4F);
    }

    // ---------------------------------------------------------------- the merge
    public static void serverTick(Level level, BlockPos pos, BlockState state, QuicksilverReactorBlockEntity be) {
        if (be.permutatio <= 0 || level.getGameTime() < be.nextAt || level.hasNeighborSignal(pos)) {
            return;
        }
        be.nextAt = level.getGameTime() + COOLDOWN;
        if (level instanceof ServerLevel server) {
            be.sendQuicksilver(server);
        }
    }

    /** Original: bubbles at the surface while it holds permutatio and isn't powered. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, QuicksilverReactorBlockEntity be) {
        if (be.permutatio <= 0 || level.hasNeighborSignal(pos)) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            double x = pos.getX() + (5 + level.random.nextInt(22)) / 32.0;
            double z = pos.getZ() + (5 + level.random.nextInt(22)) / 32.0;
            level.addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.6F, 0.6F, 0.6F), 0.8F),
                    x, pos.getY() + 0.3 + be.permutatio / 100.0, z, 0.0, 0.02, 0.0);
        }
    }

    private void sendQuicksilver(ServerLevel level) {
        List<BlockEntityCrucible> crucibles = new ArrayList<>();
        BlockPos pos = this.worldPosition;
        for (int cx = (pos.getX() - RANGE) >> 4; cx <= (pos.getX() + RANGE) >> 4; cx++) {
            for (int cz = (pos.getZ() - RANGE) >> 4; cz <= (pos.getZ() + RANGE) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos p = be.getBlockPos();
                    if (be instanceof BlockEntityCrucible c && Math.abs(p.getX() - pos.getX()) <= RANGE
                            && Math.abs(p.getY() - pos.getY()) <= RANGE && Math.abs(p.getZ() - pos.getZ()) <= RANGE) {
                        crucibles.add(c);
                    }
                }
            }
        }
        // The original took the first in its x, y, z scan; the nearest is the stable equivalent.
        crucibles.sort((a, b) -> Double.compare(a.getBlockPos().distSqr(pos), b.getBlockPos().distSqr(pos)));
        Map<Pair, Holder<IAspect>> compounds = compounds(level);
        for (BlockEntityCrucible crucible : crucibles) {
            AspectList list = ((CrucibleAccessor) crucible).thaumicconcilium$getAspects();
            List<AspectInstance> e = list.entries();
            if (e.size() < 2) {
                continue;
            }
            AspectInstance last = e.get(e.size() - 1);
            AspectInstance second = e.get(e.size() - 2);
            Holder<IAspect> combo = compounds.get(Pair.of(second.aspect(), last.aspect()));
            if (combo == null) {
                continue;
            }
            AspectList next = AspectList.EMPTY;
            for (int i = 0; i < e.size() - 2; i++) {
                next = next.add(e.get(i));
            }
            next = next.add(combo, 1);
            if (second.amount() > 1) {
                next = next.add(second.aspect(), second.amount() - 1);
            }
            if (last.amount() > 1) {
                next = next.add(last.aspect(), last.amount() - 1);
            }
            ((CrucibleAccessor) crucible).thaumicconcilium$setAspects(next);
            crucible.setChanged();
            crucible.syncToClient();
            this.setPermutatio(this.permutatio - 1);
            PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(pos),
                    new QuicksilverFlowPayload(crucible.getBlockPos(), pos));
            return;
        }
    }

    /** An unordered aspect pair. */
    private record Pair(ResourceLocation a, ResourceLocation b) {
        static Pair of(Holder<IAspect> x, Holder<IAspect> y) {
            ResourceLocation kx = x.unwrapKey().orElseThrow().location();
            ResourceLocation ky = y.unwrapKey().orElseThrow().location();
            return kx.compareTo(ky) <= 0 ? new Pair(kx, ky) : new Pair(ky, kx);
        }
    }

    @Nullable
    private static Registry<IAspect> cachedFor;
    private static Map<Pair, Holder<IAspect>> cache = Map.of();

    /** Every aspect made of exactly two others, by that pair (either order); rebuilt when the registry changes. */
    private static synchronized Map<Pair, Holder<IAspect>> compounds(ServerLevel level) {
        Registry<IAspect> registry = level.registryAccess().registryOrThrow(IAspect.REGISTRY_KEY);
        if (registry != cachedFor) {
            Map<Pair, Holder<IAspect>> map = new HashMap<>();
            registry.holders().forEach(h -> {
                List<Holder<IAspect>> parts = h.value().components();
                if (parts.size() == 2 && parts.get(0).unwrapKey().isPresent() && parts.get(1).unwrapKey().isPresent()) {
                    map.putIfAbsent(Pair.of(parts.get(0), parts.get(1)), h);
                }
            });
            cache = map;
            cachedFor = registry;
        }
        return cache;
    }

    // ---------------------------------------------------------------- saving, syncing
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("permutatio", this.permutatio);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.permutatio = tag.getInt("permutatio");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    static boolean isNormalItem(ItemEntity item) {
        return !(item instanceof EntitySpecialItem);
    }
}
