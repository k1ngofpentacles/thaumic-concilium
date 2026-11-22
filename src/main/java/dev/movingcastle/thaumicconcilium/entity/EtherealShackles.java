package dev.movingcastle.thaumicconcilium.entity;

import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import dev.movingcastle.thaumicconcilium.registry.TCSounds;
import dev.movingcastle.thaumicconcilium.world.Chained;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The Paladin's Ethereal Shackles (original: EtherealShacklesEntity): a gravity-free bolt that vanishes after 100 ticks or on any
 * impact. A player it hits is chained for 100 ticks (see {@link Chained}); any other mob is slowed (V) for one second.
 */
public class EtherealShackles extends ThrowableProjectile {
    public static final int CHAIN_TICKS = 100;

    public EtherealShackles(EntityType<? extends EtherealShackles> type, Level level) {
        super(type, level);
    }

    public EtherealShackles(Level level, LivingEntity owner) {
        super(TCEntities.ETHEREAL_SHACKLES.get(), owner, level);
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
        if (!level().isClientSide && tickCount > 100) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (level().isClientSide) {
            return;
        }
        if (hit.getEntity() instanceof Player player) {
            Chained.chain(player, CHAIN_TICKS);
            level().playSound(null, player.getX(), player.getY(), player.getZ(), TCSounds.SHACKLES.get(), SoundSource.HOSTILE, 0.9F, 0.9F);
        } else if (hit.getEntity() instanceof Mob mob) {
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 4), getOwner());
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            discard();
        }
    }
}
