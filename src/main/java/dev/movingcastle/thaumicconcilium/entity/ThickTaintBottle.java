package dev.movingcastle.thaumicconcilium.entity;

import com.leclowndu93150.thaumaturge.api.entity.trait.MobTraits;
import com.leclowndu93150.thaumaturge.api.taint.TaintApi;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import dev.movingcastle.thaumicconcilium.registry.TCItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Thrown Bottle of Thick Taint. The original gave Taint Poison to every living, non-tainted, non-undead creature
 * within 5 blocks, then, in up to ten spots around the impact, turned the biome to taint, laid taint fibres on the
 * ground and a pocket of flux gas above. Thaumaturge has no taint biome; its equivalent is ecological pressure, so
 * each spot gains pressure (as much as a flux lightning strike leaves) and an attempt at fibre growth instead.
 */
public class ThickTaintBottle extends ThrowableItemProjectile {
    private static final float PRESSURE = 0.04F;

    public ThickTaintBottle(EntityType<? extends ThickTaintBottle> type, Level level) {
        super(type, level);
    }

    public ThickTaintBottle(Level level, LivingEntity thrower) {
        super(TCEntities.THICK_TAINT_BOTTLE.get(), thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return TCItems.BOTTLE_OF_THICK_TAINT.get();
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
        Vec3 at = result.getLocation();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(5.0),
                e -> !MobTraits.isTainted(e) && !e.getType().is(EntityTypeTags.UNDEAD))) {
            e.addEffect(new MobEffectInstance(TTMobEffects.FLUX_TAINT, 100, 0, false, true));
        }
        BlockPos origin = BlockPos.containing(at);
        for (int i = 0; i < 10; i++) {
            if (!this.random.nextBoolean()) {
                continue;
            }
            int dx = (int) ((this.random.nextFloat() - this.random.nextFloat()) * 5.0F);
            int dz = (int) ((this.random.nextFloat() - this.random.nextFloat()) * 5.0F);
            BlockPos pos = origin.offset(dx, 0, dz);
            TaintApi.addEcologicalPressure(level, pos, PRESSURE);
            BlockPos below = pos.below();
            if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) && level.getBlockState(pos).canBeReplaced()) {
                TaintApi.spreadFibres(level, pos, true);
            }
            if (level.getBlockState(pos.above()).isAir()) {
                level.setBlock(pos.above(), TTBlocks.FLUX_GAS.get().defaultBlockState(), 3);
            }
        }
        BottleSplash.burst(level, at, this.getItem(), 0x7A2E9E, net.minecraft.core.particles.ParticleTypes.WITCH, 30);
        this.discard();
    }
}
