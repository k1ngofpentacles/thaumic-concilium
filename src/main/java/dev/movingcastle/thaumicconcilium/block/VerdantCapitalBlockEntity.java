package dev.movingcastle.thaumicconcilium.block;

import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.registry.TCBlocks;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The state of a Verdant Capital (phase 5d): a mana buffer, the player who placed it, and the display state the client animates from
 * ({@link Drive}, the side the last mana arrived from, and when). It never ticks: the Vessel it serves reads and spends its mana, and a
 * capital with no Vessel does nothing. The Botania receiver and spark socket live in {@code compat.BotaniaCapital}, not here, so this
 * class loads without Botania. The client is told only when the drive changes (or a feed arrives from a new side, or after a pause in feeding).
 */
public class VerdantCapitalBlockEntity extends BlockEntity {
    /** What the capital is doing for its Vessel: nothing, advancing a unit on mana, paused without mana, or unwinding its progress. */
    public enum Drive {
        IDLE, FED, STARVED, UNWINDING;

        public static Drive of(int ordinal) {
            Drive[] all = values();
            return ordinal >= 0 && ordinal < all.length ? all[ordinal] : IDLE;
        }
    }

    /** Feeds closer together than this, from the same side, are not worth another client update. */
    private static final long FEED_SYNC_TICKS = 20L;

    private int mana;
    @Nullable
    private UUID placer;
    private Drive drive = Drive.IDLE;
    @Nullable
    private Direction lastFeedSide;
    private long lastFeedGameTime = Long.MIN_VALUE / 2;
    private long lastFeedSynced = Long.MIN_VALUE / 2;

    public VerdantCapitalBlockEntity(BlockPos pos, BlockState state) {
        super(TCBlocks.VERDANT_CAPITAL_BLOCK_ENTITY.get(), pos, state);
    }

    public static int capacity() {
        return TCConfig.RITE_CAPITAL_BUFFER.get();
    }

    public int mana() {
        return this.mana;
    }

    public boolean isFull() {
        return this.mana >= capacity();
    }

    /** Adds mana up to the buffer; returns how much was taken. {@code side} is where it came from (null when unknown). */
    public int addMana(int amount, @Nullable Direction side) {
        int taken = Math.max(0, Math.min(amount, capacity() - this.mana));
        if (taken > 0) {
            this.mana += taken;
            this.setChanged();
            if (this.level != null && !this.level.isClientSide) {
                long now = this.level.getGameTime();
                boolean newSide = side != this.lastFeedSide;
                this.lastFeedSide = side;
                this.lastFeedGameTime = now;
                if (newSide || now - this.lastFeedSynced >= FEED_SYNC_TICKS) {
                    this.lastFeedSynced = now;
                    this.sync();
                }
            }
        }
        return taken;
    }

    /** Spends mana (the Vessel's drain). Returns whether it had that much. */
    public boolean consume(int amount) {
        if (this.mana < amount) {
            return false;
        }
        this.mana -= amount;
        this.setChanged();
        return true;
    }

    /** Test hook: sets the buffer directly. */
    public void setMana(int mana) {
        this.mana = Math.max(0, Math.min(mana, capacity()));
        this.setChanged();
    }

    @Nullable
    public UUID placer() {
        return this.placer;
    }

    public void setPlacer(@Nullable UUID placer) {
        this.placer = placer;
        this.setChanged();
    }

    public Drive drive() {
        return this.drive;
    }

    /** Called by the Vessel; the client is told only when the state actually changes. */
    public void setDrive(Drive drive) {
        if (drive != this.drive) {
            this.drive = drive;
            this.setChanged();
            this.sync();
        }
    }

    @Nullable
    public Direction lastFeedSide() {
        return this.lastFeedSide;
    }

    public long lastFeedGameTime() {
        return this.lastFeedGameTime;
    }

    private void sync() {
        Level level = this.level;
        if (level != null && !level.isClientSide) {
            BlockState state = this.getBlockState();
            level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("mana", this.mana);
        if (this.placer != null) {
            tag.putUUID("placer", this.placer);
        }
        tag.putByte("drive", (byte) this.drive.ordinal());
        if (this.lastFeedSide != null) {
            tag.putByte("feedSide", (byte) this.lastFeedSide.get3DDataValue());
        }
        tag.putLong("feedTime", this.lastFeedGameTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.mana = Math.max(0, Math.min(tag.getInt("mana"), capacity()));
        this.placer = tag.hasUUID("placer") ? tag.getUUID("placer") : null;
        this.drive = Drive.of(tag.getByte("drive"));
        this.lastFeedSide = tag.contains("feedSide") ? Direction.from3DDataValue(tag.getByte("feedSide")) : null;
        this.lastFeedGameTime = tag.contains("feedTime") ? tag.getLong("feedTime") : Long.MIN_VALUE / 2;
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
