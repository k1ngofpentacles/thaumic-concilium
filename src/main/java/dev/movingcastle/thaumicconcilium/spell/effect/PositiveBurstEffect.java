package dev.movingcastle.thaumicconcilium.spell.effect;

import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.thaumicconcilium.entity.PositiveBurstOrb;
import dev.movingcastle.thaumicconcilium.registry.TCSpellBehaviors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Effect: the Positive Burst focus. Sprays {@code 4 + rand(5 + potency)} orbs of life energy that seek out the
 * nearest player <i>other than the caster</i> and are absorbed on contact.
 * <ul>
 *   <li>{@code mode} heal / fulfillment / vitaminize: heal half a heart; or grant {@code potency} absorption
 *   (capped at 10 hearts); or a random Speed, Resistance or Strength. Replaces the two old upgrades.</li>
 *   <li>{@code potency} 0-3: old Potency upgrade.</li>
 * </ul>
 */
public final class PositiveBurstEffect extends AbstractEffectBehavior {
    public static final MapCodec<PositiveBurstEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(0, 32).optionalFieldOf("base_orbs", 4).forGetter(PositiveBurstEffect::baseOrbs),
            Codec.floatRange(0.05F, 3.0F).optionalFieldOf("speed", 1.0F).forGetter(PositiveBurstEffect::speed)
    ).apply(i, PositiveBurstEffect::new));

    private final int baseOrbs;
    private final float speed;

    public PositiveBurstEffect(int baseOrbs, float speed) {
        this.baseOrbs = baseOrbs;
        this.speed = speed;
    }

    public int baseOrbs() {
        return this.baseOrbs;
    }

    public float speed() {
        return this.speed;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpellBehaviors.POSITIVE_BURST.get();
    }

    @Override
    protected boolean widens() {
        return false;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        ServerLevel level = ctx.level();
        RandomSource random = ctx.random();
        int potency = Math.round(ctx.setting("potency") * power);
        int mode = ctx.setting("mode");
        int amount = this.baseOrbs + random.nextInt(5 + Math.max(0, potency));
        Vec3 at = target.position();
        Vec3 dir = target.direction();
        for (int n = 0; n < amount; n++) {
            Vec3 velocity = dir.scale(this.speed).add(
                    random.nextGaussian() * 0.09, random.nextGaussian() * 0.09, random.nextGaussian() * 0.09);
            PositiveBurstOrb.spawn(level, ctx.caster(), at.add(0.0, -0.5, 0.0), velocity, mode, potency);
        }
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                0.6F, 0.8F + random.nextFloat() * 0.1F);
    }
}
