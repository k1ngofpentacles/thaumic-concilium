package dev.movingcastle.thaumicconcilium.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.thaumicconcilium.registry.TCRecipes;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * A chained rift recipe (original: {@code ChainedRiftRecipe}): an item thrown onto a Hex of Predictability whose rift
 * resonates with enough essentia becomes {@code result}. JSON:
 * {@code {"type": "thaumicconcilium:rift", "catalyst": <ingredient>, "aspects": [{"aspect", "amount"}],
 * "research"?: "<entry id>", "result": <item stack>}}. When several match, the one with the most aspects wins.
 */
public record RiftRecipe(Ingredient catalyst, List<AspectAmount> aspects, Optional<ResourceLocation> research, ItemStack result)
        implements Recipe<RecipeInput> {

    public record AspectAmount(ResourceLocation aspect, int amount) {
        public static final Codec<AspectAmount> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("aspect").forGetter(AspectAmount::aspect),
                Codec.INT.fieldOf("amount").forGetter(AspectAmount::amount)).apply(i, AspectAmount::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, AspectAmount> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, AspectAmount::aspect, ByteBufCodecs.VAR_INT, AspectAmount::amount, AspectAmount::new);
    }

    public static final MapCodec<RiftRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("catalyst").forGetter(RiftRecipe::catalyst),
            AspectAmount.CODEC.listOf().fieldOf("aspects").forGetter(RiftRecipe::aspects),
            ResourceLocation.CODEC.optionalFieldOf("research").forGetter(RiftRecipe::research),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(RiftRecipe::result)).apply(i, RiftRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RiftRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, RiftRecipe::catalyst,
            AspectAmount.STREAM_CODEC.apply(ByteBufCodecs.list()), RiftRecipe::aspects,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), RiftRecipe::research,
            ItemStack.STREAM_CODEC, RiftRecipe::result,
            RiftRecipe::new);

    /** The thrown item matches and the hex holds every aspect, {@code multiplier} times over. */
    public boolean matches(ItemStack item, Map<ResourceLocation, Integer> essentia, int multiplier) {
        if (!this.catalyst.test(item)) {
            return false;
        }
        for (AspectAmount a : this.aspects) {
            if (essentia.getOrDefault(a.aspect(), 0) < a.amount() * multiplier) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return input.size() > 0 && this.catalyst.test(input.getItem(0));
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TCRecipes.RIFT.get();
    }

    @Override
    public RecipeType<?> getType() {
        return TCRecipes.RIFT_TYPE.get();
    }

    public static final class Serializer implements RecipeSerializer<RiftRecipe> {
        @Override
        public MapCodec<RiftRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RiftRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
