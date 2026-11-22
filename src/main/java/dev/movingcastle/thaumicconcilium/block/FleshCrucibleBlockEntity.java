package dev.movingcastle.thaumicconcilium.block;

import com.leclowndu93150.thaumaturge.registry.TTSounds;
import dev.movingcastle.thaumicconcilium.entity.mob.MadThaumaturge;
import dev.movingcastle.thaumicconcilium.registry.TCBlocks;
import dev.movingcastle.thaumicconcilium.world.CrimsonState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Flesh Crucible state (original: {@code FleshCrucibleTile}): up to 64 of one aspect (the original's FLESH, Thaumaturge's
 * victus; the pack has no flesh aspect). It never ticks: everything happens in {@code entityInside}. A creature standing in it
 * is hurt at most once per 10 ticks (the original's invulnerability frames); a player sips every tick.
 */
public class FleshCrucibleBlockEntity extends BlockEntity {
    public static final int MAX = 64;
    private static final int COOLDOWN = 10;

    private int amount;
    /** Game time of each creature's last handling (not saved: a pause of at most 10 ticks). */
    private final Map<UUID, Long> last = new HashMap<>();

    public FleshCrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(TCBlocks.FLESH_CRUCIBLE_BLOCK_ENTITY.get(), pos, state);
    }

    public int amount() {
        return this.amount;
    }

    public void setAmount(int value) {
        int old = this.amount;
        this.amount = Math.max(0, Math.min(MAX, value));
        if (old != this.amount) {
            this.setChanged();
            if (this.level != null) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
                this.level.updateNeighbourForOutputSignal(this.worldPosition, this.getBlockState().getBlock());
            }
        }
    }

    /** Server side, called from the block's entityInside. */
    public void touch(Entity entity) {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        if (entity instanceof MadThaumaturge mad) {
            this.attemptSmelt(mad);
            return;
        }
        if (entity instanceof Player player) {
            // Sipping is every tick (the original fired on every collision tick); only damage keeps the cooldown.
            if (CrimsonState.get(player).fleshAmount() > 0 && this.amount >= 1) {
                this.attemptSip(player);
            }
            return;
        }
        long now = this.level.getGameTime();
        Long before = this.last.get(entity.getUUID());
        if (before != null && now - before < COOLDOWN) {
            return;
        }
        if (entity instanceof LivingEntity) {
            this.mark(entity, now);
            entity.hurt(this.level.damageSources().inFire(), 1.0F);
            this.level.playSound(null, this.worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F,
                    2.0F + this.level.random.nextFloat() * 0.4F);
        }
    }

    private void mark(Entity entity, long now) {
        if (this.last.size() > 16) {
            this.last.values().removeIf(t -> now - t >= COOLDOWN);
        }
        this.last.put(entity.getUUID(), now);
    }

    /** Original {@code attemptSip}: one aspect and one flesh for one gore sound. */
    public void attemptSip(Player player) {
        this.setAmount(this.amount - 1);
        CrimsonState.addFlesh(player, -1);
        this.level.playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.GORE.get(), SoundSource.BLOCKS, 0.3F, 1.0F);
    }

    /**
     * Original {@code attemptSmelt}: the Mad Thaumaturge is consumed for 2 aspect (capped at 64) and the block stays a
     * Flesh Crucible, never reverting. The original's {@code func_147452_c} is {@code World.addBlockEvent}, not setBlock, and
     * nothing handles the event (confirmed in the 1.7.10 game, docs/design/5b-crucible-original-check.md; user decision
     * 2026-11-22).
     */
    public void attemptSmelt(MadThaumaturge mad) {
        this.level.playSound(null, mad.getX(), mad.getY(), mad.getZ(), TTSounds.BUBBLE.get(), SoundSource.BLOCKS, 0.2F,
                1.0F + this.level.random.nextFloat() * 0.4F);
        mad.discard();
        this.setAmount(this.amount + 2);
    }

    // ---------------------------------------------------------------- saving, syncing
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("amount", this.amount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.amount = Math.min(MAX, tag.getInt("amount"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
