package dev.movingcastle.thaumicconcilium.entity;

/**
 * The Vessel's pure rules (phase 5c, memo sections 1 to 3 and 5): no Minecraft classes, so the pack test asserts them and the
 * server and the client's own pull read the same numbers. Distances are blocks, accelerations blocks per tick squared unless a
 * name says otherwise.
 */
public final class VesselPull {
    private VesselPull() {
    }

    /** accel(d) = base * (1 - d/range)^2 for d below range, else 0. */
    public static double accel(double distance, double base, double range) {
        if (distance >= range) {
            return 0.0;
        }
        double f = 1.0 - Math.max(0.0, distance) / range;
        return base * f * f;
    }

    /** The personal factor of a player (memo table); the first matching row wins in the memo's order of strength. */
    public static double playerFactor(boolean immune, boolean flung, boolean aboardShip, boolean tethered, boolean sheltered, boolean sneaking,
            double sneakFactor, double tetheredFactor) {
        if (immune || flung || aboardShip) {
            return 0.0;
        }
        if (tethered) {
            return tetheredFactor; // the anchor gives no shelter while drawing
        }
        if (sheltered) {
            return 0.0;
        }
        return sneaking ? sneakFactor : 1.0;
    }

    /** The factor of one of the Vessel's own swarms by its age in ticks. */
    public static double swarmFactor(long age, int oldAge, double young, double old) {
        return age >= oldAge ? old : young;
    }

    /** Start Mass in whole points for a number of players (at least one): 8 plus 2 each extra, capped. */
    public static int startMass(int players, int base, int perExtra, int cap) {
        return Math.min(cap, base + perExtra * (Math.max(1, players) - 1));
    }

    /** Swarms a Spin-up wave makes: one per player (at least one), never more than the cap allows alive. */
    public static int swarmsToSpawn(int players, int alive, int max) {
        return Math.max(0, Math.min(Math.max(1, players), max - alive));
    }

    /** The phase for a mass in quarter points: 0 Feeding, 1 Spin-up, 2 Collapse (at Mass 0 the fight is over). */
    public static int phase(int quarters, double spinUpAt, double collapseAt) {
        if (quarters <= collapseAt * 4.0 + 1e-9) {
            return 2;
        }
        return quarters <= spinUpAt * 4.0 + 1e-9 ? 1 : 0;
    }

    /** Mass after eating {@code units} quarters: capped by the window's remaining allowance and by start + overflow. Returns the gain in quarters. */
    public static int gain(int mass, int start, int units, int gainedInWindow, int windowCapQuarters, int overflowQuarters) {
        int room = Math.min(windowCapQuarters - gainedInWindow, start + overflowQuarters - mass);
        return Math.max(0, Math.min(units, room));
    }

    /** The disc plane's unit normal at a game time: tilted from vertical, turning once per {@code precession} ticks. Returns {x, y, z}. */
    public static double[] discNormal(long gameTime, double tiltDegrees, int precession) {
        double tilt = Math.toRadians(tiltDegrees);
        double phi = 2.0 * Math.PI * ((gameTime % precession) / (double) precession);
        return new double[] {Math.sin(tilt) * Math.cos(phi), Math.cos(tilt), Math.sin(tilt) * Math.sin(phi)};
    }

    /**
     * Whether a point offset (dx, dy, dz) from the hole's centre touches the disc: its radius within the plane lies in [inner, outer]
     * and its distance from the plane is within {@code thickness}. The renderer calls this too.
     */
    public static boolean inDisc(double dx, double dy, double dz, double thickness, long gameTime, double tiltDegrees, int precession,
            double inner, double outer) {
        double[] n = discNormal(gameTime, tiltDegrees, precession);
        double h = dx * n[0] + dy * n[1] + dz * n[2];
        double px = dx - h * n[0];
        double py = dy - h * n[1];
        double pz = dz - h * n[2];
        double r = Math.sqrt(px * px + py * py + pz * pz);
        return Math.abs(h) <= thickness && r >= inner && r <= outer;
    }

    /** Damage at the horizon: min(max, health - 2), never negative. */
    public static float horizonDamage(float health, float max) {
        return Math.max(0.0F, Math.min(max, health - 2.0F));
    }

    /**
     * Sable (2b): the impulse magnitude on a ship (mass times velocity change, capped), zero out of range. {@code dv} per tick is
     * {@code accel * (1 - d/range)^2 * pullScale} in blocks per second.
     */
    public static double shipImpulse(double mass, double distance, double range, double accelBlocksPerSecond, double pullScale, double cap) {
        if (distance >= range) {
            return 0.0;
        }
        double f = 1.0 - Math.max(0.0, distance) / range;
        return Math.min(mass * accelBlocksPerSecond * f * f * pullScale, cap);
    }

    /** Whether a ship should be pushed this tick: not already approaching the hole at the cap. */
    public static boolean shipNeedsPush(double approachSpeed, double approachCap) {
        return approachSpeed < approachCap;
    }

    /** Whether the twist (impulse at a point) is allowed: below the angular-speed cap; above it the push goes through the centre of mass. */
    public static boolean twistAllowed(double angularSpeed, double angularCap) {
        return angularSpeed < angularCap;
    }
}
