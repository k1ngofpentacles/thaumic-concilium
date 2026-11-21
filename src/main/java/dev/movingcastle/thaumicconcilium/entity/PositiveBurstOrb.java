package dev.movingcastle.thaumicconcilium.entity;

import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A mote of life energy from the Positive Burst spell. Ported from {@code PositiveBurstOrbEntity}:
 * falls gently (0.03/tick), bounces, fizzes out of lava, and every 5 ticks looks for the nearest player within
 * 8 blocks who is not the caster, then homes in like an experience orb. Absorbed on touch.
 */
public class PositiveBurstOrb extends Entity {
    public static final int MODE_HEAL = 0;
    public static final int MODE_FULFILLMENT = 1;
    public static final int MODE_VITAMINIZE = 2;

    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(PositiveBurstOrb.class, EntityDataSerializers.INT);
    private static final double SEEK_RANGE = 8.0;

    private int orbAge;
    private int health = 5;
    private int potency;
    @Nullable
    private UUID casterId;
    @Nullable
    private Player target;

    public PositiveBurstOrb(EntityType<? extends PositiveBurstOrb> type, Level level) {
        super(type, level);
    }

    public static void spawn(ServerLevel level, @Nullable LivingEntity caster, Vec3 at, Vec3 velocity, int mode, int potency) {
        PositiveBurstOrb orb = new PositiveBurstOrb(TCEntities.POSITIVE_BURST_ORB.get(), level);
        orb.setPos(at);
        orb.setDeltaMovement(velocity);
        orb.setYRot(level.random.nextFloat() * 360.0F);
        orb.entityData.set(MODE, mode);
        orb.potency = potency;
        orb.casterId = caster == null ? null : caster.getUUID();
        level.addFreshEntity(orb);
    }

    public int mode() {
        return this.entityData.get(MODE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MODE, MODE_HEAL);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.03;
    }

    @Override
    public void tick() {
        super.tick();
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        this.applyGravity();

        if (this.level().getFluidState(this.blockPosition()).is(FluidTags.LAVA)) {
            this.setDeltaMovement((this.random.nextFloat() - this.random.nextFloat()) * 0.2F, 0.2F,
                    (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
            this.playSound(SoundEvents.GENERIC_BURN, 0.4F, 2.0F + this.random.nextFloat() * 0.4F);
        }

        if (!this.level().noCollision(this.getBoundingBox())) {
            this.moveTowardsClosestSpace(this.getX(), (this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0, this.getZ());
        }

        if (this.tickCount % 5 == 0 && (this.target == null || !this.target.isAlive())) {
            this.target = null;
            double best = Double.MAX_VALUE;
            for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(SEEK_RANGE))) {
                if (player.getUUID().equals(this.casterId) || player.isSpectator()) {
                    continue;
                }
                double d = player.distanceToSqr(this);
                if (d < best) {
                    best = d;
                    this.target = player;
                }
            }
        }

        if (this.target != null) {
            Vec3 to = new Vec3(this.target.getX() - this.getX(),
                    this.target.getEyeY() - this.getY(),
                    this.target.getZ() - this.getZ()).scale(1.0 / SEEK_RANGE);
            double dist = to.length();
            double pull = 1.0 - dist;
            if (pull > 0.0 && dist > 1.0E-4) {
                pull *= pull;
                this.setDeltaMovement(this.getDeltaMovement().add(to.scale(1.0 / dist * pull * 0.1)));
            }
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        float friction = 0.98F;
        if (this.onGround()) {
            friction = this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getFriction(this.level(), this.getBlockPosBelowThatAffectsMyMovement(), this) * 0.98F;
        }
        Vec3 m = this.getDeltaMovement();
        this.setDeltaMovement(m.x * friction, m.y * 0.98, m.z * friction);
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, -0.9, 1.0));
        }

        if (++this.orbAge >= TCConfig.POSITIVE_ORB_LIFETIME.get()) {
            this.discard();
        }
    }

    @Override
    public void playerTouch(Player player) {
        if (this.level().isClientSide || player.takeXpDelay != 0 || player.isInvulnerable()
                || player.getUUID().equals(this.casterId)) {
            return;
        }
        switch (this.mode()) {
            case MODE_FULFILLMENT -> player.setAbsorptionAmount(Math.min(20.0F, player.getAbsorptionAmount() + this.potency));
            case MODE_VITAMINIZE -> {
                int duration = 100 + this.potency * 40;
                var effect = switch (this.random.nextInt(3)) {
                    case 0 -> MobEffects.MOVEMENT_SPEED;
                    case 1 -> MobEffects.DAMAGE_RESISTANCE;
                    default -> MobEffects.DAMAGE_BOOST;
                };
                player.addEffect(new MobEffectInstance(effect, duration, this.potency));
            }
            default -> player.heal(0.5F);
        }
        player.takeXpDelay = 2;
        this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.1F,
                0.5F * ((this.random.nextFloat() - this.random.nextFloat()) * 0.7F + 1.8F));
        this.discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        this.markHurt();
        this.health -= (int) amount;
        if (this.health <= 0) {
            this.discard();
        }
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 0.5 * 64.0 * getViewScale();
        return distance < range * range;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.health = tag.getShort("Health");
        this.orbAge = tag.getShort("Age");
        this.potency = tag.getInt("Power");
        this.entityData.set(MODE, tag.getInt("Mode"));
        this.casterId = tag.hasUUID("Caster") ? tag.getUUID("Caster") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putShort("Health", (short) this.health);
        tag.putShort("Age", (short) this.orbAge);
        tag.putInt("Power", this.potency);
        tag.putInt("Mode", this.mode());
        if (this.casterId != null) {
            tag.putUUID("Caster", this.casterId);
        }
    }
}
