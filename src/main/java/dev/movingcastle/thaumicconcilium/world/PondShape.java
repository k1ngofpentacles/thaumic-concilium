package dev.movingcastle.thaumicconcilium.world;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntBinaryOperator;

/**
 * Phase 5d cycle 4d: which 13 columns the grove's mana pond takes and at what height it stands (user, 2026-11-22: a rough circle, always
 * level, read from the crater floor so the pool lies in the ground, 13 sources). A pure rule over the crater's cut depths, so it is the same
 * on every run and can be tested without a world.
 *
 * <p>The rule. The pool starts on the deepest column next to the centre (the centre itself in every crater: the bowl is deepest there) and
 * grows one column at a time, always taking the cheapest column that touches it by a side:
 * <pre>cost = {@value #SHALLOW} x (layers shallower than the start)
 *      + {@value #ROUND} x (distance from the middle of the pool as it stands, squared)
 *      + {@value #ROUGH} x (a seeded number in 0..1)
 *      - {@value #COVE} if three or four of its sides already touch the pool
 *      + {@value #CORNER} if it would be a corner of the pool's bounding box
 *      + {@value #LONG} if taking it would leave the pool more than one column longer than it is wide</pre>
 * Depth pulls the pool toward the lowest ground, the distance term keeps it round about its own middle (so it may sit a column off the
 * centre where the floor is lower), the seeded number makes the outline irregular, the cove term closes coves and holes, the corner term
 * is what makes it a circle and not a square (13 columns fit a 4 by 4 box with three to spare: without it the pool is a square with a
 * bite out), and the last keeps it from becoming a bar. The usual result is rows of 2, 4, 4, 2 with one more column as a bump, in a
 * 4 by 5 box with no corner taken. The pool's level is the
 * floor height most of its columns share (the deeper one on a tie): the pond block takes the place of the floor block there, a deeper
 * column is filled up to it, a shallower one is cut down to it, and a neighbour column that would leave a side of the pool open is raised
 * to its level ({@code GrovePond.place}). So the surface is one height and sits one ninth of a block under the ground around it.
 */
public record PondShape(int level, List<int[]> cells) {
    public static final int SIZE = 13;
    public static final double SHALLOW = 1.25;
    /** The round-1 weights (a varied lozenge, usually 4 by 5). */
    public static final double ROUND = 0.5;
    public static final double ROUGH = 1.0;
    /**
     * Cycle 4e round 2 (user, 2026-11-23): shapes vary by crater seed but favour the round one. Per seed, one of three (ROUND, ROUGH) settings is
     * taken with the chances below: the roundest (a 4 by 4 box with three corners cut, about a third of craters), a middling one and the round-1
     * setting. As planned in {@code tools/plan_pond_mound.py} ({@code VARIETY}).
     */
    public static final double[][] VARIETY = {{0.33, 1.6, 0.35}, {0.34, 1.0, 0.8}, {0.33, ROUND, ROUGH}};
    public static final double COVE = 10.0;
    public static final double LONG = 100.0;
    public static final double CORNER = 3.0;
    /** Columns further than this from the start are never taken. */
    public static final int REACH = 4;

    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /**
     * @param cut  layers the crater cuts below the hex's level in a column (dx, dz from the centre)
     * @param seed the crater's seed
     * @return the pool: {@code level} is the cut depth of the pond blocks (their y is the centre's y minus it), {@code cells} the columns
     */
    public static PondShape choose(IntBinaryOperator cut, long seed) {
        double roll = PondMound.unit(9, 9, seed, 21);
        double acc = 0.0;
        double[] pick = VARIETY[VARIETY.length - 1];
        for (double[] v : VARIETY) {
            acc += v[0];
            if (roll < acc) {
                pick = v;
                break;
            }
        }
        return choose(cut, seed, pick[1], pick[2]);
    }

    /** The pool with the given ROUND and ROUGH weights (round 1 is {@code choose(cut, seed, ROUND, ROUGH)}). */
    public static PondShape choose(IntBinaryOperator cut, long seed, double round, double rough) {
        int sx = 0;
        int sz = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (cut.applyAsInt(dx, dz) > cut.applyAsInt(sx, sz)) {
                    sx = dx;
                    sz = dz;
                }
            }
        }
        int deep = cut.applyAsInt(sx, sz);
        List<int[]> cells = new ArrayList<>();
        Set<Long> in = new HashSet<>();
        cells.add(new int[] {sx, sz});
        in.add(key(sx, sz));
        int sumX = sx;
        int sumZ = sz;
        int minX = sx;
        int maxX = sx;
        int minZ = sz;
        int maxZ = sz;
        while (cells.size() < SIZE) {
            int[] best = null;
            double bestCost = Double.MAX_VALUE;
            // every column beside the pool, in a fixed order so that equal costs always fall the same way
            for (int dx = sx - REACH; dx <= sx + REACH; dx++) {
                for (int dz = sz - REACH; dz <= sz + REACH; dz++) {
                    if (in.contains(key(dx, dz))) {
                        continue;
                    }
                    int touching = 0;
                    for (int[] s : SIDES) {
                        if (in.contains(key(dx + s[0], dz + s[1]))) {
                            touching++;
                        }
                    }
                    if (touching == 0) {
                        continue;
                    }
                    double ox = dx - sumX / (double) cells.size();   // from the middle of the pool as it stands
                    double oz = dz - sumZ / (double) cells.size();
                    int wide = Math.max(maxX, dx) - Math.min(minX, dx) + 1;
                    int deepZ = Math.max(maxZ, dz) - Math.min(minZ, dz) + 1;
                    double cost = SHALLOW * Math.max(0, deep - cut.applyAsInt(dx, dz)) + round * (ox * ox + oz * oz) + rough * unit(dx, dz, seed)
                            - (touching >= 3 ? COVE : 0.0) + (Math.abs(wide - deepZ) > 1 ? LONG : 0.0)
                            + (wide >= 3 && deepZ >= 3 && (dx <= minX || dx >= maxX) && (dz <= minZ || dz >= maxZ) ? CORNER : 0.0);
                    if (cost < bestCost) {
                        bestCost = cost;
                        best = new int[] {dx, dz};
                    }
                }
            }
            if (best == null) {
                break; // cannot happen: the reach holds 81 columns
            }
            cells.add(best);
            in.add(key(best[0], best[1]));
            sumX += best[0];
            sumZ += best[1];
            minX = Math.min(minX, best[0]);
            maxX = Math.max(maxX, best[0]);
            minZ = Math.min(minZ, best[1]);
            maxZ = Math.max(maxZ, best[1]);
        }
        // the level: the floor height most columns share, the deeper on a tie
        int level = deep;
        int most = 0;
        for (int c = deep; c >= 0; c--) {
            int n = 0;
            for (int[] cell : cells) {
                if (cut.applyAsInt(cell[0], cell[1]) == c) {
                    n++;
                }
            }
            if (n > most) {
                most = n;
                level = c;
            }
        }
        return new PondShape(level, List.copyOf(cells));
    }

    public boolean has(int dx, int dz) {
        for (int[] c : this.cells) {
            if (c[0] == dx && c[1] == dz) {
                return true;
            }
        }
        return false;
    }

    private static long key(int dx, int dz) {
        return ((long) dx << 32) ^ (dz & 0xFFFFFFFFL);
    }

    /** A seeded number in 0..1 for a column. */
    private static double unit(int dx, int dz, long seed) {
        long v = dx * 73856093L ^ dz * 19349663L ^ 0x9E3779B97F4A7C15L ^ seed;
        v = (v ^ (v >>> 13)) * 1274126177L;
        v = (v ^ (v >>> 29)) * 0xBF58476D1CE4E5B9L;
        return ((v ^ (v >>> 32)) & 0xFFFFL) / 65535.0;
    }
}
