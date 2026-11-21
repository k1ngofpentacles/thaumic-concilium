package dev.movingcastle.thaumicconcilium.spell.effect;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.leclowndu93150.thaumaturge.content.research.pool.AspectPools;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import com.mojang.serialization.MapCodec;
import dev.movingcastle.thaumicconcilium.registry.TCSpellBehaviors;
import dev.movingcastle.thaumicconcilium.spell.Experience;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/**
 * Reflection: the focus put to one's temple to "rid you of visions of the past". It exchanges experience, or with
 * Oblivion research points, for temporary warp (the fuel the warp baubles and Warp Extraction burn). Ported 1:1 from
 * {@code ReflectionFoci}: the original ran every tick of channelling, so each pulse here runs the original's per-tick
 * rule once for every tick since this player's last pulse.
 * <ul>
 *   <li>mode 0, <b>experience</b>, per tick: with at least {@code 10 + potency} experience points, drain 10 and gain
 *   {@code 1 + potency} temporary warp.</li>
 *   <li>mode 1, <b>oblivion</b> (old upgrade), every 5 ticks: pick a random compound aspect; if the player has a point
 *   of it in their research pool, forget it (with the original message) and gain {@code 10 + 2 * potency} temporary
 *   warp.</li>
 * </ul>
 * The original's Frugal lowered the experience price; Thaumaturge's Efficient lowers vis instead. The original cost no
 * vis at all, but Thaumaturge refuses a cast that costs nothing, so the part costs the minimum.
 * Applies to the player it lands on; cast through Self (or straight off the origin) it is the caster.
 */
public final class ReflectionEffect extends AbstractEffectBehavior {
    public static final MapCodec<ReflectionEffect> CODEC = MapCodec.unit(ReflectionEffect::new);
    private static final int XP_PER_TICK = 10;
    private static final int OBLIVION_PERIOD = 5;
    /** Per player: game time of the last pulse, and ticks counted towards the next Oblivion attempt. */
    private static final Map<UUID, long[]> TICKS = new HashMap<>();

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpellBehaviors.REFLECTION.get();
    }

    @Override
    protected boolean widens() {
        return false;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        if (!(target.entity().orElse(null) instanceof ServerPlayer player)) {
            return;
        }
        int potency = ctx.setting("potency");
        long now = ctx.level().getGameTime();
        long[] state = TICKS.computeIfAbsent(player.getUUID(), k -> new long[]{now - 1, 0});
        long since = now - state[0];
        // A gap longer than a pulse means a new channel: count one tick, as the original's first tick.
        int ticks = since <= 0 || since > 20 ? 1 : (int) since;
        state[0] = now;
        if (now / 20 != (now - ticks) / 20) {
            ctx.level().playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.WHISPERS.get(), SoundSource.PLAYERS,
                    0.5F, 0.4F / (ctx.random().nextFloat() * 0.4F + 0.8F));
        }
        boolean changed = false;
        for (int t = 0; t < ticks; t++) {
            if (ctx.setting("mode") == 1) {
                if (++state[1] % OBLIVION_PERIOD == 0) {
                    changed |= this.oblivion(ctx, player, potency);
                }
            } else if (Experience.total(player) >= XP_PER_TICK + potency && Experience.drain(player, XP_PER_TICK)) {
                WarpHelper.addWarp(player, 1 + potency, WarpType.TEMPORARY);
                changed = true;
            }
        }
        if (changed) {
            ctx.level().sendParticles(ParticleTypes.PORTAL, player.getX(), player.getEyeY(), player.getZ(), 6, 0.3, 0.2, 0.3, 0.2);
        }
    }

    /** The original: a random compound aspect, forgotten only if the player has a point of it. */
    private boolean oblivion(CastContext ctx, ServerPlayer player, int potency) {
        List<Holder<IAspect>> compounds = new ArrayList<>();
        ctx.registries().registryOrThrow(IAspect.REGISTRY_KEY).holders().forEach(h -> {
            if (!h.value().isPrimal()) {
                compounds.add(h);
            }
        });
        if (compounds.isEmpty()) {
            return false;
        }
        Holder<IAspect> aspect = compounds.get(ctx.random().nextInt(compounds.size()));
        if (AspectPools.amount(player, aspect) <= 0 || !AspectPools.spend(player, aspect, 1)) {
            return false;
        }
        String id = AspectPools.idOf(aspect).toLanguageKey("aspect");
        player.sendSystemMessage(Component.translatable("message.thaumicconcilium.oblivion_response")
                .append(" ").append(Component.translatable(id + ".help")).withStyle(ChatFormatting.DARK_PURPLE));
        WarpHelper.addWarp(player, 10 + 2 * potency, WarpType.TEMPORARY);
        return true;
    }
}
