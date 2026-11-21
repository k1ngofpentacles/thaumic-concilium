package dev.movingcastle.thaumicconcilium.spell.effect;

import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.leclowndu93150.thaumaturge.content.equipment.runic.RunicShieldState;
import com.leclowndu93150.thaumaturge.registry.TTAttachments;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.thaumicconcilium.registry.TCSpellBehaviors;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Scribing: "runic cursive". Tops up another player's runic shield by {@code base_runes + potency} points per pulse,
 * never past the maximum their runic gear grants. Runes stream visibly from caster to target. It does nothing for
 * players without runic shielding, and cannot be turned on the caster.
 */
public final class ScribingEffect extends AbstractEffectBehavior {
    public static final MapCodec<ScribingEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(1, 20).optionalFieldOf("base_runes", 2).forGetter(ScribingEffect::baseRunes)
    ).apply(i, ScribingEffect::new));

    private final int baseRunes;

    public ScribingEffect(int baseRunes) {
        this.baseRunes = baseRunes;
    }

    public int baseRunes() {
        return this.baseRunes;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpellBehaviors.SCRIBING.get();
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        LivingEntity caster = ctx.caster();
        if (!(target.entity().orElse(null) instanceof ServerPlayer player) || player == caster) {
            return;
        }
        RunicShieldState state = player.getData(TTAttachments.RUNIC_SHIELD);
        if (state.maxCharge <= 0) {
            return;
        }
        int shield = (int) player.getAbsorptionAmount();
        int granted = Math.min(state.maxCharge, shield + this.baseRunes + ctx.setting("potency"));
        if (granted > shield) {
            player.setAbsorptionAmount(granted);
            state.lastCharge = granted;
        }
        ServerLevel level = ctx.level();
        if (caster != null) {
            level.playSound(null, caster.getX(), caster.getY() + 1.0, caster.getZ(), TTSounds.WRITE.get(), SoundSource.PLAYERS,
                    0.5F, 0.4F / (ctx.random().nextFloat() * 0.4F + 0.8F));
            Vec3 from = caster.getEyePosition();
            Vec3 to = player.getBoundingBox().getCenter();
            Vec3 step = to.subtract(from);
            for (int n = 1; n <= 12; n++) {
                Vec3 p = from.add(step.scale(n / 12.0));
                level.sendParticles(ParticleTypes.ENCHANT, p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.4);
            }
        }
    }
}
