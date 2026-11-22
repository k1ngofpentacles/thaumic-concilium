package dev.movingcastle.thaumicconcilium.entity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The Brightest One's shadow beam (original {@code Shadowbeam}, a throwable run to its end in one tick by
 * {@code updateUntilDead}): 300 steps of 0.04 blocks (about 12 blocks) from the shooter's eye along its look direction,
 * bouncing off blocks (the velocity is mirrored in the face's normal) and hurting every creature it passes through for
 * {@code 8 + potency} magic damage. A creature takes the damage once per hurt window, as in the original.
 */
public final class Shadowbeam {
    public static final int STEPS = 300;
    public static final double STEP = 0.04;
    /** How often a trail mote is drawn (every 24 steps, a block). */
    private static final int MOTE_EVERY = 24;
    private static final DustParticleOptions SHADOW = new DustParticleOptions(new Vector3f(0.25F, 0.0F, 0.4F), 1.6F);

    private Shadowbeam() {
    }

    /** Fires the beam; returns the creatures it hurt (for tests). */
    public static List<LivingEntity> fire(ServerLevel level, LivingEntity shooter, int potency) {
        Vec3 pos = shooter.getEyePosition().subtract(0.0, 0.1, 0.0);
        Vec3 motion = shooter.getLookAngle().normalize().scale(STEP);
        List<LivingEntity> hit = new ArrayList<>();
        for (int step = 0; step < STEPS; step++) {
            Vec3 next = pos.add(motion);
            BlockHitResult block = level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
            Vec3 end = block.getType() == HitResult.Type.MISS ? next : block.getLocation();
            AABB sweep = new AABB(pos, end).inflate(0.3);
            for (Entity e : level.getEntities(shooter, sweep, c -> c instanceof LivingEntity living && living.isAlive() && !living.isSpectator())) {
                if (e instanceof LivingEntity living && !hit.contains(living) && living.getBoundingBox().inflate(0.3).clip(pos, end).isPresent()) {
                    living.hurt(level.damageSources().indirectMagic(shooter, shooter), 8 + potency);
                    hit.add(living);
                }
            }
            if (block.getType() != HitResult.Type.MISS) {
                Direction face = block.getDirection();
                Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
                motion = motion.subtract(normal.scale(2.0 * motion.dot(normal)));
                pos = end.add(normal.scale(0.01));
            } else {
                pos = end;
            }
            if (step > 2 && step % MOTE_EVERY == 0) {
                level.sendParticles(SHADOW, pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.0);
            }
        }
        return hit;
    }
}
