package dev.movingcastle.thaumicconcilium.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Bleed: its damage lands once, on the last tick of the window (see {@link Bleeding}). Costs nothing until it is on a creature. */
public final class BleedEffect extends MobEffect {
    public BleedEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B0000);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration == 1;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        Bleeding.deal(entity, amplifier + 1);
        return true;
    }
}
