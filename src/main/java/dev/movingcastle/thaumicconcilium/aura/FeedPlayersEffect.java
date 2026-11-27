package dev.movingcastle.thaumicconcilium.aura;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.gadomancy.aura.effect.AuraEffect;
import dev.movingcastle.gadomancy.aura.effect.AuraEffectType;
import dev.movingcastle.thaumicconcilium.registry.TCAuraEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Original gula effect ({@code GadomancyIntegration.java:97-112}): every player in range gains 1 food and 1.0 saturation. */
public record FeedPlayersEffect(Timing timing) implements AuraEffect {
    public static final MapCodec<FeedPlayersEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Timing.CODEC.forGetter(FeedPlayersEffect::timing)).apply(i, FeedPlayersEffect::new));

    @Override
    public AuraEffectType<?> type() {
        return TCAuraEffects.FEED_PLAYERS.get();
    }

    @Override
    public boolean appliesTo(Entity entity) {
        return entity instanceof Player;
    }

    @Override
    public void affectEntity(ServerLevel level, BlockPos origin, Entity entity) {
        ((Player) entity).getFoodData().eat(1, 1.0F);
    }
}
