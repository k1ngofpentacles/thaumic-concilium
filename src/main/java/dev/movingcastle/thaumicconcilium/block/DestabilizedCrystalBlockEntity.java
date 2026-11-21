package dev.movingcastle.thaumicconcilium.block;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import dev.movingcastle.thaumicconcilium.registry.TCBlocks;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A destabilized crystal cluster's charge (original: {@code DestabilizedCrystalTile}): one aspect and how much of it.
 * Feeding it an essentia crystal of its aspect adds one; an empty cluster takes any aspect.
 */
public class DestabilizedCrystalBlockEntity extends BlockEntity {
    @Nullable
    private ResourceLocation aspect;
    private int capacity = 1;

    public DestabilizedCrystalBlockEntity(BlockPos pos, BlockState state) {
        super(TCBlocks.DESTABILIZED_CRYSTAL_BLOCK_ENTITY.get(), pos, state);
    }

    @Nullable
    public ResourceLocation aspect() {
        return this.aspect;
    }

    public int capacity() {
        return this.capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = Math.max(0, capacity);
        this.sync();
    }

    /** Sets the charge outright (QA command). */
    public void charge(ResourceLocation aspect, int amount) {
        this.aspect = aspect;
        this.capacity = amount;
        this.sync();
    }

    /** The aspect's colour (white when empty), for the block tint and the arcs. */
    public int color() {
        if (this.aspect == null || this.level == null) {
            return 0xFFFFFF;
        }
        return this.level.registryAccess().registry(IAspect.REGISTRY_KEY)
                .flatMap(r -> r.getOptional(this.aspect)).map(IAspect::color).orElse(0xFFFFFF);
    }

    /** Original handleInputStack: an essentia crystal of the cluster's aspect adds one; an empty cluster takes any. */
    public boolean feed(Player player, ItemStack stack) {
        if (!stack.is(TTItems.ESSENTIA_CRYSTAL.get())) {
            return false;
        }
        AspectInstance held = stack.get(TTDataComponents.CRYSTAL_ASPECT.get());
        if (held == null) {
            return false;
        }
        ResourceLocation id = held.aspect().unwrapKey().map(k -> k.location()).orElse(null);
        if (id == null) {
            return false;
        }
        if (id.equals(this.aspect)) {
            this.capacity++;
        } else if (this.aspect == null || this.capacity == 0) {
            this.aspect = id;
        } else {
            return false;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        this.sync();
        return true;
    }

    private void sync() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("capacity", this.capacity);
        if (this.aspect != null) {
            tag.putString("aspect", this.aspect.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.capacity = tag.getInt("capacity");
        this.aspect = tag.contains("aspect") ? ResourceLocation.tryParse(tag.getString("aspect")) : null;
        if (this.level != null && this.level.isClientSide()) {
            this.requestModelDataUpdate();
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
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
