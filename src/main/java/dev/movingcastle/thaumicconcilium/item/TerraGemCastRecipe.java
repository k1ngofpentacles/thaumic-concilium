package dev.movingcastle.thaumicconcilium.item;

import dev.movingcastle.thaumicconcilium.registry.TCDataComponents;
import dev.movingcastle.thaumicconcilium.registry.TCItems;
import dev.movingcastle.thaumicconcilium.registry.TCRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Original {@code TerraGemRecipe}: exactly an active Gem of Rifts and an active Terra-cast Gem that holds a cast, nothing else. The result
 * is the rift gem with the cast added and selected (16 at most; a full gem matches nothing). The original also took any item as its "rift"
 * side and only checked the cast when building the result: fixed here.
 */
public final class TerraGemCastRecipe extends CustomRecipe {
    public TerraGemCastRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != 2) {
            return false;
        }
        ItemStack rift = ItemStack.EMPTY;
        ItemStack terra = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            if (s.is(TCItems.RIFT_GEM.get()) && rift.isEmpty()) {
                rift = s;
            } else if (s.is(TCItems.TERRA_CAST_GEM.get()) && terra.isEmpty()) {
                terra = s;
            } else {
                return false;
            }
        }
        return !rift.isEmpty() && !terra.isEmpty() && TerraCastGem.cast(terra) != null
                && RiftGem.state(rift).casts().size() < RiftGem.MAX_CASTS;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack rift = ItemStack.EMPTY;
        GemCast cast = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.is(TCItems.RIFT_GEM.get())) {
                rift = s;
            } else if (s.is(TCItems.TERRA_CAST_GEM.get())) {
                cast = TerraCastGem.cast(s);
            }
        }
        if (rift.isEmpty() || cast == null) {
            return ItemStack.EMPTY;
        }
        RiftGem.State next = RiftGem.state(rift).with(cast);
        if (next == null) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = rift.copyWithCount(1);
        copy.set(TCDataComponents.RIFT_STATE.get(), next);
        return copy;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TCRecipes.TERRA_GEM_CAST.get();
    }
}
