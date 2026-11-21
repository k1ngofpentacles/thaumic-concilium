package dev.movingcastle.thaumicconcilium.client;

import dev.movingcastle.thaumicconcilium.network.QuicksilverFlowPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Client side of {@link QuicksilverFlowPayload}: a line of mercury-grey dust from the crucible to the reactor. */
public final class QuicksilverFlowFx {
    private QuicksilverFlowFx() {
    }

    public static void spawn(QuicksilverFlowPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Vec3 from = Vec3.atCenterOf(payload.crucible()).add(0.0, 0.5, 0.0);
        Vec3 to = Vec3.atCenterOf(payload.reactor()).add(0.0, 0.5, 0.0);
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(0.7F, 0.7F, 0.75F), 1.0F);
        int steps = Math.max(6, (int) (from.distanceTo(to) * 4));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.lerp(to, i / (double) steps);
            level.addParticle(dust, p.x, p.y, p.z, 0.0, 0.0, 0.0);
        }
    }
}
