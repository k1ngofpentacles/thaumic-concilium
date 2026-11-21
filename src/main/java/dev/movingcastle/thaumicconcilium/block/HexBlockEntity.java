package dev.movingcastle.thaumicconcilium.block;

import dev.movingcastle.thaumicconcilium.registry.TCBlocks;
import dev.movingcastle.thaumicconcilium.rift.Hexes;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One block of a Hex of Predictability (original: {@code HexOfPredictabilityTile}). The centre (master) holds the
 * essentia the Vis Condensers send, the heat that times a transmutation, and whether a rift is held; the eight around
 * it only remember the block they replaced. A lesser hex (port addition) is the same ring around any centre.
 */
public class HexBlockEntity extends BlockEntity {
    private boolean master;
    private boolean lesser;
    private boolean hasRift;
    private int heat;
    private final Map<ResourceLocation, Integer> essentia = new LinkedHashMap<>();
    /** The block this one replaced (bedrock or the lesser hex's centre, ancient stone for the ring), restored on break. */
    private BlockState replaced = Blocks.AIR.defaultBlockState();

    public HexBlockEntity(BlockPos pos, BlockState state) {
        super(TCBlocks.HEX_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HexBlockEntity hex) {
        if (hex.master && level instanceof ServerLevel server) {
            Hexes.tickMaster(server, pos, hex);
        }
    }

    public void setup(boolean master, boolean lesser, BlockState replaced) {
        this.master = master;
        this.lesser = lesser;
        this.replaced = replaced;
        this.sync();
    }

    public boolean isMaster() {
        return this.master;
    }

    public boolean isLesser() {
        return this.lesser;
    }

    public boolean hasRift() {
        return this.hasRift;
    }

    public void setHasRift(boolean hasRift) {
        if (this.hasRift != hasRift) {
            this.hasRift = hasRift;
            if (!hasRift) {
                this.essentia.clear();
                this.heat = 0;
            }
            this.sync();
        }
    }

    public int heat() {
        return this.heat;
    }

    public void setHeat(int heat) {
        this.heat = Math.max(0, heat);
        this.setChanged();
    }

    public Map<ResourceLocation, Integer> essentia() {
        return this.essentia;
    }

    public void addEssentia(ResourceLocation aspect, int amount) {
        this.essentia.merge(aspect, amount, Integer::sum);
        this.sync();
    }

    public void clearEssentia() {
        this.essentia.clear();
        this.sync();
    }

    public BlockState replaced() {
        return this.replaced;
    }

    public void sync() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("master", this.master);
        tag.putBoolean("lesser", this.lesser);
        tag.putBoolean("hasRift", this.hasRift);
        tag.putInt("heat", this.heat);
        CompoundTag ess = new CompoundTag();
        this.essentia.forEach((k, v) -> ess.putInt(k.toString(), v));
        tag.put("essentia", ess);
        tag.put("replaced", NbtUtils.writeBlockState(this.replaced));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.master = tag.getBoolean("master");
        this.lesser = tag.getBoolean("lesser");
        this.hasRift = tag.getBoolean("hasRift");
        this.heat = tag.getInt("heat");
        this.essentia.clear();
        CompoundTag ess = tag.getCompound("essentia");
        for (String k : ess.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(k);
            if (id != null) {
                this.essentia.put(id, ess.getInt(k));
            }
        }
        this.replaced = NbtUtils.readBlockState(registries.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK), tag.getCompound("replaced"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
