package dev.movingcastle.thaumicconcilium.entity.mob;

import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A body lying on its back (original: the corpse mobs, {@code EntityLiving}s with boat physics). It floats on water and
 * lava like a boat, drifts, turns towards where it drifts, and does something when hit or touched instead of taking damage.
 */
public abstract class Corpse extends Mob {
    protected Corpse(EntityType<? extends Corpse> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 0.5).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void travel(Vec3 input) {
        if (!this.isEffectiveAi()) {
            return;
        }
        // Original boat physics: how much of the body sits in water or lava, in fifths.
        AABB box = this.getBoundingBox();
        double submerged = 0.0;
        for (int i = 0; i < 5; i++) {
            double y0 = box.minY + (box.maxY - box.minY) * i / 5.0 - 0.125;
            double y1 = box.minY + (box.maxY - box.minY) * (i + 1) / 5.0 - 0.125;
            AABB slice = new AABB(box.minX, y0, box.minZ, box.maxX, y1, box.maxZ);
            if (this.level().getBlockStates(slice).anyMatch(s -> s.getFluidState().is(FluidTags.WATER) || s.getFluidState().is(FluidTags.LAVA))) {
                submerged += 0.2;
            }
        }
        Vec3 v = this.getDeltaMovement();
        double vy = v.y;
        if (submerged < 1.0) {
            vy += 0.04 * (submerged * 2.0 - 1.0);
        } else {
            if (vy < 0.0) {
                vy /= 2.0;
            }
            vy += 0.007;
        }
        double vx = v.x;
        double vz = v.z;
        double speed = Math.sqrt(vx * vx + vz * vz);
        if (speed > 0.45) {
            vx *= 0.45 / speed;
            vz *= 0.45 / speed;
        }
        if (this.onGround()) {
            vx *= 0.5;
            vy *= 0.5;
            vz *= 0.5;
        }
        double oldX = this.getX();
        double oldZ = this.getZ();
        this.move(MoverType.SELF, new Vec3(vx, vy, vz));
        this.setDeltaMovement(vx * 0.99, vy * 0.95, vz * 0.99);
        double dx = this.getX() - oldX;
        double dz = this.getZ() - oldZ;
        if (dx * dx + dz * dz > 0.001) {
            float target = (float) (Mth.atan2(dz, dx) * 180.0 / Math.PI);
            float turn = Mth.clamp(Mth.wrapDegrees(target - this.getYRot()), -20.0F, 20.0F);
            this.setYRot(this.getYRot() + turn);
            this.yBodyRot = this.getYRot();
        }
        for (Boat boat : this.level().getEntitiesOfClass(Boat.class, this.getBoundingBox().inflate(0.2, 0.0, 0.2))) {
            boat.push(this);
        }
    }

    /** Hit or touched: does its thing. */
    protected abstract void disturb(net.minecraft.world.entity.player.Player player);

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide() || this.isRemoved()) {
            return false;
        }
        if (this.fireImmune() && source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            return false;
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            this.discard();
            return true;
        }
        this.disturb(source.getDirectEntity() instanceof net.minecraft.world.entity.player.Player p ? p : null);
        return true;
    }

    @Override
    protected net.minecraft.world.InteractionResult mobInteract(net.minecraft.world.entity.player.Player player,
                                                                net.minecraft.world.InteractionHand hand) {
        if (!this.level().isClientSide()) {
            this.disturb(player);
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(this.level().isClientSide());
    }
}
