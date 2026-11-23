package dev.movingcastle.thaumicconcilium.entity;

import dev.movingcastle.thaumicconcilium.world.GroveSeedDeath;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * The grove's hold on a doomed Taint Seed (phase 5d cycle 4b): the roots and leaves the client draws round the seed while it
 * dies, and the clock for the moments of that death that need the exact tick (see {@link GroveSeedDeath} for the timeline). It exists
 * for those twelve seconds only, stands still, collides with nothing and cannot be hurt. Its age is game time since the synced start,
 * so a client that starts watching part of the way through sees the right moment.
 */
public class GroveBind extends Entity {
    private static final EntityDataAccessor<Long> START = SynchedEntityData.defineId(GroveBind.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(GroveBind.class, EntityDataSerializers.FLOAT);

    /** Set by the client: called every client tick with the bind and the ages it has just passed (after, up to). */
    @Nullable
    public static ClientTick clientTick;
    /** Lashes and leaf bursts fired on the server since the game started (the tests read them). */
    public static int lashesFired;
    public static int burstsFired;

    @FunctionalInterface
    public interface ClientTick {
        void tick(GroveBind bind, long after, long upTo);
    }

    @Nullable
    private UUID seedId;
    /** The last age handled; -1 before the first tick here (after a load, the moments missed meanwhile are not replayed). */
    private long lastAge = -1L;

    public GroveBind(EntityType<? extends GroveBind> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** Stands this bind round {@code seed}, its sequence starting at game time {@code start}. */
    public void bind(Entity seed, long start) {
        this.seedId = seed.getUUID();
        this.setPos(seed.getX(), seed.getY(), seed.getZ());
        this.entityData.set(START, start);
        this.entityData.set(SCALE, seed.getBbWidth() / 1.5F); // 1 for a Taint Seed, 1.33 for a prime one
        this.lastAge = 0L;
        if (this.level() instanceof ServerLevel level) {
            this.sound(level, SoundEvents.ROOTED_DIRT_BREAK, 1.4F, 0.6F);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(START, 0L);
        builder.define(SCALE, 1.0F);
    }

    public long start() {
        return this.entityData.get(START);
    }

    /** Moves the sequence's start (cycle 4f: the finale re-times roots rolled before sight's return was decided). */
    public void setStart(long start) {
        this.entityData.set(START, start);
    }

    /** The seed's width over a plain Taint Seed's: everything drawn is scaled by it. */
    public float scale() {
        return this.entityData.get(SCALE);
    }

    /** Ticks since the sequence began, with the partial tick. */
    public float age(float partialTick) {
        return (float) (this.level().getGameTime() - this.start()) + partialTick;
    }

    @Override
    public void tick() {
        super.tick();
        long age = this.level().getGameTime() - this.start();
        long from = this.lastAge < 0L ? age - 1L : this.lastAge;
        this.lastAge = age;
        if (this.level().isClientSide) {
            if (clientTick != null && age > from) {
                clientTick.tick(this, from, age);
            }
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        if (age - from <= 20L) { // the moments of the last tick (not a backlog after a long gap)
            for (int i = 0; i < GroveSeedDeath.LASHES.length; i++) {
                if (passed(from, age, GroveSeedDeath.LASHES[i])) {
                    this.lash(level, i);
                }
            }
            for (int i = 0; i < GroveSeedDeath.BURSTS; i++) {
                if (passed(from, age, GroveSeedDeath.BURST_START + i * GroveSeedDeath.BURST_EVERY)) {
                    burstsFired++;
                    this.sound(level, SoundEvents.AZALEA_LEAVES_BREAK, 1.2F, 0.8F + 0.08F * i);
                }
            }
        }
        if (age >= GroveSeedDeath.KILL) { // from the kill to the end (40 ticks), so a bind that comes back late still ends its seed
            Entity seed = this.seed(level);
            if (seed != null && seed.isAlive()) {
                seed.setInvulnerable(false);
                seed.kill();
            }
            if (from < GroveSeedDeath.KILL) {
                this.sound(level, SoundEvents.ROOTED_DIRT_BREAK, 1.4F, 0.5F);
            }
        }
        if (age >= GroveSeedDeath.END) {
            this.discard();
        }
    }

    private static boolean passed(long from, long upTo, int moment) {
        return from < moment && moment <= upTo;
    }

    @Nullable
    private Entity seed(ServerLevel level) {
        return this.seedId == null ? null : level.getEntity(this.seedId);
    }

    /** One lash: the seed's own strike animation on every client watching it, and a creak from the roots. */
    private void lash(ServerLevel level, int index) {
        lashesFired++;
        Entity seed = this.seed(level);
        if (seed != null && seed.isAlive()) {
            level.broadcastEntityEvent(seed, GroveSeedDeath.LASH_EVENT);
            SoundEvent tentacle = com.leclowndu93150.thaumaturge.registry.TTSounds.TENTACLE.get();
            level.playSound(null, seed.getX(), seed.getY(), seed.getZ(), tentacle, SoundSource.HOSTILE, 1.0F, 0.9F + 0.1F * index);
        }
        this.sound(level, SoundEvents.MANGROVE_ROOTS_BREAK, 1.0F, 0.6F + 0.05F * index);
    }

    private void sound(ServerLevel level, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, this.getX(), this.getY(), this.getZ(), sound, SoundSource.BLOCKS, volume, pitch);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(START, tag.getLong("start"));
        this.entityData.set(SCALE, tag.contains("scale") ? tag.getFloat("scale") : 1.0F);
        this.seedId = tag.hasUUID("seed") ? tag.getUUID("seed") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong("start", this.start());
        tag.putFloat("scale", this.scale());
        if (this.seedId != null) {
            tag.putUUID("seed", this.seedId);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}
