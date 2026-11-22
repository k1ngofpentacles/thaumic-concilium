package dev.movingcastle.thaumicconcilium.registry;

import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.part.SpellPartKind;
import com.mojang.serialization.MapCodec;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.spell.delivery.CompressedBlastDelivery;
import dev.movingcastle.thaumicconcilium.spell.delivery.FlurryDelivery;
import dev.movingcastle.thaumicconcilium.spell.effect.AlchemistSpreeEffect;
import dev.movingcastle.thaumicconcilium.spell.effect.EtherealManipulatorEffect;
import dev.movingcastle.thaumicconcilium.spell.effect.ImpulseEffect;
import dev.movingcastle.thaumicconcilium.spell.effect.PositiveBurstEffect;
import dev.movingcastle.thaumicconcilium.spell.effect.ReflectionEffect;
import dev.movingcastle.thaumicconcilium.spell.effect.ScribingEffect;
import dev.movingcastle.thaumicconcilium.spell.effect.SignalEffect;
import dev.movingcastle.thaumicconcilium.spell.effect.SpotlightEffect;
import dev.movingcastle.thaumicconcilium.spell.effect.TaintAnimationEffect;
import dev.movingcastle.thaumicconcilium.spell.effect.WarpExtractionEffect;
import dev.movingcastle.thaumicconcilium.spell.effect.WrathAttractionEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Spell behaviours registered into Thaumaturge's behaviour registry. Each original focus becomes one
 * signature part; the spell-part JSON in {@code data/thaumicconcilium/thaumaturge/spell_part} wires
 * icons, aspects, research gates and the settings that replace the old focus upgrades.
 */
public final class TCSpellBehaviors {
    public static final DeferredRegister<SpellBehaviorType<?>> BEHAVIORS =
            DeferredRegister.create(SpellBehaviorType.REGISTRY_KEY, ThaumicConcilium.MODID);

    // Deliveries
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<CompressedBlastDelivery>> COMPRESSED_BLAST =
            register("compressed_blast", SpellPartKind.DELIVERY, CompressedBlastDelivery.CODEC);
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<FlurryDelivery>> FLURRY =
            register("flurry", SpellPartKind.DELIVERY, FlurryDelivery.CODEC);

    // Effects
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<ImpulseEffect>> IMPULSE =
            register("impulse", SpellPartKind.EFFECT, ImpulseEffect.CODEC);
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<SignalEffect>> SIGNAL =
            register("signal", SpellPartKind.EFFECT, SignalEffect.CODEC);
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<PositiveBurstEffect>> POSITIVE_BURST =
            register("positive_burst", SpellPartKind.EFFECT, PositiveBurstEffect.CODEC);
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<ReflectionEffect>> REFLECTION =
            register("reflection", SpellPartKind.EFFECT, ReflectionEffect.CODEC);
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<ScribingEffect>> SCRIBING =
            register("scribing", SpellPartKind.EFFECT, ScribingEffect.CODEC);
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<WrathAttractionEffect>> WRATH_ATTRACTION =
            register("wrath_attraction", SpellPartKind.EFFECT, WrathAttractionEffect.CODEC);
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<TaintAnimationEffect>> TAINT_ANIMATION =
            register("taint_animation", SpellPartKind.EFFECT, TaintAnimationEffect.CODEC);
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<EtherealManipulatorEffect>> ETHEREAL_MANIPULATOR =
            register("ethereal_manipulator", SpellPartKind.EFFECT, EtherealManipulatorEffect.CODEC);
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<SpotlightEffect>> SPOTLIGHT =
            register("spotlight", SpellPartKind.EFFECT, SpotlightEffect.CODEC);
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<AlchemistSpreeEffect>> ALCHEMIST_SPREE =
            register("alchemist_spree", SpellPartKind.EFFECT, AlchemistSpreeEffect.CODEC);
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<WarpExtractionEffect>> WARP_EXTRACTION =
            register("warp_extraction", SpellPartKind.EFFECT, WarpExtractionEffect.CODEC);

    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<dev.movingcastle.thaumicconcilium.spell.effect.VisConductorEffect>> VIS_CONDUCTOR =
            register("vis_conductor", SpellPartKind.EFFECT, dev.movingcastle.thaumicconcilium.spell.effect.VisConductorEffect.CODEC);

    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<dev.movingcastle.thaumicconcilium.spell.effect.CrimsonRaidEffect>> CRIMSON_RAID =
            register("crimson_raid", SpellPartKind.EFFECT, dev.movingcastle.thaumicconcilium.spell.effect.CrimsonRaidEffect.CODEC);

    private static <B extends SpellBehavior> DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<B>> register(
            String name, SpellPartKind kind, MapCodec<B> codec) {
        return BEHAVIORS.register(name, () -> new SpellBehaviorType<>(kind, codec));
    }

    private TCSpellBehaviors() {
    }
}
