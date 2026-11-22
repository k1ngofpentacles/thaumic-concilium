package dev.movingcastle.thaumicconcilium.entity.mob;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * The attack AI of the Crimson Archer and Ranger (original: CrimsonArcherAttackAI / CrimsonRangerAttackAI). Shoots when the target
 * has been in sight, is within range and the cooldown is up; strafes (flipping direction every 40 ticks) while inside 85% of the
 * range, and walks toward the target otherwise. Always in ranged mode: no melee.
 */
public final class StrafeShootGoal extends Goal {
    public interface Shooter {
        void shoot(LivingEntity target);
    }

    private final Mob mob;
    private final Shooter shooter;
    private final int cooldownMax;
    private final double rangeSqr;
    private int cooldown;
    private int seeTime;
    private int strafeTime;
    private boolean clockwise;

    public StrafeShootGoal(Mob mob, Shooter shooter, int cooldownMax, float range) {
        this.mob = mob;
        this.shooter = shooter;
        this.cooldownMax = cooldownMax;
        this.rangeSqr = (double) range * range;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        mob.setAggressive(true);
        cooldown = 0;
    }

    @Override
    public void stop() {
        mob.setAggressive(false);
        seeTime = 0;
        mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        double dist = mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
        seeTime = mob.getSensing().hasLineOfSight(target) ? seeTime + 1 : 0;
        if (cooldown < cooldownMax) {
            cooldown++;
        }
        if (dist < rangeSqr * 0.85) {
            mob.getNavigation().stop();
            if (++strafeTime >= 40) {
                clockwise = !clockwise;
                strafeTime = 0;
            }
            mob.getMoveControl().strafe(dist < rangeSqr * 0.25 ? -0.5F : 0.0F, clockwise ? 0.5F : -0.5F);
        } else {
            mob.getNavigation().moveTo(target, 1.0);
        }
        mob.lookAt(target, 30.0F, 30.0F);
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (seeTime > 0 && dist <= rangeSqr && cooldown >= cooldownMax) {
            shooter.shoot(target);
            cooldown = 0;
        }
    }
}
