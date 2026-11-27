package dev.movingcastle.thaumicconcilium.aura;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.gadomancy.aura.effect.AuraEffect;
import dev.movingcastle.gadomancy.aura.effect.AuraEffectType;
import dev.movingcastle.thaumicconcilium.registry.TCAuraEffects;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Original desidia effect ({@code GadomancyIntegration.java:157-177}): every active effect of every living thing in range lasts 20 ticks
 * longer. 1.21 has infinite effects, which are skipped (adding 20 would turn them into 19-tick effects).
 */
public record ProlongEffectsEffect(Timing timing) implements AuraEffect {
    public static final MapCodec<ProlongEffectsEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Timing.CODEC.forGetter(ProlongEffectsEffect::timing)).apply(i, ProlongEffectsEffect::new));

    @Override
    public AuraEffectType<?> type() {
        return TCAuraEffects.PROLONG_EFFECTS.get();
    }

    @Override
    public boolean appliesTo(Entity entity) {
        return entity instanceof LivingEntity;
    }

    @Override
    public void affectEntity(ServerLevel level, BlockPos origin, Entity entity) {
        LivingEntity living = (LivingEntity) entity;
        for (MobEffectInstance effect : new ArrayList<>(living.getActiveEffects())) {
            if (effect.isInfiniteDuration()) {
                continue;
            }
            living.addEffect(new MobEffectInstance(effect.getEffect(), effect.getDuration() + 20, effect.getAmplifier(), effect.isAmbient(),
                    effect.isVisible(), effect.showIcon()));
        }
    }
}
