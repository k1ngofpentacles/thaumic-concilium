package dev.movingcastle.thaumicconcilium.item;

import dev.movingcastle.thaumicconcilium.registry.TCDataComponents;
import dev.movingcastle.thaumicconcilium.registry.TCRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Original {@code WandXylography}: exactly one wand in the grid, nothing else. The result is a copy with a placeholder
 * signature that {@link Xylography#crafted} replaces with the crafter.
 */
public final class XylographyRecipe extends CustomRecipe {
    public XylographyRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return input.ingredientCount() == 1 && wand(input) != null;
    }

    private static ItemStack wand(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                return Xylography.signable(stack) ? stack : null;
            }
        }
        return null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack wand = wand(input);
        if (wand == null) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = wand.copyWithCount(1);
        copy.set(TCDataComponents.XYLOGRAPHY.get(), Xylography.Signer.PENDING);
        return copy;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TCRecipes.XYLOGRAPHY.get();
    }
}
