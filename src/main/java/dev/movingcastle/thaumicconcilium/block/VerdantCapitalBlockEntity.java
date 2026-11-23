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
    /** Game time at which the drive last became STARVED: the client reads how long the capital has gone hungry from it (its anger). */
    private long starvedSince;
    /** The starve grace as the server has it, sent with every update (the client's own config may differ in multiplayer). */
    private int starveGrace = -1;

    // ---- client-only animation state (cycle 4b): written by the renderer as it draws, never saved or synced; the block entity still never ticks
    /** The upper half's angle in degrees (forward = clockwise from above; the lower half mirrors it) and its speed in degrees a tick. */
    public float clientAngle;
    public float clientOmega;
    /** How far every small tetrahedron stands out from its closed place, in blocks. */
    public float clientSep;
    /** How dark the sphere is: 0 with mana, 1 leaking. */
    public float clientDark;
    /** The ray's alpha (0 to 1) and how far it has turned to the unwinding colours (0 to 1). */
    public float clientRay;
    public float clientUnwind;
    /** The rest of the client's animation state (client.CapitalMotion: the breath, the crossing, the pose). */
    @Nullable
    public Object clientMotion;
    /** Game time (with the partial tick) of the last frame drawn; NaN before the first. */
    public double clientTime = Double.NaN;
    /** Set by the client: told when a capital on the client loads, changes or is removed (the crackles keep their list from it). */
    @Nullable
    public static java.util.function.Consumer<VerdantCapitalBlockEntity> clientState;

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
            if (drive == Drive.STARVED && this.level != null) {
                this.starvedSince = this.level.getGameTime();
            }
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

    /** Game time at which the capital last began to starve (meaningful while the drive is STARVED). */
    public long starvedSince() {
        return this.starvedSince;
    }

    /** Ticks a capital may starve before it unwinds: the server's value where it was sent, else this side's config. */
    public int starveGrace() {
        return this.starveGrace >= 0 ? this.starveGrace : TCConfig.RITE_STARVE_GRACE.get();
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
        tag.putLong("starvedSince", this.starvedSince);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.mana = Math.max(0, Math.min(tag.getInt("mana"), capacity()));
        this.placer = tag.hasUUID("placer") ? tag.getUUID("placer") : null;
        this.drive = Drive.of(tag.getByte("drive"));
        this.lastFeedSide = tag.contains("feedSide") ? Direction.from3DDataValue(tag.getByte("feedSide")) : null;
        this.lastFeedGameTime = tag.contains("feedTime") ? tag.getLong("feedTime") : Long.MIN_VALUE / 2;
        this.starvedSince = tag.getLong("starvedSince");
        this.starveGrace = tag.contains("starveGrace") ? tag.getInt("starveGrace") : -1;
        this.tellClient();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        this.tellClient();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        this.tellClient();
    }

    private void tellClient() {
        if (clientState != null && this.level != null && this.level.isClientSide) {
            clientState.accept(this);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = this.saveWithoutMetadata(registries);
        tag.putInt("starveGrace", TCConfig.RITE_STARVE_GRACE.get());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
