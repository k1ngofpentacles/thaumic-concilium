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

    private TCEffects() {
    }
}
