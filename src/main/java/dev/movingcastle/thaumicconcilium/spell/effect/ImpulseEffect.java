package dev.movingcastle.thaumicconcilium.spell.effect;

import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.entity.Motion;
import dev.movingcastle.thaumicconcilium.registry.TCSpellBehaviors;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Effect: the Impulse focus's detonation.
 * <ul>
 *   <li>mode 0, <b>blast</b>: an explosion of strength {@code 1 + potency}. It breaks blocks only if the
 *   mobGriefing rule (and the config) allow, exactly like the original.</li>
 *   <li>mode 1, <b>pneumatic strike</b> (old upgrade): leaves blocks unharmed and hurls every creature within
 *   {@code knock_radius} blocks outward and upward.</li>
 * </ul>
 * {@code potency} 0-3 replaces the old Potency upgrade; Thaumaturge's power stat (Amplify) scales it further.
 */
public final class ImpulseEffect extends AbstractEffectBehavior {
    public static final MapCodec<ImpulseEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.floatRange(1.0F, 32.0F).optionalFieldOf("knock_radius", 10.0F).forGetter(ImpulseEffect::knockRadius)
    ).apply(i, ImpulseEffect::new));

    private final float knockRadius;

    public ImpulseEffect(float knockRadius) {
        this.knockRadius = knockRadius;
    }

    public float knockRadius() {
        return this.knockRadius;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpellBehaviors.IMPULSE.get();
    }

    @Override
    protected boolean widens() {
        return false;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        ServerLevel level = ctx.level();
        Vec3 at = target.position();
        int potency = ctx.setting("potency");
        if (ctx.setting("mode") == 1) {
            float push = 0.5F * Math.max(1, potency) * power;
            for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(this.knockRadius))) {
                Vec3 away = new Vec3(near.getX() - at.x, 0.0, near.getZ() - at.z);
                away = away.lengthSqr() > 1.0E-6 ? away.normalize() : Vec3.ZERO;
                Motion.setVelocity(near, new Vec3(away.x * push, push, away.z * push));
            }
            level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        } else {
            Level.ExplosionInteraction interaction = TCConfig.IMPULSE_BREAKS_BLOCKS.get()
                    ? Level.ExplosionInteraction.MOB
                    : Level.ExplosionInteraction.NONE;
            level.explode(ctx.caster(), at.x, at.y, at.z, (1.0F + potency) * power, interaction);
        }
    }
}
