package dev.movingcastle.thaumicconcilium.client;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** The hammer's life drain (original: proxy.lifedrain): a short trail of crimson motes from the victim to the wielder. Client only, once a second. */
public final class LifeDrainFx {
    private static final DustParticleOptions DUST = new DustParticleOptions(new Vector3f(0.75F, 0.05F, 0.1F), 1.0F);

    private LifeDrainFx() {
    }

    public static void spawn(Level level, LivingEntity from, LivingEntity to) {
        Vec3 a = from.position().add(0.0, from.getBbHeight() / 2.0, 0.0);
        Vec3 b = to.position().add(0.0, to.getBbHeight() / 2.0, 0.0);
        for (int i = 0; i < 8; i++) {
            Vec3 p = a.lerp(b, i / 7.0);
            level.addParticle(DUST, p.x, p.y, p.z, 0.0, 0.02, 0.0);
        }
    }
}
