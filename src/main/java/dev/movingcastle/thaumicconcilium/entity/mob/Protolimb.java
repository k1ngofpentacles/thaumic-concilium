package dev.movingcastle.thaumicconcilium.entity.mob;

import dev.movingcastle.thaumicconcilium.world.Protolimbs;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A piece of a shattered Protobody (original {@code Protolimb}, a 1.7.10 {@code EntityFlying}): 6 types (0 head, 1 left arm,
 * 2 right arm, 3 body, 4 left leg, 5 right leg), 1 health, fire immune, no experience, no fall damage. It drifts: a target
 * 8/4/8 blocks around it, re-picked one tick in a hundred or on arrival, speed eased toward 0.1 a tick, and when a player is
 * within 4 blocks (horizontally) the target is pushed 4 further away from them. Touching a player is absorbing it
 * ({@link Protolimbs#absorb}). Not an {@code Enemy} (the 1.7.10 flying mob was not removed on peaceful), never despawns.
 * <p>Movement runs on the server only; the nearest player is looked up once a second, not every tick (the original did it once
 * and never let go). Deviations are listed in docs/agents/retro/phase8-4-notes.md.
 */
public class Protolimb extends FlyingMob {
    private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(Protolimb.class, EntityDataSerializers.INT);
    /** How often the nearest player is looked up, in ticks. */
    private static final int LOOKUP_EVERY = 20;

    private double targetX;
    private double targetY;
    private double targetZ;
    private boolean targeted;
    @Nullable
    private Player nearest;
    private int lookupIn;
    /** Client spin seed (original: random 0..100000), counted up every tick. */
    public int innerRotation;

    public Protolimb(EntityType<? extends Protolimb> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.innerRotation = this.random.nextInt(100000);
        this.setNoGravity(true);
    }

    public Protolimb(Level level, int type) {
        this(dev.movingcastle.thaumicconcilium.registry.TCEntities.PROTOLIMB.get(), level);
        setLimbType(type);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return FlyingMob.createMobAttributes().add(Attributes.MAX_HEALTH, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TYPE, 0);
    }

    public int limbType() {
        return this.entityData.get(TYPE);
    }

    public void setLimbType(int type) {
        this.entityData.set(TYPE, Mth.clamp(type, 0, 5));
    }

    @Override
    public void tick() {
        super.tick();
        this.innerRotation++;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!targeted) {
            pickTarget();
            targeted = true;
        }
        if (--lookupIn <= 0) {
            lookupIn = LOOKUP_EVERY;
            nearest = level().getNearestPlayer(this, 16.0);
        }
        if (nearest != null) {
            double dx = getX() - nearest.getX();
            double dz = getZ() - nearest.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 4.0 && dist > 1.0E-4) {
                targetX += dx / dist * 4.0;
                targetZ += dz / dist * 4.0;
                targetY += (getY() - nearest.getY()) * 0.5;
            }
        }
        double dx = targetX - getX();
        double dy = targetY - getY();
        double dz = targetZ - getZ();
        Vec3 v = getDeltaMovement();
        setDeltaMovement(v.x + (Math.signum(dx) * 0.1 - v.x) * 0.1,
                v.y + (Math.signum(dy) * 0.1 - v.y) * 0.1,
                v.z + (Math.signum(dz) * 0.1 - v.z) * 0.1);
        if (random.nextInt(100) == 0 || (Math.abs(dx) < 1.0 && Math.abs(dy) < 1.0 && Math.abs(dz) < 1.0)) {
            pickTarget();
        }
    }

    private void pickTarget() {
        targetX = getX() + (random.nextDouble() - 0.5) * 16.0;
        targetY = getY() + (random.nextDouble() - 0.5) * 8.0;
        targetZ = getZ() + (random.nextDouble() - 0.5) * 16.0;
    }

    /** Flies on its own motion: no friction, no gravity, no steering input. */
    @Override
    public void travel(Vec3 input) {
        if (isControlledByLocalInstance()) {
            move(MoverType.SELF, getDeltaMovement());
        }
        calculateEntityAnimation(false);
    }

    /** Server: absorbed by the player. Client: a small burst (original: Thaumcraft.proxy.burst, 2.0). */
    @Override
    public void playerTouch(Player player) {
        if (level().isClientSide) {
            for (int i = 0; i < 6; i++) {
                level().addParticle(ParticleTypes.REVERSE_PORTAL, getX() + (random.nextDouble() - 0.5) * 0.6, getY() + random.nextDouble() * 0.5,
                        getZ() + (random.nextDouble() - 0.5) * 0.6, (random.nextDouble() - 0.5) * 0.2, random.nextDouble() * 0.2, (random.nextDouble() - 0.5) * 0.2);
            }
        } else if (player instanceof ServerPlayer server && !isRemoved()) {
            Protolimbs.absorb(server);
            discard();
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Type", (byte) limbType());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setLimbType(tag.getByte("Type"));
    }
}
