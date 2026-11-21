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
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.thaumicconcilium.registry.TCSpellBehaviors;
import dev.movingcastle.thaumicconcilium.spell.Experience;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/**
 * Reflection: the focus put to one's temple to "rid you of visions of the past". It trades something for
 * temporary warp, the fuel Warp Extraction burns.
 * <ul>
 *   <li>mode 0, <b>experience</b>: each pulse drains {@code xp_per_pulse} experience points and grants
 *   {@code (1 + potency) * warp_per_pulse} temporary warp. Without enough experience nothing happens.</li>
 *   <li>mode 1, <b>oblivion</b> (old upgrade): each pulse forgets one point of a random compound aspect the
 *   target still has in their research pool, with the original whispered message, for
 *   {@code 10 + 2 * potency} temporary warp.</li>
 * </ul>
 * Applies to the player it lands on; cast through Self (or straight off the origin) it is the caster.
 * The original ran every tick; the defaults here are tuned for Thaumaturge's channel pulse of about five ticks.
 */
public final class ReflectionEffect extends AbstractEffectBehavior {
    public static final MapCodec<ReflectionEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(1, 1000).optionalFieldOf("xp_per_pulse", 50).forGetter(ReflectionEffect::xpPerPulse),
            Codec.intRange(1, 100).optionalFieldOf("warp_per_pulse", 5).forGetter(ReflectionEffect::warpPerPulse)
    ).apply(i, ReflectionEffect::new));

    private final int xpPerPulse;
    private final int warpPerPulse;

    public ReflectionEffect(int xpPerPulse, int warpPerPulse) {
        this.xpPerPulse = xpPerPulse;
        this.warpPerPulse = warpPerPulse;
    }

    public int xpPerPulse() {
        return this.xpPerPulse;
    }

    public int warpPerPulse() {
        return this.warpPerPulse;
    }

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
        if (ctx.random().nextInt(4) == 0) {
            ctx.level().playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.WHISPERS.get(), SoundSource.PLAYERS,
                    0.5F, 0.4F / (ctx.random().nextFloat() * 0.4F + 0.8F));
        }
        if (ctx.setting("mode") == 1) {
            this.oblivion(ctx, player, potency);
        } else if (Experience.drain(player, this.xpPerPulse)) {
            WarpHelper.addWarp(player, (1 + potency) * this.warpPerPulse, WarpType.TEMPORARY);
            this.puff(ctx, player);
        }
    }

    private void oblivion(CastContext ctx, ServerPlayer player, int potency) {
        List<Holder<IAspect>> known = new ArrayList<>();
        ctx.registries().registryOrThrow(IAspect.REGISTRY_KEY).holders().forEach(h -> {
            if (!h.value().isPrimal() && AspectPools.amount(player, h) > 0) {
                known.add(h);
            }
        });
        if (known.isEmpty()) {
            return;
        }
        Holder<IAspect> aspect = known.get(ctx.random().nextInt(known.size()));
        if (AspectPools.spend(player, aspect, 1)) {
            String id = AspectPools.idOf(aspect).toLanguageKey("aspect");
            player.sendSystemMessage(Component.translatable("message.thaumicconcilium.oblivion_response")
                    .append(" ").append(Component.translatable(id + ".help")).withStyle(ChatFormatting.DARK_PURPLE));
            WarpHelper.addWarp(player, 10 + 2 * potency, WarpType.TEMPORARY);
            this.puff(ctx, player);
        }
    }

    private void puff(CastContext ctx, ServerPlayer player) {
        ctx.level().sendParticles(ParticleTypes.PORTAL, player.getX(), player.getEyeY(), player.getZ(), 6, 0.3, 0.2, 0.3, 0.2);
    }
}
