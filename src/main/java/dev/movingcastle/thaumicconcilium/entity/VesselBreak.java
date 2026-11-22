package dev.movingcastle.thaumicconcilium.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The break cinematic's timeline (phase 5c cycle 3c): pure functions of the sequence's age in ticks, shared by the server (which
 * holds a Sable sub-level to this pose, see {@code compat/SableBreak}) and the client (which draws the stand-in hex, the cracks of
 * light and the flying shards from it). About six seconds: the hex rises into the Vessel, spins up, cracks with light, bursts, and
 * its four shards fly out along their spokes to become the anchors.
 */
public final class VesselBreak {
    /** The hex has risen to the Vessel's centre. */
    public static final int RISE_END = 40;
    /** Light starts to break out of it. */
    public static final int CRACK_START = 80;
    /** It bursts: the hex is gone and the shards leave. */
    public static final int BURST = 100;
    /** The shards land and the anchors form; the Vessel can be woken from here. */
    public static final int END = 130;
    /** How high a shard's arc stands above the straight line at its middle, in blocks. */
    public static final double ARC = 4.5;

    private VesselBreak() {
    }

    /** How far the hex has risen, 0..1 of the way to the Vessel's centre. */
    public static double rise(double age) {
        double t = Mth.clamp(age / RISE_END, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /** The hex's turn about the vertical, radians: still until tick 20, then the spin builds, faster again while it cracks. */
    public static double yaw(double age) {
        if (age <= 20.0) {
            return 0.0;
        }
        if (age <= CRACK_START) {
            return 0.35 * (age - 20.0) * (age - 20.0) / 120.0;
        }
        double d = age - CRACK_START;
        return 10.5 + 0.35 * d + 0.25 * d * d / 40.0;
    }

    /** A wobble about a horizontal axis, radians, growing once it is inside the Vessel. */
    public static double tilt(double age) {
        double grow = Mth.clamp((age - RISE_END) / 40.0, 0.0, 1.0);
        return 0.22 * grow * Math.sin(age * 0.19);
    }

    /** How far the light has broken out, 0 before the cracks, 1 at the burst. */
    public static float crack(double age) {
        return (float) Mth.clamp((age - CRACK_START) / (BURST - CRACK_START), 0.0, 1.0);
    }

    /**
     * The blast's strength at {@code distance} blocks from the centre of a crater of {@code radius}: 1 at the centre, falling like
     * an explosion's (k (k + 1) / 2 with k = 1 - distance / radius), nothing at and beyond the radius.
     */
    public static double blast(double distance, double radius) {
        if (radius <= 0.0 || distance >= radius) {
            return 0.0;
        }
        double k = 1.0 - Math.max(0.0, distance) / radius;
        return k * (k + 1.0) / 2.0;
    }

    /** The speed the blast throws something at (blocks per tick): outward from the centre and up, by its strength there. */
    public static Vec3 blastPush(Vec3 fromCentre, double radius, double knockback) {
        double d = Math.sqrt(fromCentre.x * fromCentre.x + fromCentre.z * fromCentre.z);
        double k = blast(d, radius);
        if (k <= 0.0) {
            return Vec3.ZERO;
        }
        double ox = d < 1.0e-4 ? 0.0 : fromCentre.x / d;
        double oz = d < 1.0e-4 ? 0.0 : fromCentre.z / d;
        return new Vec3(ox * knockback * k, 0.25 * knockback * (0.6 + k), oz * knockback * k);
    }

    /** A shard's flight, 0 at the burst, 1 on landing. */
    public static double flight(double age) {
        return Mth.clamp((age - BURST) / (END - BURST), 0.0, 1.0);
    }

    /** Where a shard is at flight fraction {@code t}: an arc from the Vessel's centre to its landing point, ending exactly on it. */
    public static Vec3 shardAt(Vec3 from, Vec3 to, double t) {
        return from.lerp(to, t).add(0.0, ARC * 4.0 * t * (1.0 - t), 0.0);
    }
}
