package dev.movingcastle.thaumicconcilium.registry;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.item.MembraneRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Special recipe serializers. */
public final class TCRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, ThaumicConcilium.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<MembraneRecipe>> NETHER_MEMBRANE =
            SERIALIZERS.register("nether_membrane", () -> new SimpleCraftingRecipeSerializer<>(MembraneRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<dev.movingcastle.thaumicconcilium.item.XylographyRecipe>> XYLOGRAPHY =
            SERIALIZERS.register("xylography", () -> new SimpleCraftingRecipeSerializer<>(dev.movingcastle.thaumicconcilium.item.XylographyRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, dev.movingcastle.thaumicconcilium.recipe.RiftRecipe.Serializer> RIFT =
            SERIALIZERS.register("rift", dev.movingcastle.thaumicconcilium.recipe.RiftRecipe.Serializer::new);

    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, ThaumicConcilium.MODID);
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeType<?>,
            net.minecraft.world.item.crafting.RecipeType<dev.movingcastle.thaumicconcilium.recipe.RiftRecipe>> RIFT_TYPE =
            TYPES.register("rift", () -> net.minecraft.world.item.crafting.RecipeType.simple(ThaumicConcilium.id("rift")));

    private TCRecipes() {
    }
}
