package dev.movingcastle.thaumicconcilium.world;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntBinaryOperator;

/**
 * Phase 5d cycle 4e, round 2 (user, 2026-11-23, approved in the browser preview {@code tools/preview/mound.html}): the low outcrop of Botania
 * livingrock the grove's mana pond is recessed into. The mound stands {@value #TOP} block above the floor block at the pool (the pool's bed is
 * that floor block, the water lies in the top layer); its rim is wider and uneven (mostly 2 to 3 blocks) with a weathered contour of
 * livingrock slabs and stairs, and moss and cracks creep up from the grass: mossy and cracked livingrock bricks, moss carpet, moss blocks and
 * grass tongues are densest where the stone meets the grass and thin toward the water, none within {@value #FAR} of it. A pure rule over the
 * pool's shape, the crater's cut depths and the crater's seed: the same numbers as {@code tools/plan_pond_mound.py} (round 2), where it was
 * planned and proved.
 *
 * <p>Heights are layers above the floor block at the pool ({@code F}); {@code d} is the distance from a column to the nearest pool column and
 * {@code t = edge - d} how far inside the stone's edge it lies:
 * <ul>
 * <li>the eight neighbours of every pool column ({@code d < 1.5}) are plain rock to the top layer: they hold the water in, always;</li>
 * <li>the rim's width by direction is {@value #W_MIN} to {@value #W_MAX} (five seeded sines), moved by slow lumps and a per-column wobble;</li>
 * <li>{@code 0 < t <= }{@value #T_LOW}: the lower slope: grass tongue, stair (low side outward), slab or rock;</li>
 * <li>{@code t > }{@value #T_LOW}: rock; full rock columns {@value #FAR} or more from the water become mossy or cracked bricks with a chance
 * that falls with {@code t}, and may carry moss carpet;</li>
 * <li>{@code -}{@value #APRON}{@code  < t <= 0}: the apron on the grass: moss blocks replace its top block, flora grows on it.</li>
 * </ul>
 */
public final class PondMound {
    private PondMound() {
    }

    /** The mound's top layer above the floor block at the pool. The bed is the floor block; the pond block lies in this layer. */
    public static final int TOP = 1;
    public static final double W_MIN = 2.0;
    public static final double W_MAX = 3.9;
    public static final double JITTER = 0.55;
    public static final double LUMP = 0.5;
    public static final double T_LOW = 1.4;
    public static final double APRON = 1.6;
    public static final double FAR = 2.0;
    public static final double P_TURF = 0.30;
    public static final double P_STAIR = 0.28;
    public static final double P_SLAB = 0.28;
    public static final double P_BRICK = 0.62;
    public static final double BRICK_DECAY = 1.0;
    public static final double P_MOSSY_OF_BRICK = 0.62;
    public static final double P_CARPET = 0.45;
    public static final double CARPET_DECAY = 1.5;
    public static final double P_APRON_MOSS = 0.34;
    public static final double P_APRON_FLORA = 0.30;
    public static final double P_TURF_FLORA = 0.75;
    /** Columns further than this from the crater's centre are never part of it. */
    public static final int REACH = 10;

    public enum Kind {
        /** Livingrock. */
        ROCK,
        MOSSY,
        CRACKED,
        SLAB,
        /** A livingrock stair, low side {@link Cell#low()}. */
        STAIR,
        /** A grass block at the mound's height (a tongue of turf). */
        TURF,
        /** A moss block replacing the ground's top block, beside the stone. */
        MOSS,
        /** Nothing changes but a small plant on the ground. */
        FLORA
    }

    /**
     * One column of the mound. {@code top}: the layer of its top block above the pool's floor block ({@link #TOP}; 0 for MOSS and FLORA, which sit
     * on the ground). {@code added}: blocks above the column's own ground (0 = the ground's top block itself is replaced). {@code touches}: one
     * of the eight neighbours of a pool column. {@code carpet}: moss carpet on its top. {@code flora}: a small plant on top. {@code low}: for a
     * stair, the compass letter (e, w, s, n; +x east, +z south) its low step points to, away from the pool; the block's facing is the opposite.
     * {@code d}: distance to the nearest pool column; {@code t}: how far inside the stone's edge.
     */
    public record Cell(int dx, int dz, int top, Kind kind, int added, boolean touches, boolean carpet, boolean flora, char low, double d, double t) {
        /** A column that is a block of the mound itself (not an apron moss block or a flora-only column). */
        public boolean structural() {
            return this.kind != Kind.MOSS && this.kind != Kind.FLORA;
        }
    }

    public static List<Cell> plan(IntBinaryOperator cut, long seed, PondShape pool) {
        double mx = 0.0;
        double mz = 0.0;
        for (int[] c : pool.cells()) {
            mx += c[0];
            mz += c[1];
        }
        mx /= pool.cells().size();
        mz /= pool.cells().size();
        double p1 = unit(1, 0, seed, 11) * 6.283;
        double p2 = unit(2, 0, seed, 11) * 6.283;
        double p3 = unit(3, 0, seed, 11) * 6.283;
        double p4 = unit(4, 0, seed, 11) * 6.283;
        double p5 = unit(5, 0, seed, 11) * 6.283;
        List<Cell> out = new ArrayList<>();
        for (int dx = -REACH; dx <= REACH; dx++) {
            for (int dz = -REACH; dz <= REACH; dz++) {
                if (pool.has(dx, dz)) {
                    continue;
                }
                double d = Double.MAX_VALUE;
                for (int[] c : pool.cells()) {
                    d = Math.min(d, Math.hypot(dx - c[0], dz - c[1]));
                }
                if (d > W_MAX + LUMP + JITTER + APRON + 0.1) {
                    continue;
                }
                double angle = Math.atan2(dz - mz, dx - mx);
                double n = 0.5 + 0.5 * (0.5 * Math.sin(2 * angle + p1) + 0.3 * Math.sin(3 * angle + p2) + 0.2 * Math.sin(5 * angle + p3));
                double w = W_MIN + (W_MAX - W_MIN) * n;
                double edge = w + LUMP * Math.sin(0.85 * dx + p4) * Math.sin(0.75 * dz + p5) + (unit(dx, dz, seed, 12) * 2.0 - 1.0) * JITTER;
                boolean touches = d < 1.5;
                double t = edge - d;
                int ground = pool.level() - cut.applyAsInt(dx, dz);
                int added = TOP - ground;
                boolean nearWater = d < FAR;
                if (touches) {
                    if (added >= 0) {
                        out.add(new Cell(dx, dz, TOP, Kind.ROCK, added, true, false, false, ' ', d, Math.max(t, 2.0)));
                    }
                    continue;
                }
                if (t <= 0.0) { // the apron: moss and flora creep off the stone onto the grass
                    if (t < -APRON || nearWater) {
                        continue;
                    }
                    double f = 1.0 + t / APRON;
                    if (unit(dx, dz, seed, 16) < P_APRON_MOSS * f) {
                        out.add(new Cell(dx, dz, 0, Kind.MOSS, 0, false, false, unit(dx, dz, seed, 18) < 0.25, ' ', d, t));
                    } else if (unit(dx, dz, seed, 17) < P_APRON_FLORA * f) {
                        out.add(new Cell(dx, dz, 0, Kind.FLORA, 0, false, false, true, ' ', d, t));
                    }
                    continue;
                }
                if (added < 0) {
                    continue; // the ground is already higher: the mound runs into the slope
                }
                Kind kind = Kind.ROCK;
                char low = ' ';
                boolean flora = false;
                boolean carpet = false;
                if (t <= T_LOW) { // the weathered lower slope
                    double u = unit(dx, dz, seed, 13);
                    double pt = P_TURF * (1.0 - 0.5 * t / T_LOW);
                    if (u < pt) {
                        kind = Kind.TURF;
                        flora = unit(dx, dz, seed, 19) < P_TURF_FLORA;
                    } else if (u < pt + P_STAIR) {
                        kind = Kind.STAIR;
                    } else if (u < pt + P_STAIR + P_SLAB) {
                        kind = Kind.SLAB;
                    }
                }
                if (kind == Kind.TURF && added == 0) {
                    continue; // turf where the ground is: nothing changes
                }
                if (kind == Kind.STAIR) {
                    double vx = dx - mx;
                    double vz = dz - mz;
                    low = Math.abs(vx) >= Math.abs(vz) ? (vx > 0 ? 'e' : 'w') : (vz > 0 ? 's' : 'n');
                }
                if (kind == Kind.ROCK && !nearWater && unit(dx, dz, seed, 14) < P_BRICK * Math.exp(-Math.max(t, 0.0) / BRICK_DECAY)) {
                    kind = unit(dx, dz, seed, 15) < P_MOSSY_OF_BRICK ? Kind.MOSSY : Kind.CRACKED;
                }
                if ((kind == Kind.ROCK || kind == Kind.MOSSY || kind == Kind.CRACKED) && !nearWater
                        && unit(dx, dz, seed, 20) < P_CARPET * Math.exp(-Math.max(t, 0.0) / CARPET_DECAY)) {
                    carpet = true;
                }
                out.add(new Cell(dx, dz, TOP, kind, added, false, carpet, flora, low, d, t));
            }
        }
        return List.copyOf(out);
    }

    /** A seeded number in 0..1 for a column and a purpose. */
    public static double unit(int dx, int dz, long seed, int salt) {
        long v = dx * 73856093L ^ dz * 19349663L ^ 0x9E3779B97F4A7C15L ^ seed ^ salt * 83492791L;
        v = (v ^ (v >>> 13)) * 1274126177L;
        v = (v ^ (v >>> 29)) * 0xBF58476D1CE4E5B9L;
        return ((v ^ (v >>> 32)) & 0xFFFFL) / 65535.0;
    }
}
