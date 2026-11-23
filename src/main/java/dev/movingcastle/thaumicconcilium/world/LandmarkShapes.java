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
    public record Shape(String code, int cells, int tips, int widthX, int widthZ, int top, int lowLeaf, int firstLimb, int trunkTop) {
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
                    o.get("top").getAsInt(), o.get("lowLeaf").getAsInt(), o.get("firstLimb").getAsInt(), o.get("trunkTop").getAsInt()));
        }
    }

    /** All the shapes of a species (4 oak greatwoods, 3 gum silverwoods; empty when the data is missing). */
    public static List<Shape> shapes(boolean silver) {
        load();
        return silver ? silverwood : greatwood;
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
