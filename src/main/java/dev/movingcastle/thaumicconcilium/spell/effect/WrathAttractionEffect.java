package dev.movingcastle.thaumicconcilium.spell.effect;

import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.thaumicconcilium.registry.TCSounds;
import dev.movingcastle.thaumicconcilium.registry.TCSpellBehaviors;
import dev.movingcastle.thaumicconcilium.spell.ServerTasks;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Wrath Attraction: every hostile creature within {@code 5 + 5 * reach} blocks turns its anger on the target.
 * Straight off the origin (or through Self) that is the caster, as in the original, so the best-armoured member of
 * a party can draw the mob; aimed with a delivery, it marks someone else. A burning sigil of Ira rises over the
 * target. With <b>burnout</b> (old upgrade) the embittered creatures are also badly weakened.
 */
public final class WrathAttractionEffect extends AbstractEffectBehavior {
    public static final MapCodec<WrathAttractionEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.floatRange(1.0F, 64.0F).optionalFieldOf("base_radius", 5.0F).forGetter(WrathAttractionEffect::baseRadius),
            Codec.floatRange(0.0F, 64.0F).optionalFieldOf("radius_per_reach", 5.0F).forGetter(WrathAttractionEffect::radiusPerReach)
    ).apply(i, WrathAttractionEffect::new));

    /** The original's sigil, as (height, half-width) pixel pairs traced two per tick. */
    private static final int[][] IRA = {
            {0, 5}, {1, 4}, {2, 3}, {3, 3}, {4, 3}, {5, 3}, {6, 3}, {7, 3}, {8, 3}, {9, 3}, {10, 3}, {11, 3}, {12, 4}, {13, 4},
            {14, 1}, {14, 5}, {14, 10}, {14, 12}, {15, 2}, {15, 12}, {16, 3}, {16, 13}, {17, 1}, {17, 4}, {17, 9}, {17, 14},
            {18, 2}, {18, 5}, {18, 7}, {18, 9}, {18, 13}, {18, 16}, {19, 3}, {19, 11}, {19, 14}, {20, 3}, {20, 12}, {20, 14},
            {21, 3}, {21, 12}, {21, 14}, {22, 3}, {22, 12}, {22, 14}, {23, 3}, {25, 2}, {26, 1}, {24, 3}, {23, 2}, {22, 1}};

    private final float baseRadius;
    private final float radiusPerReach;

    public WrathAttractionEffect(float baseRadius, float radiusPerReach) {
        this.baseRadius = baseRadius;
        this.radiusPerReach = radiusPerReach;
    }

    public float baseRadius() {
        return this.baseRadius;
    }

    public float radiusPerReach() {
        return this.radiusPerReach;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpellBehaviors.WRATH_ATTRACTION.get();
    }

    @Override
    protected boolean widens() {
        return false;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        LivingEntity taunter = target.entity().orElse(null) instanceof LivingEntity l ? l : ctx.caster();
        if (taunter == null) {
            return;
        }
        ServerLevel level = ctx.level();
        boolean burnout = ctx.setting("burnout") == 1;
        double range = this.baseRadius + this.radiusPerReach * ctx.setting("reach");
        Vec3 at = target.position();
        for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(at, at).inflate(range),
                m -> m instanceof Enemy && m != taunter && m.isAlive())) {
            mob.setTarget(taunter);
            mob.setLastHurtByMob(taunter);
            if (burnout) {
                mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 3));
            }
        }
        level.playSound(null, taunter.getX(), taunter.getY(), taunter.getZ(), TCSounds.IRA.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        sigil(level, taunter, burnout);
    }

    /** Traces the sigil above the target over 25 ticks; with burnout, lava sparks join in after the first ten. */
    private static void sigil(ServerLevel level, LivingEntity anchor, boolean burnout) {
        double yaw = Math.toRadians(anchor.getYRot());
        double baseX = anchor.getX();
        double baseY = anchor.getY();
        double baseZ = anchor.getZ();
        int[] age = {0};
        ServerTasks.add(() -> {
            if (level.getServer() == null || age[0] >= IRA.length) {
                return false;
            }
            for (int k = 0; k < 2; k++) {
                int[] px = IRA[(age[0] + k) % IRA.length];
                double y = baseY + px[0] * 0.2;
                for (int side = -1; side <= 1; side += 2) {
                    double x = baseX - Math.cos(yaw) * px[1] * 0.3 * side;
                    double z = baseZ - Math.sin(yaw) * px[1] * 0.3 * side;
                    level.sendParticles(ParticleTypes.FLAME, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
                    if (burnout && age[0] > 20) {
                        level.sendParticles((ParticleOptions) ParticleTypes.LAVA, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
                    }
                }
            }
            age[0] += 2;
            return true;
        });
    }
}
