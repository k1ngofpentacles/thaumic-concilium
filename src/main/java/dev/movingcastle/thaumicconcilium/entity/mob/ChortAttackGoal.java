package dev.movingcastle.thaumicconcilium.entity.mob;

import dev.movingcastle.thaumicconcilium.entity.ChortSpit;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Original {@code ChortAttackAI}: with a target inside 16 blocks, face it and, every 40 ticks, spit three times (aim turned
 * -4, 0 and +4 degrees, speed 1.8, inaccuracy 1.0). Claims movement and looking, like the original's mutex bits 3.
 */
public final class ChortAttackGoal extends Goal {
    public static final double RANGE = 16.0;
    public static final int INTERVAL = 40;
    public static final float SPREAD_DEG = 4.0F;
    public static final float VELOCITY = 1.8F;
    public static final float INACCURACY = 1.0F;

    private final Mob attacker;
    private LivingEntity target;
    private int cooldown;

    public ChortAttackGoal(Mob attacker) {
        this.attacker = attacker;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity t = this.attacker.getTarget();
        if (t == null || !t.isAlive() || this.attacker.distanceToSqr(t) > RANGE * RANGE) {
            return false;
        }
        this.target = t;
        return true;
    }

    @Override
    public void start() {
        this.cooldown = 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null && this.target.isAlive();
    }

    @Override
    public void stop() {
        this.target = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.attacker.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
        if (this.cooldown > 0) {
            this.cooldown--;
            return;
        }
        for (int i = -1; i < 2; i++) {
            this.attacker.level().addFreshEntity(
                    ChortSpit.aimed(this.attacker.level(), this.attacker, this.target, SPREAD_DEG * i, VELOCITY, INACCURACY));
        }
        this.cooldown = INTERVAL;
    }
}
