package dev.movingcastle.thaumicconcilium.aura;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.gadomancy.aura.effect.AuraEffect;
import dev.movingcastle.gadomancy.aura.effect.AuraEffectType;
import dev.movingcastle.thaumicconcilium.registry.TCAuraEffects;
import dev.visfox.forbiddenreforged.FmConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;

/**
 * Original luxuria effect ({@code GadomancyIntegration.java:76-95}): an adult animal with a breeding cooldown (age above 0) has it
 * reset. The original registers it only when Forbidden Magic's lust is on; here it does nothing when {@code general.no_lust} is set.
 */
public record ResetBreedingEffect(Timing timing) implements AuraEffect {
    public static final MapCodec<ResetBreedingEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Timing.CODEC.forGetter(ResetBreedingEffect::timing)).apply(i, ResetBreedingEffect::new));

    @Override
    public AuraEffectType<?> type() {
        return TCAuraEffects.RESET_BREEDING.get();
    }

    @Override
    public boolean affectsEntities() {
        return !FmConfig.lustDisabled();
    }

    @Override
    public boolean appliesTo(Entity entity) {
        return entity instanceof AgeableMob;
    }

    @Override
    public void affectEntity(ServerLevel level, BlockPos origin, Entity entity) {
        AgeableMob ageable = (AgeableMob) entity;
        if (ageable.getAge() > 0) {
            ageable.setAge(0);
        }
    }
}
