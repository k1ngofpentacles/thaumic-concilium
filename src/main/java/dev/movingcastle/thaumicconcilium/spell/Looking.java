package dev.movingcastle.thaumicconcilium.spell;

import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** The entity a creature is looking at, like TC4's {@code EntityUtils.getPointedEntity}. */
public final class Looking {
    private Looking() {
    }

    @Nullable
    public static Entity at(LivingEntity viewer, double range, double padding) {
        return at(viewer, range, padding, e -> e.isPickable() && e.isAlive());
    }

    /**
     * Nearest entity matching {@code filter} whose box, grown by {@code padding}, crosses the view ray within
     * {@code range} blocks (stopping at the first solid block).
     */
    @Nullable
    public static Entity at(LivingEntity viewer, double range, double padding, Predicate<Entity> filter) {
        Vec3 eye = viewer.getEyePosition();
        Vec3 look = viewer.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        BlockHitResult block = viewer.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }
        AABB sweep = viewer.getBoundingBox().expandTowards(look.scale(range)).inflate(padding + 1.0);
        Entity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : viewer.level().getEntities(viewer, sweep, filter)) {
            AABB box = e.getBoundingBox().inflate(padding);
            if (box.contains(eye)) {
                return e;
            }
            var hit = box.clip(eye, end);
            if (hit.isPresent()) {
                double d = eye.distanceToSqr(hit.get());
                if (d < bestDist) {
                    bestDist = d;
                    best = e;
                }
            }
        }
        return best;
    }
}
