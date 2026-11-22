package dev.movingcastle.thaumicconcilium.entity;

import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * One ember of the Brightest One's ignis burst (original: Thaumcraft's {@code EntityEmber} with damage 25 and no fire).
 * A small bolt of flame that flies straight for 30 ticks, hurts the first creature it meets for its damage and sets
 * nothing alight and breaks nothing.
 */
public class BossEmber extends ThrowableProjectile {
    public static final int LIFETIME = 30;
    private float damage = 25.0F;

    public BossEmber(EntityType<? extends BossEmber> type, Level level) {
        super(type, level);
    }

    public static BossEmber create(ServerLevel level, LivingEntity shooter, double x, double y, double z, double mx, double my, double mz, float damage) {
        BossEmber ember = new BossEmber(TCEntities.BOSS_EMBER.get(), level);
        ember.setOwner(shooter);
        ember.setPos(x, y, z);
        ember.setDeltaMovement(mx, my, mz);
        ember.damage = damage;
        return ember;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
        } else if (this.tickCount > LIFETIME) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (result.getEntity() instanceof LivingEntity living && !(living instanceof dev.movingcastle.thaumicconcilium.entity.mob.BrightestOne)
                && result.getEntity() != this.getOwner()) {
            living.hurt(this.damageSources().indirectMagic(this, this.getOwner()), this.damage);
            this.discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (result.getType() == HitResult.Type.BLOCK && !this.level().isClientSide()) {
            this.discard();
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
