package dev.movingcastle.thaumicconcilium.aura;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.gadomancy.aura.effect.AuraEffect;
import dev.movingcastle.gadomancy.aura.effect.AuraEffectType;
import dev.movingcastle.thaumicconcilium.registry.TCAuraEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Original invidia effect ({@code GadomancyIntegration.java:133-156}): every living thing but a player drops what it holds in its main hand. */
public record DisarmEffect(Timing timing) implements AuraEffect {
    public static final MapCodec<DisarmEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Timing.CODEC.forGetter(DisarmEffect::timing)).apply(i, DisarmEffect::new));

    @Override
    public AuraEffectType<?> type() {
        return TCAuraEffects.DISARM.get();
    }

    @Override
    public boolean appliesTo(Entity entity) {
        return entity instanceof LivingEntity && !(entity instanceof Player);
    }

    @Override
    public void affectEntity(ServerLevel level, BlockPos origin, Entity entity) {
        LivingEntity living = (LivingEntity) entity;
        ItemStack held = living.getMainHandItem();
        if (!held.isEmpty()) {
            living.spawnAtLocation(held.copy(), 0.5F);
            living.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }
}
