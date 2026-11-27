package dev.movingcastle.thaumicconcilium.aura;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.gadomancy.aura.effect.AuraEffect;
import dev.movingcastle.gadomancy.aura.effect.AuraEffectType;
import dev.movingcastle.thaumicconcilium.registry.TCAuraEffects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;

/**
 * Original ira effect ({@code GadomancyIntegration.java:113-132}): a monster takes a random other mob within 8 blocks (box grown by 8
 * on every axis) as the one that last hurt it, so it turns on it.
 */
public record ProvokeEffect(Timing timing) implements AuraEffect {
    public static final MapCodec<ProvokeEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Timing.CODEC.forGetter(ProvokeEffect::timing)).apply(i, ProvokeEffect::new));

    @Override
    public AuraEffectType<?> type() {
        return TCAuraEffects.PROVOKE.get();
    }

    @Override
    public boolean appliesTo(Entity entity) {
        return entity instanceof Monster;
    }

    @Override
    public void affectEntity(ServerLevel level, BlockPos origin, Entity entity) {
        List<Mob> list = new ArrayList<>(level.getEntitiesOfClass(Mob.class, entity.getBoundingBox().inflate(8.0, 8.0, 8.0)));
        list.remove(entity);
        if (!list.isEmpty()) {
            Collections.shuffle(list, new java.util.Random(level.random.nextLong()));
            ((Monster) entity).setLastHurtByMob(list.get(0));
        }
    }
}
