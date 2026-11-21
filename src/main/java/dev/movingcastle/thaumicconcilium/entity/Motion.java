package dev.movingcastle.thaumicconcilium.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Small motion helpers shared by several ported projectiles. */
public final class Motion {
    private Motion() {
    }

    /**
     * Sets an entity's velocity toward {@code point}, capped at unit length and scaled by {@code modifier}.
     * Mirrors Thaumic Tinkerer's {@code setEntityMotionFromVector}, which Concilium relied on.
     */
    public static void pullTowards(Entity entity, Vec3 point, float modifier) {
        Vec3 delta = point.subtract(entity.getBoundingBox().getCenter());
        if (delta.length() > 1.0) {
            delta = delta.normalize();
        }
        setVelocity(entity, delta.scale(modifier));
    }

    /** Sets velocity and makes sure the change reaches clients (needed for players and most mobs). */
    public static void setVelocity(Entity entity, Vec3 velocity) {
        entity.setDeltaMovement(velocity);
        entity.hurtMarked = true;
    }
}
