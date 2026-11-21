package dev.movingcastle.thaumicconcilium.entity;

import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import dev.movingcastle.thaumicconcilium.registry.TCItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Thrown Bottle of Clear Water: puts out every burning entity within 5 blocks, scalds blazes and magma cubes for 10,
 * and quenches fire in a 6 x 6 patch at and above the impact, as in the original.
 */
public class ClearWaterBottle extends ThrowableItemProjectile {
    public ClearWaterBottle(EntityType<? extends ClearWaterBottle> type, Level level) {
        super(type, level);
    }

    public ClearWaterBottle(Level level, LivingEntity thrower) {
        super(TCEntities.CLEAR_WATER_BOTTLE.get(), thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return TCItems.BOTTLE_OF_CLEAR_WATER.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        for (Entity e : level.getEntities(this, this.getBoundingBox().inflate(5.0))) {
            e.clearFire();
            if (e instanceof Blaze || e instanceof MagmaCube) {
                e.hurt(this.damageSources().indirectMagic(this, this.getOwner()), 10.0F);
            }
        }
        BlockPos centre = result instanceof BlockHitResult block ? block.getBlockPos() : BlockPos.containing(result.getLocation());
        for (int x = -3; x < 3; x++) {
            for (int z = -3; z < 3; z++) {
                for (int y = 0; y <= 1; y++) {
                    BlockPos pos = centre.offset(x, y, z);
                    if (level.getBlockState(pos).is(BlockTags.FIRE)) {
                        level.removeBlock(pos, false);
                    }
                }
            }
        }
        BottleSplash.burst(level, result.getLocation(), this.getItem(), 0x385DC6, net.minecraft.core.particles.ParticleTypes.SPLASH, 60);
        this.discard();
    }
}
