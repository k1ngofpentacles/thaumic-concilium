package dev.movingcastle.thaumicconcilium.spell.effect;

import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.leclowndu93150.thaumaturge.content.entity.EntityEldritchOrb;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.thaumicconcilium.entity.FlurryShot;
import dev.movingcastle.thaumicconcilium.registry.TCSounds;
import dev.movingcastle.thaumicconcilium.registry.TCSpellBehaviors;
import dev.movingcastle.thaumicconcilium.spell.Taint;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Alchemist's Spree: the party concentrates its fire on one trusted marksman, and the focus re-forges every
 * projectile that reaches them and sends it back out along their aim. Projectiles inside a box of
 * {@code 3 x 1.5 x 3} blocks around the target (normally the caster) are transmuted:
 * <ul>
 *   <li>snowball: frost shard dealing {@code 1 + potency}; with <b>primal essence</b> it also chills;</li>
 *   <li>egg: a tainted spider hatches on the spot;</li>
 *   <li>small fireball (blaze shot, fire charge, the original's ember): an explosive fireball of strength
 *   {@code 1 + potency};</li>
 *   <li>eldritch orb: a piercing eldritch ray dealing {@code 3 * (1 + potency)}.</li>
 * </ul>
 * Needs a Ring of Blustering Light. Bottles of clear water (to thick taint) join once those bottles are ported.
 */
public final class AlchemistSpreeEffect extends AbstractEffectBehavior {
    public static final MapCodec<AlchemistSpreeEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.floatRange(0.5F, 16.0F).optionalFieldOf("reach_xz", 3.0F).forGetter(AlchemistSpreeEffect::reachXz),
            Codec.floatRange(0.5F, 16.0F).optionalFieldOf("reach_y", 1.5F).forGetter(AlchemistSpreeEffect::reachY)
    ).apply(i, AlchemistSpreeEffect::new));

    private final float reachXz;
    private final float reachY;

    public AlchemistSpreeEffect(float reachXz, float reachY) {
        this.reachXz = reachXz;
        this.reachY = reachY;
    }

    public float reachXz() {
        return this.reachXz;
    }

    public float reachY() {
        return this.reachY;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpellBehaviors.ALCHEMIST_SPREE.get();
    }

    @Override
    protected boolean widens() {
        return false;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        LivingEntity caster = ctx.caster();
        if (caster == null || index > 0) {
            return;
        }
        ServerLevel level = ctx.level();
        int potency = ctx.setting("potency");
        boolean primal = ctx.setting("primal") == 1;
        Entity anchor = target.entity().orElse(caster);
        AABB area = anchor.getBoundingBox().inflate(this.reachXz, this.reachY, this.reachXz);
        if (ctx.random().nextInt(3) == 0) {
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), TCSounds.BUBBLES.get(), SoundSource.PLAYERS, 0.5F, 1.0F);
        }
        level.sendParticles(ParticleTypes.EFFECT, anchor.getX(), anchor.getY(0.5), anchor.getZ(), 3, this.reachXz * 0.4, 0.5, this.reachXz * 0.4, 0.0);

        for (Projectile p : level.getEntitiesOfClass(Projectile.class, area, Entity::isAlive)) {
            if (p instanceof Snowball) {
                p.discard();
                FlurryShot.reforgeShard(level, caster, 1.0F + potency, primal ? 1 + potency : 0);
            } else if (p instanceof ThrownEgg egg) {
                Vec3 at = egg.position();
                egg.discard();
                Taint.spawnTaintSpider(level, at, egg.getYRot());
            } else if (p instanceof SmallFireball) {
                p.discard();
                Vec3 look = caster.getLookAngle();
                LargeFireball orb = new LargeFireball(level, caster,
                        new Vec3(look.x + ctx.random().nextGaussian() * 0.1, look.y, look.z + ctx.random().nextGaussian() * 0.1), 1 + potency + (primal ? 1 : 0));
                orb.setPos(caster.getX() + look.x, caster.getEyeY() - 0.1 + look.y, caster.getZ() + look.z);
                level.addFreshEntity(orb);
            } else if (p instanceof EntityEldritchOrb) {
                p.discard();
                eldritchRay(level, caster, 3.0F * (1 + potency));
            }
        }
    }

    private static void eldritchRay(ServerLevel level, LivingEntity caster, float damage) {
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(caster.getLookAngle().scale(32.0));
        HitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, caster, eye, end,
                new AABB(eye, end).inflate(1.0), e -> e instanceof LivingEntity && e.isAlive() && e != caster);
        if (hit != null) {
            end = hit.getLocation();
            hit.getEntity().hurt(level.damageSources().indirectMagic(caster, caster), damage);
        }
        Vec3 step = end.subtract(eye);
        int n = (int) Math.max(4, step.length() * 2);
        for (int k = 1; k <= n; k++) {
            Vec3 at = eye.add(step.scale((double) k / n));
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }
}
