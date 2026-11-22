package dev.movingcastle.thaumicconcilium.entity;

import com.leclowndu93150.thaumaturge.api.damagesource.TTDamageSources;
import com.leclowndu93150.thaumaturge.content.taint.flux.PhysicalFlux;
import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import dev.movingcastle.thaumicconcilium.TCConfig;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A clot the Vessel throws at someone drawing from an anchor (phase 5c, cycle 2). It winds up at the disc's edge, then flies straight
 * at the target's position at that moment. A player's attack (melee or projectile) bats it back at the hole: arriving there staggers
 * the Vessel. A hit costs 4 taint damage and breaks the tether; a miss (block or timeout) leaves flux goo; both count as missed clots on the Vessel.
 * Not saved (a clot is part of one moment of the fight; the Vessel drops them when it sleeps). Synced for the renderer: the tick it
 * starts to fly ({@link #launchTick()}) and whether it has been batted.
 */
public class VesselClot extends Entity {
    private static final EntityDataAccessor<Integer> LAUNCH = SynchedEntityData.defineId(VesselClot.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> BATTED = SynchedEntityData.defineId(VesselClot.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<java.util.Optional<UUID>> TARGET = SynchedEntityData.defineId(VesselClot.class, EntityDataSerializers.OPTIONAL_UUID);
    /** Client hook (set in {@code TCClient}): a tick each client tick, for sounds and particles. */
    @Nullable
    public static java.util.function.Consumer<VesselClot> clientTick;
    /** Client-side look state. */
    public boolean fxBatted;
    public boolean fxLaunched;
    @Nullable
    private UUID vessel;
    @Nullable
    private UUID target;
    private int flown;

    public VesselClot(EntityType<? extends VesselClot> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** Sets up a clot at {@code at}: it flies at {@code launchTick} (a game time) at {@code target}. */
    public void setup(Vessel owner, @Nullable ServerPlayer target, Vec3 at, long launchTick) {
        this.vessel = owner.getUUID();
        this.target = target == null ? null : target.getUUID();
        this.entityData.set(TARGET, java.util.Optional.ofNullable(this.target));
        this.setPos(at);
        this.entityData.set(LAUNCH, (int) launchTick);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(LAUNCH, 0).define(BATTED, false).define(TARGET, java.util.Optional.empty());
    }

    /** Who it is aimed at (synced, for the wind-up's aim line). */
    public java.util.Optional<UUID> syncedTarget() {
        return this.entityData.get(TARGET);
    }

    /** The game time (as an int) at which it starts to fly; before that it is winding up. */
    public int launchTick() {
        return this.entityData.get(LAUNCH);
    }

    public boolean batted() {
        return this.entityData.get(BATTED);
    }

    @Nullable
    public UUID vesselId() {
        return this.vessel;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    /** The ghast rule: any attack by a player, melee or a projectile they fired, returns it at the hole. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || this.batted() || !(source.getEntity() instanceof ServerPlayer player)) {
            return false;
        }
        Vessel owner = this.owner();
        if (owner == null) {
            return false;
        }
        this.entityData.set(BATTED, true);
        this.setDeltaMovement(owner.holeCentre().subtract(this.position()).normalize().scale(TCConfig.CLOT_BAT_SPEED.get()));
        owner.addParticipant(player.getUUID());
        return true;
    }

    @Nullable
    private Vessel owner() {
        return this.vessel != null && this.level() instanceof ServerLevel level && level.getEntity(this.vessel) instanceof Vessel v && !v.isRemoved() ? v : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            if (clientTick != null) {
                clientTick.accept(this);
            }
            return;
        }
        if (this.qaStill) {
            if (this.tickCount == 1) {
                var near = level.getNearestPlayer(this, 64.0);
                this.entityData.set(TARGET, java.util.Optional.ofNullable(near == null ? null : near.getUUID()));
            }
            return;
        }
        Vessel owner = this.owner();
        if (owner == null || !owner.fightIsOn()) {
            this.discard();
            return;
        }
        if (this.batted()) {
            this.flyBack(owner);
            return;
        }
        long now = level.getGameTime();
        if (now < this.launchTick()) {
            return; // winding up at the disc edge
        }
        ServerPlayer who = this.target == null ? null : Vessel.playerById(level, this.target);
        if (this.flown == 0) {
            if (who == null || !who.isAlive() || who.level() != level) {
                this.discard();
                return;
            }
            Vec3 aim = who.position().add(0.0, who.getBbHeight() * 0.6, 0.0).subtract(this.position()).normalize();
            this.setDeltaMovement(aim.scale(TCConfig.CLOT_SPEED.get()));
        }
        this.flown++;
        Vec3 from = this.position();
        Vec3 to = from.add(this.getDeltaMovement());
        var clip = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (clip.getType() == HitResult.Type.BLOCK) {
            this.miss(level, owner, clip.getBlockPos().relative(clip.getDirection()));
            return;
        }
        this.setPos(to);
        for (ServerPlayer p : Vessel.players(level)) {
            if (!p.isAlive() || p.isSpectator() || !p.getBoundingBox().intersects(this.getBoundingBox().inflate(0.2))) {
                continue;
            }
            {
                ServerPlayer sp = p;
                sp.hurt(TTDamageSources.taint(level), (float) (double) TCConfig.CLOT_DAMAGE.get());
                sp.addEffect(new MobEffectInstance(TTMobEffects.FLUX_TAINT, TCConfig.CLOT_TAINT_TICKS.get()));
                owner.breakTether(sp.getUUID());
                owner.countMissedClot(); // user 2026-11-22: every unbatted clot scars the land, hits included
                this.discard();
                return;
            }
        }
        if (this.flown >= TCConfig.CLOT_LIFE.get()) {
            this.miss(level, owner, this.blockPosition());
        }
    }

    private void flyBack(Vessel owner) {
        Vec3 hole = owner.holeCentre();
        Vec3 delta = hole.subtract(this.position());
        double speed = TCConfig.CLOT_BAT_SPEED.get();
        if (delta.length() <= Math.max(TCConfig.HORIZON.get(), speed)) {
            owner.stagger();
            this.discard();
            return;
        }
        Vec3 step = delta.normalize().scale(speed);
        this.setDeltaMovement(step);
        this.setPos(this.position().add(step));
    }

    private void miss(ServerLevel level, Vessel owner, BlockPos at) {
        PhysicalFlux.placeGoo(level, at, 1);
        owner.countMissedClot();
        this.discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // QA only (the render harness): QaLaunchIn = ticks of wind-up, QaBatted, and the clot then stands still aimed at the nearest player
        if (tag.contains("QaLaunchIn")) {
            this.qaStill = true;
            this.entityData.set(LAUNCH, (int) (this.level().getGameTime() + tag.getInt("QaLaunchIn")));
            this.entityData.set(BATTED, tag.getBoolean("QaBatted"));
        }
    }

    private boolean qaStill;

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
