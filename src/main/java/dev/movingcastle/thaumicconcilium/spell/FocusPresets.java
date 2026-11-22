package dev.movingcastle.thaumicconcilium.spell;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.spell.CastStyle;
import com.leclowndu93150.thaumaturge.api.spell.Spell;
import com.leclowndu93150.thaumaturge.api.spell.SpellNode;
import com.leclowndu93150.thaumaturge.api.spell.Spells;
import com.leclowndu93150.thaumaturge.api.spell.part.SpellPart;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Ready-made spells that reproduce each original focus as it behaved out of the box (no upgrades).
 * Players can also build these, or variations, in Thaumaturge's focal manipulator once the research is done.
 */
public final class FocusPresets {
    private FocusPresets() {
    }

    public record Preset(String id, CastStyle style, SpellNode root, int warp) {
        public Preset(String id, CastStyle style, SpellNode root) {
            this(id, style, root, 0);
        }

        public Spell spell() {
            return new Spell(this.style, this.root);
        }

        public ItemStack stack() {
            ItemStack focus = new ItemStack(TTItems.FOCUS_1.get());
            Spells.setSpell(focus, this.spell());
            focus.set(DataComponents.CUSTOM_NAME, Component.translatable(ThaumicConcilium.MODID + ".focus.preset",
                    Component.translatable("research." + ThaumicConcilium.MODID + "." + this.id + ".title")));
            if (this.warp > 0) {
                focus.set(TTDataComponents.STACK_WARP.get(), this.warp);
            }
            return focus;
        }
    }

    public static final Preset SIGNAL = new Preset("signal_focus", CastStyle.INSTANT,
            origin(node("compressed_blast", Map.of("fuse", 1), node("signal", Map.of("color", 1)))));
    public static final Preset IMPULSE = new Preset("impulse_focus", CastStyle.INSTANT,
            origin(node("compressed_blast", Map.of(), node("impulse", Map.of()))));
    public static final Preset POSITIVE_BURST = new Preset("positive_burst_focus", CastStyle.INSTANT,
            origin(node("positive_burst", Map.of())));
    public static final Preset COOLING = new Preset("cooling_focus", CastStyle.CHANNELED,
            origin(node("flurry", Map.of())));

    public static final Preset REFLECTION = new Preset("reflection_focus", CastStyle.CHANNELED,
            origin(node("reflection", Map.of())), 4);
    public static final Preset SCRIBING = new Preset("scribing_focus", CastStyle.CHANNELED,
            origin(thaumaturge("beam", Map.of("range", 8), node("scribing", Map.of()))));
    public static final Preset WRATH_ATTRACTION = new Preset("wrath_attraction_focus", CastStyle.INSTANT,
            origin(node("wrath_attraction", Map.of())), 3);
    public static final Preset TAINT_ANIMATION = new Preset("taint_animation_focus", CastStyle.CHANNELED,
            origin(node("taint_animation", Map.of())), 4);
    public static final Preset ETHEREAL_MANIPULATOR = new Preset("ethereal_manipulator_focus", CastStyle.CHANNELED,
            origin(node("ethereal_manipulator", Map.of())));
    public static final Preset SPOTLIGHT = new Preset("spotlight_focus", CastStyle.CHANNELED,
            origin(thaumaturge("beam", Map.of("range", 24), node("spotlight", Map.of()))));
    public static final Preset ALCHEMIST_SPREE = new Preset("alchemist_spree_focus", CastStyle.CHANNELED,
            origin(node("alchemist_spree", Map.of())));
    public static final Preset WARP_EXTRACTION = new Preset("warp_extraction_focus", CastStyle.CHANNELED,
            origin(node("warp_extraction", Map.of())), 5);

    /** Phase 6a. The dematerialization variant is the original's focus with that upgrade (the Vis Condenser needs one). */
    public static final Preset VIS_CONDUCTOR = new Preset("vis_conductor_focus", CastStyle.CHANNELED,
            origin(node("vis_conductor", Map.of())));
    public static final Preset DEMATERIALIZATION = new Preset("dematerialization", CastStyle.CHANNELED,
            origin(node("vis_conductor", Map.of("upgrade", 1))));
    /** Phase 6c. The Wisp Launcher variant (upgrade 2, potency 2): the charged wisp that summons the Brightest One. */
    public static final Preset WISP_LAUNCHER = new Preset("wisp_launcher", CastStyle.CHANNELED,
            origin(node("vis_conductor", Map.of("upgrade", 2, "potency", 2))));

    public static final List<Preset> ALL = List.of(SIGNAL, IMPULSE, POSITIVE_BURST, COOLING,
            REFLECTION, SCRIBING, WRATH_ATTRACTION, TAINT_ANIMATION, ETHEREAL_MANIPULATOR, SPOTLIGHT, ALCHEMIST_SPREE, WARP_EXTRACTION,
            VIS_CONDUCTOR, DEMATERIALIZATION, WISP_LAUNCHER);

    private static SpellNode origin(SpellNode... children) {
        return new SpellNode(Spell.ORIGIN, Optional.empty(), Map.of(), List.of(children));
    }

    private static SpellNode node(String part, Map<String, Integer> settings, SpellNode... children) {
        return new SpellNode(ResourceKey.create(SpellPart.REGISTRY_KEY, ThaumicConcilium.id(part)),
                Optional.<ResourceKey<IAspect>>empty(), new LinkedHashMap<>(settings), List.of(children));
    }

    private static SpellNode thaumaturge(String part, Map<String, Integer> settings, SpellNode... children) {
        return new SpellNode(ResourceKey.create(SpellPart.REGISTRY_KEY, ResourceLocation.fromNamespaceAndPath("thaumaturge", part)),
                Optional.<ResourceKey<IAspect>>empty(), new LinkedHashMap<>(settings), List.of(children));
    }

    static ResourceLocation partId(String part) {
        return ThaumicConcilium.id(part);
    }
}
