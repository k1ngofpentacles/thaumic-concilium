package dev.movingcastle.thaumicconcilium.aura;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.gadomancy.aura.effect.AuraEffect;
import dev.movingcastle.gadomancy.aura.effect.AuraEffectType;
import dev.movingcastle.thaumicconcilium.registry.TCAuraEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;

/**
 * Original infernus effect ({@code GadomancyIntegration.java:56-75}): a dropped item with a furnace recipe becomes its result, keeping
 * the stack's count. Bound to infernum (Thaumic Additions' aspect, our fallback when it is absent).
 */
public record SmeltItemsEffect(Timing timing) implements AuraEffect {
    public static final MapCodec<SmeltItemsEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Timing.CODEC.forGetter(SmeltItemsEffect::timing)).apply(i, SmeltItemsEffect::new));

    @Override
    public AuraEffectType<?> type() {
        return TCAuraEffects.SMELT_ITEMS.get();
    }

    @Override
    public boolean appliesTo(Entity entity) {
        return entity instanceof ItemEntity;
    }

    @Override
    public void affectEntity(ServerLevel level, BlockPos origin, Entity entity) {
        ItemEntity item = (ItemEntity) entity;
        ItemStack stack = item.getItem();
        SingleRecipeInput input = new SingleRecipeInput(stack);
        level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level).ifPresent(holder -> {
            ItemStack result = holder.value().assemble(input, level.registryAccess());
            if (!result.isEmpty()) {
                result.setCount(stack.getCount());
                item.setItem(result);
            }
        });
    }
}
