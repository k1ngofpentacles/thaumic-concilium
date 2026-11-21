package dev.movingcastle.thaumicconcilium.spell;

import com.leclowndu93150.thaumaturge.api.entity.ThaumaturgeEntityTypeTags;
import com.leclowndu93150.thaumaturge.api.entity.trait.MobTrait;
import com.leclowndu93150.thaumaturge.api.entity.trait.MobTraits;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.phys.Vec3;

/**
 * Thaumaturge taint glue. In Thaumaturge, tainted animals and spiders are ordinary vanilla mobs carrying the
 * {@code thaumaturge:tainted} trait; the dedicated taint creatures (swarms, taintacles, crawlers, seeds) are
 * tagged {@code thaumaturge:tainted} instead.
 */
public final class Taint {
    private Taint() {
    }

    public static boolean isTainted(LivingEntity mob) {
        return MobTraits.isTainted(mob);
    }

    /** True for mobs that are tainted only through the trait, i.e. a corrupted ordinary creature that can be cured. */
    public static boolean isCurable(LivingEntity mob) {
        return !mob.getType().is(ThaumaturgeEntityTypeTags.TAINTED) && MobTraits.has(mob, MobTraits.TAINTED);
    }

    public static boolean isEldritch(LivingEntity mob) {
        return mob.getType().is(ThaumaturgeEntityTypeTags.ELDRITCH);
    }

    public static Optional<Holder.Reference<MobTrait>> trait(ServerLevel level) {
        return level.registryAccess().registryOrThrow(MobTrait.REGISTRY_KEY).getHolder(MobTraits.TAINTED);
    }

    /** Removes the taint trait, returning the creature to its natural self. */
    public static boolean cure(ServerLevel level, LivingEntity mob) {
        return trait(level).map(t -> MobTraits.remove(mob, t)).orElse(false);
    }

    /** Spawns a tainted spider (the original's EntityTaintSpider) at {@code at}, with a taintsplosion puff. */
    public static Spider spawnTaintSpider(ServerLevel level, Vec3 at, float yaw) {
        Spider spider = EntityType.SPIDER.create(level);
        if (spider == null) {
            return null;
        }
        spider.moveTo(at.x, at.y, at.z, yaw, 0.0F);
        level.addFreshEntity(spider);
        trait(level).ifPresent(t -> MobTraits.add(spider, t));
        taintsplosion(level, at);
        return spider;
    }

    public static void taintsplosion(ServerLevel level, Vec3 at) {
        level.sendParticles(ParticleTypes.WITCH, at.x, at.y + 0.5, at.z, 24, 0.4, 0.4, 0.4, 0.05);
        level.sendParticles(ParticleTypes.SQUID_INK, at.x, at.y + 0.5, at.z, 8, 0.3, 0.3, 0.3, 0.02);
    }
}
