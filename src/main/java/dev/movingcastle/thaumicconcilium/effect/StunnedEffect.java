package dev.movingcastle.thaumicconcilium.effect;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Stunned: no movement (speed -100 percent), no melee damage (attack -100 percent), no target (cleared every tick while it
 * runs) and no damage from anything it shoots (see {@code DaggerEvents.onIncomingDamage}). The dagger refreshes it for a few
 * ticks at a time, so it ends by itself within a quarter second of the raise being dropped. Ticks only on stunned creatures.
 */
public final class StunnedEffect extends MobEffect {
    public StunnedEffect() {
        super(MobEffectCategory.HARMFUL, 0x6A0F14);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, ThaumicConcilium.id("stunned_speed"), -1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        this.addAttributeModifier(Attributes.ATTACK_DAMAGE, ThaumicConcilium.id("stunned_attack"), -1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Mob mob) {
            if (mob.getTarget() != null) {
                mob.setTarget(null);
            }
            if (!mob.getNavigation().isDone()) {
                mob.getNavigation().stop();
            }
        }
        return true;
    }
}
