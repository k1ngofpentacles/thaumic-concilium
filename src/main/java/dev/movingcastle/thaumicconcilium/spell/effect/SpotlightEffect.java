package dev.movingcastle.thaumicconcilium.spell.effect;

import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.leclowndu93150.thaumaturge.api.taint.TaintApi;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.thaumicconcilium.registry.TCSounds;
import dev.movingcastle.thaumicconcilium.registry.TCSpellBehaviors;
import dev.movingcastle.thaumicconcilium.spell.Taint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Spotlight: a searing beam whose heat builds the longer it is held. Each hit deals
 * {@code (1 + seconds * potency)} damage that ignores armour, where {@code seconds} is how long the caster has been
 * channelling. Releasing it leaves the caster's focus to cool down for a while (see
 * {@link dev.movingcastle.thaumicconcilium.spell.ChannelEvents}). Needs a Ring of Blustering Light.
 * <ul>
 *   <li>mode 0, <b>searing</b>.</li>
 *   <li>mode 1, <b>warming up</b> (old upgrade): heals instead, granting strong regeneration.</li>
 *   <li>mode 2, <b>taint purification</b> (old upgrade): harmless to ordinary creatures; cures tainted animals
 *   outright, burns taint and eldritch creatures twice as fast, and scrubs taint where the beam lands.</li>
 * </ul>
 */
public final class SpotlightEffect extends AbstractEffectBehavior {
    public static final MapCodec<SpotlightEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("purification", 0.05F).forGetter(SpotlightEffect::purification)
    ).apply(i, SpotlightEffect::new));

    private final float purification;

    public SpotlightEffect(float purification) {
        this.purification = purification;
    }

    public float purification() {
        return this.purification;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpellBehaviors.SPOTLIGHT.get();
    }

    @Override
    protected boolean widens() {
        return false;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        LivingEntity caster = ctx.caster();
        ServerLevel level = ctx.level();
        int mode = ctx.setting("mode");
        int potency = ctx.setting("potency");
        float seconds = caster != null && caster.isUsingItem() ? caster.getTicksUsingItem() / 20.0F : 0.0F;
        if (caster != null && ctx.random().nextInt(3) == 0) {
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), TCSounds.HUM.get(), SoundSource.PLAYERS, 0.5F, 1.0F);
        }

        if (mode == 2) {
            target.block().ifPresent(hit -> purifyGround(level, hit.getBlockPos()));
        }
        if (!(target.entity().orElse(null) instanceof LivingEntity victim) || victim == caster) {
            return;
        }
        switch (mode) {
            case 1 -> victim.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20, 5 + potency));
            case 2 -> {
                if (Taint.isCurable(victim)) {
                    if (Taint.cure(level, victim)) {
                        level.sendParticles(ParticleTypes.END_ROD, victim.getX(), victim.getY(0.5), victim.getZ(), 16, 0.4, 0.6, 0.4, 0.05);
                    }
                } else if (Taint.isTainted(victim) || Taint.isEldritch(victim)) {
                    sear(ctx, caster, victim, (1.0F + seconds * 2.0F * potency) * Math.max(1.0F, power));
                }
            }
            default -> sear(ctx, caster, victim, (1.0F + seconds * potency) * Math.max(1.0F, power));
        }
    }

    private static void sear(CastContext ctx, LivingEntity caster, LivingEntity victim, float damage) {
        var sources = ctx.level().damageSources();
        victim.hurt(caster != null ? sources.indirectMagic(caster, caster) : sources.magic(), damage);
    }

    private void purifyGround(ServerLevel level, BlockPos pos) {
        TaintApi.cleanEcologicalPressure(level, pos, this.purification);
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (level.getBlockState(p).is(TTBlocks.TAINT_FIBRE.get())) {
                level.removeBlock(p, false);
                level.sendParticles(ParticleTypes.END_ROD, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.02);
            }
        }
    }
}
