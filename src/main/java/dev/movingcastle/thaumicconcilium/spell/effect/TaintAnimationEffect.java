package dev.movingcastle.thaumicconcilium.spell.effect;

import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.leclowndu93150.thaumaturge.content.entity.EntityMindSpider;
import com.leclowndu93150.thaumaturge.content.entity.EntityTaintSwarm;
import com.leclowndu93150.thaumaturge.content.entity.EntityTaintacle;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.content.taint.block.BlockTaintFibre;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.thaumicconcilium.registry.TCSpellBehaviors;
import dev.movingcastle.thaumicconcilium.spell.Looking;
import dev.movingcastle.thaumicconcilium.spell.Taint;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Taint Animation: the corrupted lands bend to the caster's will.
 * <ul>
 *   <li>mode 0, <b>as the original</b>: animates, or commands while the caster sneaks.</li>
 *   <li>mode 1, <b>animate</b>: picks one random flux-gas, taint-fibre or flux-goo block within
 *   {@code 4 + 2 * reach} blocks of the target point and stirs it. Fibre rises as a taintacle and gas gathers into a
 *   swarm, both turned on whatever the caster is looking at. With <b>flux vaporization</b> goo boils into gas
 *   (ready to become a swarm on a later pulse). With <b>thought manifestation</b> a nearby mind spider is first
 *   twisted into a taint spider.</li>
 *   <li>mode 2, <b>command</b>: every tainted creature within 16 blocks of the caster attacks the creature the spell
 *   lands on, or, failing that, the one the caster is looking at. Potency feeds them strength scaled by the
 *   caster's permanent warp.</li>
 * </ul>
 */
public final class TaintAnimationEffect extends AbstractEffectBehavior {
    public static final MapCodec<TaintAnimationEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.floatRange(1.0F, 64.0F).optionalFieldOf("command_range", 16.0F).forGetter(TaintAnimationEffect::commandRange),
            Codec.intRange(0, 255).optionalFieldOf("max_strength", 200).forGetter(TaintAnimationEffect::maxStrength)
    ).apply(i, TaintAnimationEffect::new));

    private static final DustParticleOptions SPARKLE = new DustParticleOptions(new Vector3f(0.86F, 0.5F, 1.0F), 1.0F);

    private final float commandRange;
    private final int maxStrength;
    private static final int PERIOD = 10;
    private static final java.util.Map<java.util.UUID, Long> LAST = new java.util.HashMap<>();

    public TaintAnimationEffect(float commandRange, int maxStrength) {
        this.commandRange = commandRange;
        this.maxStrength = maxStrength;
    }

    public float commandRange() {
        return this.commandRange;
    }

    public int maxStrength() {
        return this.maxStrength;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpellBehaviors.TAINT_ANIMATION.get();
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
        // The original acted every 10 ticks of channelling, whatever the pulse rate.
        long now = ctx.level().getGameTime();
        Long last = LAST.get(caster.getUUID());
        if (last != null && now - last < PERIOD && now >= last) {
            return;
        }
        LAST.put(caster.getUUID(), now);
        int mode = ctx.setting("mode");
        if (mode == 2 || (mode == 0 && caster.isShiftKeyDown())) {
            this.command(ctx, caster, target);
        } else {
            this.animate(ctx, caster, target);
        }
    }

    private void command(CastContext ctx, LivingEntity caster, SpellTarget target) {
        LivingEntity victim = target.entity().orElse(null) instanceof LivingEntity l && l != caster ? l
                : Looking.at(caster, 32.0, 0.2) instanceof LivingEntity seen ? seen : null;
        if (victim == null || Taint.isTainted(victim)) {
            return;
        }
        int potency = ctx.setting("potency");
        int strength = -1;
        if (potency > 0 && caster instanceof Player player) {
            int warp = WarpHelper.getWarp(player).get(WarpType.PERMANENT);
            // The original: clamp(permanent warp / 10 * potency, 1, 200).
            strength = Mth.clamp(warp / 10 * potency, 1, this.maxStrength);
        }
        for (Mob mob : ctx.level().getEntitiesOfClass(Mob.class, caster.getBoundingBox().inflate(this.commandRange),
                m -> m.isAlive() && m != victim && Taint.isTainted(m))) {
            mob.setTarget(victim);
            if (strength > 0) {
                mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, strength));
            }
        }
    }

    private void animate(CastContext ctx, LivingEntity caster, SpellTarget target) {
        ServerLevel level = ctx.level();
        if (ctx.setting("manifestation") == 1) {
            List<EntityMindSpider> spiders = level.getEntitiesOfClass(EntityMindSpider.class, caster.getBoundingBox().inflate(16.0));
            if (!spiders.isEmpty()) {
                EntityMindSpider chosen = spiders.get(ctx.random().nextInt(spiders.size()));
                Vec3 at = chosen.position();
                float yaw = chosen.getYRot();
                chosen.discard();
                // The upgrade's research: mind spiders "turned into fearsome tainted crawlers" (Concilium's smart
                // taint spider in code; Thaumaturge's taint crawler here until the phase-4 mobs).
                Mob crawler = TTEntities.TAINT_CRAWLER.get().create(level);
                if (crawler != null) {
                    crawler.moveTo(at.x, at.y, at.z, yaw, 0.0F);
                    level.addFreshEntity(crawler);
                    Taint.taintsplosion(level, at);
                }
            }
        }

        int distance = 4 + 2 * ctx.setting("reach");
        BlockPos centre = BlockPos.containing(target.position());
        boolean vaporize = ctx.setting("vaporization") == 1;
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-distance, -distance, -distance), centre.offset(distance, distance, distance))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(TTBlocks.FLUX_GAS.get()) || state.is(TTBlocks.TAINT_FIBRE.get()) || state.is(TTBlocks.FLUX_GOO.get())) {
                found.add(pos.immutable());
            }
        }
        if (found.isEmpty()) {
            return;
        }
        BlockPos pos = found.get(ctx.random().nextInt(found.size()));
        BlockState state = level.getBlockState(pos);
        if (state.is(TTBlocks.TAINT_FIBRE.get())) {
            // The original wore the fibre down a growth stage, removing it only when none was left.
            if (state.getValue(BlockTaintFibre.GROWTH2)) {
                level.setBlock(pos, state.setValue(BlockTaintFibre.GROWTH2, false), 3);
            } else if (state.getValue(BlockTaintFibre.GROWTH1)) {
                level.setBlock(pos, state.setValue(BlockTaintFibre.GROWTH1, false), 3);
            } else {
                level.removeBlock(pos, false);
            }
            EntityTaintacle taintacle = TTEntities.TAINTACLE.get().create(level);
            if (taintacle != null) {
                taintacle.moveTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ctx.random().nextFloat() * 360.0F, 0.0F);
                aim(taintacle, caster);
                level.addFreshEntity(taintacle);
            }
            sparkle(level, pos);
        } else if (state.is(TTBlocks.FLUX_GAS.get())) {
            level.removeBlock(pos, false);
            EntityTaintSwarm swarm = TTEntities.TAINT_SWARM.get().create(level);
            if (swarm != null) {
                swarm.moveTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ctx.random().nextFloat() * 360.0F, 0.0F);
                swarm.setSummoned(true);
                aim(swarm, caster);
                level.addFreshEntity(swarm);
            }
            sparkle(level, pos);
        } else if (vaporize && state.is(TTBlocks.FLUX_GOO.get())) {
            level.setBlock(pos, TTBlocks.FLUX_GAS.get().defaultBlockState(), 3);
            sparkle(level, pos);
        }
    }

    private static void aim(Mob mob, LivingEntity caster) {
        if (Looking.at(caster, 32.0, 1.1) instanceof LivingEntity look && look != mob) {
            mob.setTarget(look);
        }
    }

    private static void sparkle(ServerLevel level, BlockPos pos) {
        level.sendParticles(SPARKLE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.0);
    }
}
