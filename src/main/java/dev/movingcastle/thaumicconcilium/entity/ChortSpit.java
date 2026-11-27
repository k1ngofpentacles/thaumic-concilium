package dev.movingcastle.thaumicconcilium.entity;

import dev.movingcastle.thaumicconcilium.compat.witchery.NetherPair;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The Chort's spit (original: {@code ChortSpitEntity}, a throwable drawn as a slime ball): gravity 0.01, deals its thrower's
 * attack damage as a mob hit, squelches and vanishes on any impact. Exists only with Witchery (registered in {@link NetherPair}).
 */
public class ChortSpit extends ThrowableItemProjectile {
    public ChortSpit(EntityType<? extends ChortSpit> type, Level level) {
        super(type, level);
    }

    public ChortSpit(Level level, LivingEntity thrower) {
        super(NetherPair.CHORT_SPIT.get(), thrower, level);
    }

    /** Aimed at {@code target} from the thrower's eyes, the aim turned {@code yawOffsetDeg} degrees about the vertical. */
    public static ChortSpit aimed(Level level, LivingEntity thrower, LivingEntity target, float yawOffsetDeg, float velocity, float inaccuracy) {
        ChortSpit spit = new ChortSpit(level, thrower);
        double dx = target.getX() - thrower.getX();
        double dy = target.getY() + target.getEyeHeight() - (thrower.getY() + thrower.getEyeHeight());
        double dz = target.getZ() - thrower.getZ();
        double yaw = Math.toRadians(yawOffsetDeg);
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        spit.shoot(dx * cos - dz * sin, dy, dx * sin + dz * cos, velocity, inaccuracy);
        return spit;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SLIME_BALL;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.01;
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (this.getOwner() instanceof LivingEntity thrower && !this.level().isClientSide
                && hit.getEntity() != thrower && hit.getEntity() != thrower.getVehicle()) {
            hit.getEntity().hurt(this.level().damageSources().mobAttack(thrower), (float) thrower.getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.HOSTILE, 0.5F, 1.0F);
        if (!this.level().isClientSide) {
            this.discard();
        }
    }
}
