package dev.movingcastle.thaumicconcilium.entity.mob;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.spell.CastStyle;
import com.leclowndu93150.thaumaturge.api.spell.Spell;
import com.leclowndu93150.thaumaturge.api.spell.SpellNode;
import com.leclowndu93150.thaumaturge.api.spell.Spells;
import com.leclowndu93150.thaumaturge.api.spell.TTSpellParts;
import com.leclowndu93150.thaumaturge.content.research.note.ItemResearchNote;
import com.leclowndu93150.thaumaturge.content.research.note.ResearchNoteData;
import com.leclowndu93150.thaumaturge.content.research.note.ResearchNotes;
import com.leclowndu93150.thaumaturge.content.wands.ItemWand;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import dev.movingcastle.thaumicconcilium.item.CastingBracelet;
import dev.movingcastle.thaumicconcilium.item.ResearchHint;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * The Thaumaturge's trade table, 1:1 with the original {@code Thaumaturge.valuedItems / tradeInventory} and
 * {@code ContainerThaumaturge.generateContents}, with Thaumcraft 4's items translated to Thaumaturge's.
 */
public final class ThaumaturgeTrades {
    /** Value of a completed research note: it buys a research hint instead of goods. */
    public static final int NOTE = -1;

    private record Entry(int weight, Function<HolderLookup.Provider, ItemStack> stack) {
    }

    private static final List<Entry> TABLE = new ArrayList<>();

    static {
        add(9, r -> new ItemStack(TTItems.MANA_BEAN.get()));
        // The six primal shards -> essentia crystals.
        for (String aspect : List.of("aer", "ignis", "aqua", "terra", "ordo", "perditio")) {
            add(10, r -> crystal(r, aspect));
        }
        // Two entries for the knowledge fragment, as in the original (Thaumaturge's Curio of Knowledge stands in).
        add(7, r -> new ItemStack(TTItems.CURIO_KNOWLEDGE.get()));
        add(7, r -> new ItemStack(TTItems.CURIO_KNOWLEDGE.get()));
        // Crystal clusters: the original had six primals and the balanced cluster (no Thaumaturge equivalent).
        for (String aspect : List.of("aer", "ignis", "aqua", "terra", "ordo", "perditio")) {
            add(2, r -> item("thaumaturge:crystal_" + aspect));
        }
        add(1, r -> new ItemStack(TTItems.FOCUS_POUCH.get()));
        add(1, ThaumaturgeTrades::fireFocus);
        add(4, r -> new ItemStack(TTItems.AMULET_VIS.get()));
        add(1, r -> item("forbiddenmagic:profane_rod"));
    }

    private ThaumaturgeTrades() {
    }

    private static void add(int weight, Function<HolderLookup.Provider, ItemStack> stack) {
        TABLE.add(new Entry(weight, stack));
    }

    private static ItemStack item(String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static ItemStack crystal(HolderLookup.Provider registries, String aspect) {
        ItemStack stack = new ItemStack(TTItems.ESSENTIA_CRYSTAL.get());
        registries.lookup(IAspect.REGISTRY_KEY)
                .flatMap(l -> l.get(ResourceKey.create(IAspect.REGISTRY_KEY, ResourceLocation.fromNamespaceAndPath("thaumaturge", aspect))))
                .ifPresent(h -> stack.set(TTDataComponents.CRYSTAL_ASPECT.get(), new AspectInstance(h, 1)));
        return stack;
    }

    /** The original's Wand Focus: Fire. */
    public static ItemStack fireFocus(HolderLookup.Provider registries) {
        ItemStack focus = new ItemStack(TTItems.FOCUS_1.get());
        Spells.setSpell(focus, new Spell(CastStyle.INSTANT, SpellNode.of(Spell.ORIGIN).then(
                SpellNode.of(TTSpellParts.PROJECTILE).then(SpellNode.of(TTSpellParts.FIRE)))));
        return focus;
    }

    /** Original values: mana bean 1, skull 3, wand 3, bottle o' enchanting 3, enchanted book 5, research notes. */
    @Nullable
    public static Integer value(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.is(TTItems.MANA_BEAN.get())) {
            return 1;
        }
        if (stack.is(Items.SKELETON_SKULL) || stack.is(Items.WITHER_SKELETON_SKULL) || stack.is(Items.ZOMBIE_HEAD)
                || stack.is(Items.PLAYER_HEAD) || stack.is(Items.CREEPER_HEAD)) {
            return 3;
        }
        if (stack.getItem() instanceof ItemWand && !(stack.getItem() instanceof CastingBracelet)) {
            return 3;
        }
        if (stack.is(Items.EXPERIENCE_BOTTLE)) {
            return 3;
        }
        if (stack.is(Items.ENCHANTED_BOOK)) {
            return 5;
        }
        if (stack.getItem() instanceof ItemResearchNote) {
            return NOTE;
        }
        return null;
    }

    /** Hints a completed research note may buy (original cases 0-3: Vis Conductor 1/4, Positive Burst 2/4, Impulse 1/4). */
    private static final List<ResourceLocation> NOTE_HINTS = List.of(ResearchHint.VIS_CONDUCTOR, ResearchHint.POSITIVE_BURST,
            ResearchHint.POSITIVE_BURST, ResearchHint.IMPULSE);

    /**
     * The trade itself. Returns what the Thaumaturge lays out, consuming nothing; the caller removes one offered item.
     * {@code hinted} is set when a completed note bought a hint (the caller sends the chat line).
     */
    public static List<ItemStack> haggle(ItemStack offer, RandomSource random, HolderLookup.Provider registries, boolean[] hinted) {
        List<ItemStack> out = new ArrayList<>();
        Integer base = value(offer);
        if (base == null) {
            return out;
        }
        if (offer.getItem() instanceof ItemResearchNote) {
            ResearchNoteData data = ResearchNotes.dataOf(offer);
            if (data != null && data.complete()) {
                ResourceLocation hint = NOTE_HINTS.get(random.nextInt(NOTE_HINTS.size()));
                // A hint for an entry this pack doesn't have yet (Vis Conductor arrives in phase 6) falls back to the
                // other note hints.
                if (!ResearchHint.exists(registries, hint)) {
                    hint = random.nextInt(3) == 2 ? ResearchHint.IMPULSE : ResearchHint.POSITIVE_BURST;
                }
                out.add(ResearchHint.stack(hint));
                hinted[0] = true;
                return out;
            }
        }
        int value = base;
        if (random.nextInt(5) == 0) {
            value += random.nextInt(2);
        } else if (random.nextBoolean()) {
            value -= random.nextInt(2);
        }
        int total = 0;
        for (Entry e : TABLE) {
            total += e.weight;
        }
        int rnd = random.nextInt(total);
        for (Entry e : TABLE) {
            if (rnd < e.weight && value > random.nextInt(10)) {
                ItemStack stack = e.stack.apply(registries);
                if (!stack.isEmpty()) {
                    out.add(stack);
                }
                if (value < random.nextInt(10)) {
                    return out;
                }
            }
            rnd -= e.weight;
        }
        return out;
    }

    public static int looting(ServerLevel level, DamageSource source) {
        if (source.getEntity() instanceof LivingEntity killer) {
            return level.registryAccess().lookup(Registries.ENCHANTMENT)
                    .flatMap(l -> l.get(Enchantments.LOOTING))
                    .map(h -> EnchantmentHelper.getEnchantmentLevel(h, killer)).orElse(0);
        }
        return 0;
    }
}
