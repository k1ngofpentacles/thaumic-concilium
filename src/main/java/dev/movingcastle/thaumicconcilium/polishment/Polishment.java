package dev.movingcastle.thaumicconcilium.polishment;

import com.leclowndu93150.thaumaturge.api.spell.Spells;
import com.leclowndu93150.thaumaturge.api.spell.part.SpellPart;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.spell.SpellTrees;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * A polished infusion: an infusion result that is only kept when the crafter draws {@code amount} of {@code aspect} from a
 * Destabilized Crystal with the Vis Conductor as the infusion completes (see {@link PolishmentGate}). Data, one file per
 * item at {@code data/<ns>/thaumicconcilium/polishment/<name>.json}:
 * <pre>{ "result": { "item": "thaumaturge:focus_1", "spell_part": "thaumicconcilium:flurry" }, "aspect": "thaumaturge:aversio", "amount": 64 }</pre>
 * The foci are all {@code thaumaturge:focus_1}, so {@code spell_part} (a part the result's spell must contain) tells them apart.
 */
public record Polishment(ResourceLocation item, Optional<ResourceLocation> spellPart, ResourceLocation aspect, int amount) {
    public static final ResourceKey<Registry<Polishment>> KEY = ResourceKey.createRegistryKey(ThaumicConcilium.id("polishment"));

    private record Result(ResourceLocation item, Optional<ResourceLocation> spellPart) {
        static final Codec<Result> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("item").forGetter(Result::item),
                ResourceLocation.CODEC.optionalFieldOf("spell_part").forGetter(Result::spellPart)).apply(i, Result::new));
    }

    public static final Codec<Polishment> CODEC = RecordCodecBuilder.create(i -> i.group(
            Result.CODEC.fieldOf("result").forGetter(p -> new Result(p.item, p.spellPart)),
            ResourceLocation.CODEC.fieldOf("aspect").forGetter(Polishment::aspect),
            Codec.intRange(1, 100000).fieldOf("amount").forGetter(Polishment::amount))
            .apply(i, (r, a, n) -> new Polishment(r.item(), r.spellPart(), a, n)));

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty() || !BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(this.item)) {
            return false;
        }
        if (this.spellPart.isEmpty()) {
            return true;
        }
        return SpellTrees.contains(Spells.spellOf(stack), ResourceKey.create(SpellPart.REGISTRY_KEY, this.spellPart.get()));
    }

    // One cached list per registry instance (rebuilt on a datapack reload), so a tooltip or an infusion costs a scan of
    // twelve records and nothing else.
    private static volatile Object cacheKey;
    private static volatile List<Polishment> cache = List.of();

    /** The polishment entry the stack satisfies, or null when it needs none. */
    @Nullable
    public static Polishment find(HolderLookup.Provider registries, ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Optional<HolderLookup.RegistryLookup<Polishment>> lookup = registries.lookup(KEY);
        if (lookup.isEmpty()) {
            return null;
        }
        List<Polishment> all = cache;
        if (cacheKey != lookup.get()) {
            all = lookup.get().listElements().map(h -> h.value()).toList();
            cache = all;
            cacheKey = lookup.get();
        }
        for (Polishment p : all) {
            if (p.matches(stack)) {
                return p;
            }
        }
        return null;
    }

    @EventBusSubscriber(modid = ThaumicConcilium.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(DataPackRegistryEvent.NewRegistry event) {
            event.dataPackRegistry(KEY, CODEC, CODEC);
        }
    }
}
