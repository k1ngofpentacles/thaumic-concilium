package dev.movingcastle.thaumicconcilium.spell.effect;

import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.mojang.serialization.MapCodec;
import dev.movingcastle.thaumicconcilium.entity.Motion;
import dev.movingcastle.thaumicconcilium.registry.TCSpellBehaviors;
import dev.movingcastle.thaumicconcilium.spell.Looking;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/**
 * Ethereal Manipulator: telekinesis for loose things. While channelled it grips the dropped item or projectile
 * (arrows, fireballs, thrown potions, spell carriers...) the caster is looking at and holds it in front of them at
 * the chosen distance. As in the original, the mouse wheel moves the held object nearer or further (1 to 16 blocks)
 * while channelling; the distance falls back to the {@code distance} setting when the channel ends.
 */
public final class EtherealManipulatorEffect extends AbstractEffectBehavior {
    public static final EtherealManipulatorEffect INSTANCE = new EtherealManipulatorEffect();
    public static final MapCodec<EtherealManipulatorEffect> CODEC = MapCodec.unit(INSTANCE);

    private static final Map<UUID, Float> RANGES = new ConcurrentHashMap<>();

    private EtherealManipulatorEffect() {
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpellBehaviors.ETHEREAL_MANIPULATOR.get();
    }

    @Override
    protected boolean widens() {
        return false;
    }

    public static void adjustRange(LivingEntity caster, int steps) {
        RANGES.compute(caster.getUUID(), (k, v) -> Mth.clamp((v == null ? 5.0F : v) + steps, 1.0F, 16.0F));
    }

    public static void resetRange(LivingEntity caster) {
        RANGES.remove(caster.getUUID());
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        LivingEntity caster = ctx.caster();
        if (caster == null || index > 0) {
            return;
        }
        float range = RANGES.computeIfAbsent(caster.getUUID(), k -> (float) Math.max(1, ctx.setting("distance")));
        Entity held = Looking.at(caster, range, 3.0, e -> e.isAlive() && (e instanceof ItemEntity || e instanceof Projectile));
        if (held == null) {
            return;
        }
        Vec3 point = caster.getBoundingBox().getCenter().add(caster.getLookAngle().scale(range - 1.0F)).add(0.0, 0.5, 0.0);
        Motion.pullTowards(held, point, 0.3333F);
        if (held instanceof Projectile) {
            held.hasImpulse = true;
        }
        Vec3 c = held.getBoundingBox().getCenter();
        ctx.level().sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 1, 0.1, 0.1, 0.1, 0.0);
    }
}
