package dev.movingcastle.thaumicconcilium.aura;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.gadomancy.aura.effect.AuraEffect;
import dev.movingcastle.gadomancy.aura.effect.AuraEffectType;
import dev.movingcastle.thaumicconcilium.registry.TCAuraEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;

/**
 * Original superbia effect ({@code GadomancyIntegration.java:178-198}) with Thaumic Tinkerer's {@code MiscHelper.setEntityMotionFromVector}
 * ({@code :42-52}): every monster within 32 blocks of the master block gets a push of up to 1 block per tick.
 */
public record RepelEffect(Timing timing) implements AuraEffect {
    /**
     * User decision S1 (2026-11-27, `docs/design/phase10-decisions.md`): port as written. True = the original's vector, where the
     * pylon's coordinates are subtracted once too few, so the push points toward the world origin and depends on where the pylon
     * stands; false = straight away from the pylon.
     */
    public static final boolean FAITHFUL_VECTOR = true;

    public static final MapCodec<RepelEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Timing.CODEC.forGetter(RepelEffect::timing)).apply(i, RepelEffect::new));

    @Override
    public AuraEffectType<?> type() {
        return TCAuraEffects.REPEL.get();
    }

    @Override
    public boolean appliesTo(Entity entity) {
        return entity instanceof Monster;
    }

    @Override
    public void affectEntity(ServerLevel level, BlockPos origin, Entity entity) {
        double ox = origin.getX();
        double oy = origin.getY();
        double oz = origin.getZ();
        if (entity.distanceToSqr(ox, oy, oz) >= 1024.0) {
            return;
        }
        Vec3 target;
        if (FAITHFUL_VECTOR) {
            // the original: (x - ox, y, z - oz) scaled by (2, 1, 2), absolute coordinates leak in
            target = new Vec3((entity.getX() - ox) * 2.0, entity.getY(), (entity.getZ() - oz) * 2.0);
        } else {
            target = new Vec3(ox + 2.0 * (entity.getX() - ox), entity.getY(), oz + 2.0 * (entity.getZ() - oz));
        }
        Vec3 push = target.subtract(entity.getBoundingBox().getCenter());
        if (push.length() > 1.0) {
            push = push.normalize();
        }
        entity.setDeltaMovement(push);
        entity.hurtMarked = true;
    }
}
