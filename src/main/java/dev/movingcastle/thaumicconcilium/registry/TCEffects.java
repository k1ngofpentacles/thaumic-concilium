package dev.movingcastle.thaumicconcilium.registry;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.effect.BleedEffect;
import dev.movingcastle.thaumicconcilium.effect.StunnedEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Phase 5b trunk: the Crimson Dagger's two effects. */
public final class TCEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ThaumicConcilium.MODID);

    /** Amplifier = counters - 1 (0..2). See {@link dev.movingcastle.thaumicconcilium.effect.Bleeding}. */
    public static final DeferredHolder<MobEffect, MobEffect> BLEED = EFFECTS.register("bleed", BleedEffect::new);
    /** A short effect the dagger's raise refreshes on rank-and-file cultists: no movement, no attack. */
    public static final DeferredHolder<MobEffect, MobEffect> STUNNED = EFFECTS.register("stunned", StunnedEffect::new);

    /**
     * Phase 5c: drawing from the Vessel. Speed -30 percent; it is also the client's "I am drawing" flag for its own pull (effects sync
     * natively). Refreshed a few ticks at a time by the Vessel, so it ends by itself once the tether is gone.
     */
    public static final DeferredHolder<MobEffect, MobEffect> TETHERED = EFFECTS.register("tethered", () -> {
        MobEffect effect = new MobEffect(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0x9B5CFF) { };
        effect.addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, ThaumicConcilium.id("tethered_speed"),
                -0.3, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        return effect;
    });
    /** Phase 5c: thrown clear of the horizon; the Vessel's pull does not touch the bearer (the client reads it too). */
    public static final DeferredHolder<MobEffect, MobEffect> FLUNG = EFFECTS.register("flung",
            () -> new MobEffect(net.minecraft.world.effect.MobEffectCategory.NEUTRAL, 0xB0F0FF) { });

    /**
     * Shield foci: while its holder's shield is raised, hits from any side and magic damage count as blocked, at double shield wear
     * (read by {@code spell.GuardEvents}; no tick). Beneficial, so it shows as the holder's own buff; the colour is the cycle 2 look's.
     */
    public static final DeferredHolder<MobEffect, MobEffect> AEGIS = EFFECTS.register("aegis",
            () -> new MobEffect(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL, 0x7FB2FF) { });

    private TCEffects() {
    }
}
