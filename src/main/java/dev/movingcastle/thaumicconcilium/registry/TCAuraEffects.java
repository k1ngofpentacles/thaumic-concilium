package dev.movingcastle.thaumicconcilium.registry;

import com.mojang.serialization.MapCodec;
import dev.movingcastle.gadomancy.aura.effect.AuraEffect;
import dev.movingcastle.gadomancy.aura.effect.AuraEffectType;
import dev.movingcastle.gadomancy.registry.GAuraEffects;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.aura.DisarmEffect;
import dev.movingcastle.thaumicconcilium.aura.FeedPlayersEffect;
import dev.movingcastle.thaumicconcilium.aura.MeteorShowerEffect;
import dev.movingcastle.thaumicconcilium.aura.ProlongEffectsEffect;
import dev.movingcastle.thaumicconcilium.aura.ProvokeEffect;
import dev.movingcastle.thaumicconcilium.aura.RepelEffect;
import dev.movingcastle.thaumicconcilium.aura.ResetBreedingEffect;
import dev.movingcastle.thaumicconcilium.aura.SmeltItemsEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Our aura effect types for Gadomancy's open pylon table (phase 8-5). Bound to aspects by data: data/gadomancy/data_maps/thaumaturge/aspect/aura_effect.json. */
public final class TCAuraEffects {
    public static final DeferredRegister<AuraEffectType<?>> TYPES = DeferredRegister.create(GAuraEffects.TYPE_REGISTRY_KEY, ThaumicConcilium.MODID);

    public static final DeferredHolder<AuraEffectType<?>, AuraEffectType<SmeltItemsEffect>> SMELT_ITEMS = type("smelt_items", SmeltItemsEffect.CODEC);
    public static final DeferredHolder<AuraEffectType<?>, AuraEffectType<ResetBreedingEffect>> RESET_BREEDING = type("reset_breeding", ResetBreedingEffect.CODEC);
    public static final DeferredHolder<AuraEffectType<?>, AuraEffectType<FeedPlayersEffect>> FEED_PLAYERS = type("feed_players", FeedPlayersEffect.CODEC);
    public static final DeferredHolder<AuraEffectType<?>, AuraEffectType<ProvokeEffect>> PROVOKE = type("provoke", ProvokeEffect.CODEC);
    public static final DeferredHolder<AuraEffectType<?>, AuraEffectType<DisarmEffect>> DISARM = type("disarm", DisarmEffect.CODEC);
    public static final DeferredHolder<AuraEffectType<?>, AuraEffectType<ProlongEffectsEffect>> PROLONG_EFFECTS = type("prolong_effects", ProlongEffectsEffect.CODEC);
    public static final DeferredHolder<AuraEffectType<?>, AuraEffectType<RepelEffect>> REPEL = type("repel", RepelEffect.CODEC);
    public static final DeferredHolder<AuraEffectType<?>, AuraEffectType<MeteorShowerEffect>> METEOR_SHOWER = type("meteor_shower", MeteorShowerEffect.CODEC);

    private TCAuraEffects() {
    }

    private static <T extends AuraEffect> DeferredHolder<AuraEffectType<?>, AuraEffectType<T>> type(String name, MapCodec<T> codec) {
        return TYPES.register(name, () -> new AuraEffectType<>(codec));
    }
}
