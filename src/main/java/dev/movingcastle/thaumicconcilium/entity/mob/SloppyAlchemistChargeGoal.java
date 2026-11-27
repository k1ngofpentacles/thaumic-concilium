package dev.movingcastle.thaumicconcilium.entity.mob;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * Original {@code SloppyAlchemistAttackAI}; runs only while a Chort rides him. Within {@code maxDistance} he backs away from the
 * target at {@code evadeSpeed}, strafing (the side changes every 30 ticks); after {@code chargeInterval} ticks of seeing it
 * he hops and stands still for 12 ticks, then rams at {@code ramSpeed} at a point 3 blocks beyond it, hitting once with
 * knockback 2.5 sideways and 0.65 up.
 */
public final class SloppyAlchemistChargeGoal extends Goal {
    public static final int WINDUP_TICKS = 12;
    public static final double KNOCKBACK_HORIZONTAL = 2.5;
    public static final double KNOCKBACK_VERTICAL = 0.65;
    public static final double RAM_OVERSHOOT = 3.0;
    private static final int STRAFE_TICKS = 30;
    private static final double STRAFE_FACTOR = 0.1;

    private final Mob attacker;
    private final double evadeSpeed;
    private final double ramSpeed;
    private final int chargeInterval;
    private final float maxDistance;
    private LivingEntity target;
    private int strafeTime;
    private int strafeDirection = 1;
    private int chargeTime;
    private boolean ramming;
    private boolean windingUp;
    private int windupTicks;
    private boolean ramHit;
    private double ramX;
    private double ramZ;

    public SloppyAlchemistChargeGoal(Mob mob, double evadeSpeed, double ramSpeed, int chargeInterval, float maxDistance) {
        this.attacker = mob;
        this.evadeSpeed = evadeSpeed;
        this.ramSpeed = ramSpeed;
        this.chargeInterval = chargeInterval;
        this.maxDistance = maxDistance;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!(this.attacker.getFirstPassenger() instanceof Chort)) {
            return false;
        }
        LivingEntity t = this.attacker.getTarget();
        if (t == null) {
            return false;
        }
        this.target = t;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null && this.target.isAlive() && this.attacker.getFirstPassenger() instanceof Chort;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        this.target = null;
        this.ramming = false;
        this.windingUp = false;
        this.ramHit = false;
        this.chargeTime = 0;
        this.windupTicks = 0;
        this.attacker.getNavigation().stop();
    }

    private void setHorizontal(double x, double z) {
        this.attacker.setDeltaMovement(x, this.attacker.getDeltaMovement().y, z);
    }

    @Override
    public void tick() {
        if (this.target == null || !(this.attacker.getFirstPassenger() instanceof Chort)) {
            return;
        }
        double lx = this.target.getX() - this.attacker.getX();
        double lz = this.target.getZ() - this.attacker.getZ();
        float yaw = (float) (Math.atan2(lz, lx) * 180.0 / Math.PI) - 90.0F;
        this.attacker.setYRot(yaw);
        this.attacker.yRotO = yaw;
        this.attacker.setYHeadRot(yaw);
        this.attacker.yHeadRotO = yaw;
        this.attacker.yBodyRot = yaw;
        this.attacker.yBodyRotO = yaw;
        double dx = this.attacker.getX() - this.target.getX();
        double dz = this.attacker.getZ() - this.target.getZ();
        double dy = this.attacker.getY() - this.target.getY();
        double distSq = dx * dx + dz * dz;
        if (this.windingUp) {
            this.windupTicks++;
            this.setHorizontal(0.0, 0.0);
            if (this.windupTicks == 1 && this.attacker.onGround()) {
                Vec3 v = this.attacker.getDeltaMovement();
                this.attacker.setDeltaMovement(v.x, 0.45, v.z);
            }
            if (this.windupTicks >= WINDUP_TICKS) {
                this.windingUp = false;
                this.ramming = true;
                this.ramHit = false;
                double len = Math.max(Math.sqrt(lx * lx + lz * lz), 1.0E-4);
                this.ramX = this.target.getX() + lx / len * RAM_OVERSHOOT;
                this.ramZ = this.target.getZ() + lz / len * RAM_OVERSHOOT;
            }
        } else if (!this.ramming) {
            if (distSq < this.maxDistance * this.maxDistance) {
                double len = Math.max(Math.sqrt(distSq), 1.0E-4);
                double bx = dx / len;
                double bz = dz / len;
                if (this.strafeTime <= 0) {
                    this.strafeTime = STRAFE_TICKS;
                    this.strafeDirection = this.attacker.getRandom().nextBoolean() ? 1 : -1;
                } else {
                    this.strafeTime--;
                }
                this.setHorizontal(bx * this.evadeSpeed + -bz * STRAFE_FACTOR * this.strafeDirection,
                        bz * this.evadeSpeed + bx * STRAFE_FACTOR * this.strafeDirection);
                this.attacker.getNavigation().stop();
            }
            if (this.attacker.getSensing().hasLineOfSight(this.target)) {
                this.chargeTime++;
            } else {
                this.chargeTime = 0;
            }
            if (this.chargeTime >= this.chargeInterval) {
                this.windingUp = true;
                this.windupTicks = 0;
                this.chargeTime = 0;
            }
        } else {
            double rx = this.ramX - this.attacker.getX();
            double rz = this.ramZ - this.attacker.getZ();
            double len = Math.max(Math.sqrt(rx * rx + rz * rz), 1.0E-4);
            this.setHorizontal(rx / len * this.ramSpeed, rz / len * this.ramSpeed);
            if (!this.ramHit && distSq < 2.2 && dy * dy < 2.2) {
                this.attacker.doHurtTarget(this.target);
                double kx = this.target.getX() - this.attacker.getX();
                double kz = this.target.getZ() - this.attacker.getZ();
                double klen = Math.sqrt(kx * kx + kz * kz);
                if (klen > 0.0) {
                    this.target.push(kx / klen * KNOCKBACK_HORIZONTAL, KNOCKBACK_VERTICAL, kz / klen * KNOCKBACK_HORIZONTAL);
                    this.target.hurtMarked = true;
                }
                this.ramHit = true;
            }
            if (this.ramHit || len < 0.6) {
                this.ramming = false;
                this.chargeTime = 0;
            }
        }
    }
}
