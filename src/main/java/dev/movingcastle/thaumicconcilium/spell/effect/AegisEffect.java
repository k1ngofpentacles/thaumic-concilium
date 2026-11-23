package dev.movingcastle.thaumicconcilium.spell.effect;

import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellStats;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.mojang.serialization.MapCodec;
import dev.movingcastle.thaumicconcilium.registry.TCEffects;
import dev.movingcastle.thaumicconcilium.registry.TCSpellBehaviors;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Effect: the Aegis focus. Gives each creature it reaches the Aegis mob effect for the {@code duration} setting (5, 10, 15 or
 * 20 seconds per step, times the duration modifiers). The effect itself is read by {@code GuardEvents}: while its holder's shield
 * is raised, hits from any side and magic damage count as blocked, at double shield wear. Self gives it to the caster; an aura
 * shares it with the creatures it picks, who need their own raised shields.
 */
public final class AegisEffect extends AbstractEffectBehavior {
    public static final MapCodec<AegisEffect> CODEC = MapCodec.unit(new AegisEffect());
    private static final int SECONDS_PER_STEP = 5;

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpellBehaviors.AEGIS.get();
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        if (target.entity().orElse(null) instanceof LivingEntity living) {
            int ticks = Math.round(Math.max(1, ctx.setting("duration")) * SECONDS_PER_STEP * 20 * ctx.state().get(SpellStats.DURATION));
            living.addEffect(new MobEffectInstance(TCEffects.AEGIS, ticks, 0, false, true, true), ctx.caster());
        }
    }
}
