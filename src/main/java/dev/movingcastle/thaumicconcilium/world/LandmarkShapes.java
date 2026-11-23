package dev.movingcastle.thaumicconcilium.world;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;

/**
 * Phase 5d cycle 2d: the landmark tree shapes (veteran oak greatwoods, ghost gum silverwoods) as Dynamic Trees JoCode strings, from
 * {@code data/thaumicconcilium/grove_trees/landmarks.json} (written by {@code tools/gen_landmark_trees.py}). Names no Dynamic Trees class,
 * so it is safe to load without that mod. The codes are never registered with Dynamic Trees or Thaumaturge's own worldgen.
 */
public final class LandmarkShapes {
    private static final String RESOURCE = "/data/" + ThaumicConcilium.MODID + "/grove_trees/landmarks.json";

    /** One shape: its JoCode and what the generator measured (the tests compare the world with these). */
    public record Shape(String code, int cells, int tips, int widthX, int widthZ, int top, int lowLeaf, int firstLimb, int trunkTop, String family, int trunkCap) {
    }

    /** A choice for one landmark: which shape and which of the four facings (the same seed and spot always give the same). */
    public record Pick(int variant, Direction facing, Shape shape) {
    }

    private static List<Shape> greatwood;
    private static List<Shape> silverwood;

    private LandmarkShapes() {
    }

    private static synchronized void load() {
        if (greatwood != null) {
            return;
        }
        List<Shape> g = new ArrayList<>();
        List<Shape> s = new ArrayList<>();
        try (InputStream in = LandmarkShapes.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                ThaumicConcilium.LOG.warn("Landmark trees: {} is missing, the grove falls back to Thaumaturge's own trees", RESOURCE);
            } else {
                JsonObject root = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
                read(root.getAsJsonArray("greatwood"), g);
                read(root.getAsJsonArray("silverwood"), s);
            }
        } catch (Exception e) {
            ThaumicConcilium.LOG.warn("Landmark trees: could not read {}: {}", RESOURCE, e.toString());
        }
        silverwood = s;
        greatwood = g;
    }

    private static void read(JsonArray array, List<Shape> into) {
        if (array == null) {
            return;
        }
        for (JsonElement e : array) {
            JsonObject o = e.getAsJsonObject();
            into.add(new Shape(o.get("code").getAsString(), o.get("cells").getAsInt(), o.get("tips").getAsInt(), o.get("widthX").getAsInt(), o.get("widthZ").getAsInt(),
                    o.get("top").getAsInt(), o.get("lowLeaf").getAsInt(), o.get("firstLimb").getAsInt(), o.get("trunkTop").getAsInt(),
                    o.has("family") ? o.get("family").getAsString() : "", o.has("trunkCap") ? o.get("trunkCap").getAsInt() : 13));
        }
    }

    /** All the shapes of a species (8 oak greatwoods, 10 silverwoods in six families; each carries its own trunk cap; empty when the data is missing). */
    public static List<Shape> shapes(boolean silver) {
        load();
        return silver ? silverwood : greatwood;
    }

    // ---------------------------------------------------------------- stages (cycle 4e, memo section 2): pure, on the raw instruction bytes
    /** Stages a landmark grows in. */
    public static final int STAGES = 8;
    private static final String BASE_64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    /** Dynamic Trees' own decoding of a JoCode string (two 3-bit instructions to a character): 0-5 a step, 6 a fork, 7 a return. */
    public static byte[] decode(String code) {
        List<Byte> out = new ArrayList<>();
        for (int i = 0; i < code.length(); i++) {
            int six = BASE_64.indexOf(code.charAt(i));
            if (six != -1) {
                out.add((byte) (six >> 3));
                out.add((byte) (six & 7));
            }
        }
        byte[] array = new byte[out.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = out.get(i);
        }
        return array;
    }

    /** The longest path, in cells, from the root. */
    public static int maxDepth(byte[] code) {
        return walk(code, new int[1], 0, Integer.MAX_VALUE, null, true);
    }

    /** How many cells stage {@code stage} of {@link #STAGES} keeps: those whose path length from the root is at most {@link #limit}. */
    public static int limit(byte[] code, int stage) {
        int max = maxDepth(code);
        return stage >= STAGES ? max : (int) Math.ceil(stage * max / (double) STAGES);
    }

    /** The instructions of stage {@code stage} (1..8): the whole code cut to the cells of path length <= {@link #limit}; parent-closed, so every stage is a tree. */
    public static byte[] stage(byte[] code, int stage) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        walk(code, new int[1], 0, limit(code, stage), out, true);
        return out.toByteArray();
    }

    /** The cells (steps) an instruction stream places: the count of its step instructions. */
    public static int cells(byte[] code) {
        int n = 0;
        for (byte b : code) {
            if (b < 6) {
                n++;
            }
        }
        return n;
    }

    /** One level of the code: returns the deepest path seen. Writes what is kept to {@code out} (when not null) while {@code emit} holds. */
    private static int walk(byte[] in, int[] idx, int depth, int limit, java.io.ByteArrayOutputStream out, boolean emit) {
        int deepest = depth;
        while (idx[0] < in.length) {
            byte c = in[idx[0]];
            if (c == 6) {
                idx[0]++;
                boolean sub = emit && depth < limit;
                if (sub && out != null) {
                    out.write(6);
                }
                deepest = Math.max(deepest, walk(in, idx, depth, limit, out, sub));
                if (sub && out != null) {
                    out.write(7);
                }
            } else if (c == 7) {
                idx[0]++;
                return deepest;
            } else {
                idx[0]++;
                depth++;
                deepest = Math.max(deepest, depth);
                if (emit && depth <= limit) {
                    if (out != null) {
                        out.write(c);
                    }
                } else {
                    emit = false;
                }
            }
        }
        return deepest;
    }

    /** The shape and facing of the landmark planned at (x, z) of the grove with this crater seed; null when there are no shapes. */
    public static Pick pick(long seed, int x, int z, boolean silver) {
        List<Shape> list = shapes(silver);
        if (list.isEmpty()) {
            return null;
        }
        int variant = Math.min(list.size() - 1, (int) (GroveLife.roll(seed, x, z, 61) * list.size()));
        int facing = Math.min(3, (int) (GroveLife.roll(seed, x, z, 62) * 4));
        return new Pick(variant, Direction.from2DDataValue(facing), list.get(variant));
    }
}
