package dev.movingcastle.thaumicconcilium.item;

import dev.movingcastle.thaumicconcilium.registry.TCDataComponents;
import dev.movingcastle.thaumicconcilium.registry.TCItems;
import dev.movingcastle.thaumicconcilium.registry.TCRecipes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Wrapping an item in Nether Membrane (original: {@code NetherMembraneRecipe}): the membrane plus any one item that
 * isn't wrapped yet and leaves nothing behind in the grid gives one of that item, fire- and lava-proof. On 1.21 that is
 * the vanilla {@code fire_resistant} component (what netherite has); {@link TCDataComponents#MEMBRANE} marks it for the
 * original's "Nether Protection" tooltip.
 */
@EventBusSubscriber(modid = dev.movingcastle.thaumicconcilium.ThaumicConcilium.MODID)
public final class MembraneRecipe extends CustomRecipe {
    public MembraneRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean membrane = false;
        boolean item = false;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(TCItems.NETHER_MEMBRANE.get())) {
                if (membrane) {
                    return false;
                }
                membrane = true;
            } else {
                if (item || stack.has(TCDataComponents.MEMBRANE.get()) || stack.has(DataComponents.FIRE_RESISTANT)
                        || stack.hasCraftingRemainingItem()) {
                    return false;
                }
                item = true;
            }
        }
        return membrane && item;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && !stack.is(TCItems.NETHER_MEMBRANE.get())) {
                ItemStack copy = stack.copyWithCount(1);
                copy.set(DataComponents.FIRE_RESISTANT, Unit.INSTANCE);
                copy.set(TCDataComponents.MEMBRANE.get(), Unit.INSTANCE);
                return copy;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TCRecipes.NETHER_MEMBRANE.get();
    }

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        if (event.getItemStack().has(TCDataComponents.MEMBRANE.get())) {
            event.getToolTip().add(Component.translatable("thaumicconcilium.tooltip.nether_membrane").withStyle(ChatFormatting.RED));
        }
    }
}
