package dev.movingcastle.thaumicconcilium.effect;

import dev.movingcastle.thaumicconcilium.registry.TCEffects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

/**
 * The Crimson Dagger's Bleed rules (user decision 2026-11-21: 1 damage every 3 s for 3 s per application, stacking to 3
 * counters; the exact reading below is provisional, one place).
 *
 * <ul>
 *   <li>A stab starts a window of {@link #WINDOW_TICKS} (3 s) with 1 counter; the window's damage lands once, on its last
 *       tick: {@link #DAMAGE_PER_COUNTER} per counter.</li>
 *   <li>A stab inside a running window adds a counter (up to {@link #MAX_COUNTERS}) and keeps the window's remaining time:
 *       stabbing does not postpone the damage. A stab at 3 counters changes nothing.</li>
 *   <li>The window's damage goes through {@code hurt} with the invulnerability frames cleared, and counts as the stabber's
 *       when they are online. {@link #damaging()} is true while it is dealt, so a kill by it counts as a dagger kill.</li>
 * </ul>
 */
public final class Bleeding {
    /** PROVISIONAL (branch decision): one window, 3 s. */
    public static final int WINDOW_TICKS = 60;
    /** PROVISIONAL: counters a creature can carry. */
    public static final int MAX_COUNTERS = 3;
    /** PROVISIONAL: damage per counter, once per window. */
    public static final float DAMAGE_PER_COUNTER = 1.0F;
    private static final String BY_KEY = "thaumicconcilium_bleed_by";
    /** PROVISIONAL(R7): Neo Vitae's sacrifice damage type (it replaced Enchanted's) while the damage-source decision is open. */
    private static final ResourceKey<DamageType> SACRIFICE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("neovitae", "sacrifice"));

    private static boolean damaging;

    private Bleeding() {
    }

    /** True only while a bleed window's damage is being dealt (so its kills and drops can be told apart). */
    public static boolean damaging() {
        return damaging;
    }

    public static int counters(LivingEntity entity) {
        MobEffectInstance bleed = entity.getEffect(TCEffects.BLEED);
        return bleed == null ? 0 : bleed.getAmplifier() + 1;
    }

    /** One stab's worth of bleed. {@code by} is remembered for the kill credit. */
    public static void apply(LivingEntity target, @Nullable Player by) {
        if (target.level().isClientSide()) {
            return;
        }
        MobEffectInstance old = target.getEffect(TCEffects.BLEED);
        if (by != null) {
            target.getPersistentData().putUUID(BY_KEY, by.getUUID());
        }
        if (old == null) {
            target.addEffect(new MobEffectInstance(TCEffects.BLEED, WINDOW_TICKS, 0, false, true, true));
        } else if (old.getAmplifier() + 1 < MAX_COUNTERS) {
            target.addEffect(new MobEffectInstance(TCEffects.BLEED, old.getDuration(), old.getAmplifier() + 1, false, true, true));
        }
    }

    /** The window's damage (called by the effect on its last tick). */
    static void deal(LivingEntity entity, int counters) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        Player by = null;
        if (entity.getPersistentData().hasUUID(BY_KEY)) {
            UUID id = entity.getPersistentData().getUUID(BY_KEY);
            by = level.getServer().getPlayerList().getPlayer(id);
        }
        DamageSource source = source(level, by);
        damaging = true;
        try {
            entity.invulnerableTime = 0;
            entity.hurt(source, DAMAGE_PER_COUNTER * counters);
        } finally {
            damaging = false;
        }
    }

    /** The dagger's damage source: Neo Vitae's sacrifice type when it is loaded (PROVISIONAL(R7)), otherwise magic (ignores armour). */
    public static DamageSource source(ServerLevel level, @Nullable Player by) {
        if (ModList.get().isLoaded("neovitae")) {
            Holder<DamageType> type = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(SACRIFICE).orElse(null);
            if (type != null) {
                return new DamageSource(type, by, by);
            }
        }
        return by == null ? level.damageSources().magic() : level.damageSources().indirectMagic(by, by);
    }
}
