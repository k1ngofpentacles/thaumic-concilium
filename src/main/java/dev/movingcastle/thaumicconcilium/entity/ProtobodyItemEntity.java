package dev.movingcastle.thaumicconcilium.entity;

import dev.movingcastle.thaumicconcilium.entity.mob.Protolimb;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import dev.movingcastle.thaumicconcilium.world.Protolimbs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A dropped Protobody (original {@code EntityItemProtobody}). An ordinary item entity until it falls below the void line
 * ({@link Protolimbs#itemThreshold}: 16 blocks under the dimension's floor), where it breaks into six Protolimbs, types 0 to 5, each
 * thrown a little outward, and is gone. The check is one comparison per tick of this one item entity; nothing else ticks.
 * Original bug fixed: the 1.7.10 code set the motion of the item (about to die) instead of each limb's.
 */
public class ProtobodyItemEntity extends ItemEntity {
    public ProtobodyItemEntity(EntityType<? extends ItemEntity> type, Level level) {
        super(type, level);
    }

    public ProtobodyItemEntity(Level level, double x, double y, double z, ItemStack stack) {
        super(TCEntities.PROTOBODY_ITEM.get(), level);
        setPos(x, y, z);
        setItem(stack);
        this.lifespan = stack.getEntityLifespan(level);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && !isRemoved() && Protolimbs.belowItemThreshold(this)) {
            shatter();
        }
    }

    /** Six limbs, one of each type, at the item, each pushed toward a random point 1 up and about 2 out at speed 0.5 (the original's intent). */
    public void shatter() {
        for (int i = 0; i < 6; i++) {
            Protolimb limb = new Protolimb(level(), i);
            limb.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
            Vec3 out = new Vec3((-0.5 + random.nextGaussian()) * 2.0, 1.0, (-0.5 + random.nextGaussian()) * 2.0).normalize().scale(0.5);
            limb.setDeltaMovement(out);
            level().addFreshEntity(limb);
        }
        discard();
    }
}
