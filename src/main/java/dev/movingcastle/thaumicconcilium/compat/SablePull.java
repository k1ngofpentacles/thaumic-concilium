package dev.movingcastle.thaumicconcilium.compat;

import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.entity.VesselPull;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

/**
 * The Vessel's pull on Sable sub-levels (phase 5c, cycle 2b). The only class that names Sable (PolyForm Shield: compiled against,
 * never shipped); {@code Vessel} reaches it only when the mod is loaded.
 *
 * <p>Tidal twist (user decision U5 b): the impulse goes through the ship's nearest point to the hole, so the ship turns and may roll.
 * Impulse vectors are in the sub-level's local frame ({@code transformNormalInverse}); points are plot coordinates
 * ({@code transformPositionInverse}); units are blocks and seconds. Caps: impulse per ship per tick, approach speed toward the hole,
 * and an angular speed above which the twist is dropped (the push then goes through the centre of mass only).
 */
public final class SablePull {
    private SablePull() {
    }

    /** Whether an entity rides or stands on a sub-level (it goes with the ship and gets no direct pull). */
    public static boolean aboard(Entity e) {
        return Sable.HELPER.getTrackingOrVehicleSubLevel(e) != null;
    }

    /** Server sub-levels intersecting the box (cached by the caller for a second). */
    public static List<ServerSubLevel> query(ServerLevel level, AABB box) {
        List<ServerSubLevel> out = new ArrayList<>();
        for (SubLevel s : Sable.HELPER.getAllIntersecting(level, new BoundingBox3d(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ))) {
            if (s instanceof ServerSubLevel ss && !ss.isRemoved()) {
                out.add(ss);
            }
        }
        return out;
    }

    /** One tick of pull on every cached ship. */
    public static void pull(Vec3 hole, float scale, List<?> ships) {
        for (Object o : ships) {
            if (o instanceof ServerSubLevel ship && !ship.isRemoved()) {
                pullOne(hole, scale, ship);
            }
        }
    }

    /** Applies one tick of pull to a ship; returns the world impulse magnitude applied (0 if skipped). Public for the pack test. */
    public static double pullOne(Vec3 hole, float scale, ServerSubLevel ship) {
        MassData mass = ship.getMassTracker();
        if (mass == null || mass.isInvalid()) {
            return 0.0;
        }
        RigidBodyHandle handle = RigidBodyHandle.of(ship);
        if (handle == null || !handle.isValid()) {
            return 0.0;
        }
        Pose3d pose = ship.logicalPose();
        Vector3d com = pose.transformPosition(new Vector3d(mass.getCenterOfMass()));
        Vec3 centre = new Vec3(com.x, com.y, com.z);
        double d = centre.distanceTo(hole);
        double range = TCConfig.PULL_RANGE.get();
        double magnitude = VesselPull.shipImpulse(mass.getMass(), d, range, TCConfig.SABLE_ACCEL.get(), scale, TCConfig.SABLE_IMPULSE_CAP.get());
        if (magnitude <= 0.0 || d < 1.0e-6) {
            return 0.0;
        }
        Vec3 dir = hole.subtract(centre).scale(1.0 / d);
        Vector3d velocity = handle.getLinearVelocity(new Vector3d());
        double approach = velocity.x * dir.x + velocity.y * dir.y + velocity.z * dir.z;
        if (!VesselPull.shipNeedsPush(approach, TCConfig.SABLE_APPROACH_CAP.get())) {
            return 0.0;
        }
        Vector3d local = pose.transformNormalInverse(new Vector3d(dir.x * magnitude, dir.y * magnitude, dir.z * magnitude), new Vector3d());
        double spin = handle.getAngularVelocity(new Vector3d()).length();
        if (VesselPull.twistAllowed(spin, TCConfig.SABLE_ANGULAR_CAP.get())) {
            BoundingBox3dc box = ship.boundingBox();
            Vector3d nearest = new Vector3d(clamp(hole.x, box.minX(), box.maxX()), clamp(hole.y, box.minY(), box.maxY()),
                    clamp(hole.z, box.minZ(), box.maxZ()));
            handle.applyImpulseAtPoint(pose.transformPositionInverse(nearest, new Vector3d()), local);
        } else {
            handle.applyLinearImpulse(local);
        }
        return magnitude;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
