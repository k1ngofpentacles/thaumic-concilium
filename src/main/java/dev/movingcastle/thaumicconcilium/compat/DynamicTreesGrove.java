package dev.movingcastle.thaumicconcilium.compat;

import com.dtteam.dynamictrees.api.registry.Registry;
import com.dtteam.dynamictrees.event.RegistryEvent;
import com.dtteam.dynamictrees.event.SeedVoluntaryPlantEvent;
import com.dtteam.dynamictrees.event.TransitionSaplingToTreeEvent;
import com.dtteam.dynamictrees.event.VoluntarySeedDropEvent;
import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKit;
import com.dtteam.dynamictrees.tree.species.Species;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.world.GroveColumns;
import dev.movingcastle.thaumicconcilium.world.GroveHill;
import dev.movingcastle.thaumicconcilium.world.GroveLife;
import dev.movingcastle.thaumicconcilium.world.LandmarkShapes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/**
 * 5d follow-up, the grove's own Dynamic Trees (user decisions 2026-11-23, {@code docs/design/5d-grove-trees.md}). The only class (with
 * {@link TemplateGrowthLogic}) that wires our species into Dynamic Trees; loaded only when Dynamic Trees is ({@link #init}).
 * <ul>
 * <li>Species classes by id: {@code thaumicconcilium:elder_*} heroes, {@code grove_*} the finale's lineage, {@code glade_*} regular grove trees
 * ({@link #kindOf}).</li>
 * <li>Templates: each tree's shape and facing come from its root's position ({@link #pick}), so the finale (which places them) and natural
 * growth ({@link TemplateGrowthLogic}) always agree. Heroes take the {@code elders} set, the rest the {@code grove} set.</li>
 * <li>Lineage: a finale tree's own seed drops are tagged; a tagged seed that plants itself becomes a {@code grove_} sapling (a descendant);
 * the tag is stripped when a player picks the seed up, so it stacks and replants as an ordinary Thaumaturge seed. A Thaumaturge
 * greatwood or silverwood sapling that turns into a tree in a grove column becomes {@code glade_} (cap 19).</li>
 * </ul>
 */
public final class DynamicTreesGrove {
    public static final String NS = ThaumicConcilium.MODID;
    private static final String LINEAGE = "thaumicconcilium_lineage";
    public static final TemplateGrowthLogic KIT = new TemplateGrowthLogic(ResourceLocation.fromNamespaceAndPath(NS, "template"));

    /** Tests: saplings planted by lineage, Thaumaturge saplings turned glade. */
    public static long descendantsPlanted;
    public static long gladeConversions;

    private DynamicTreesGrove() {
    }

    /** Wires the growth logic kit (mod bus, before Dynamic Trees builds its registries) and the lineage events. */
    @SuppressWarnings("unchecked")
    public static void init(IEventBus modBus) {
        modBus.addListener(RegistryEvent.class, e -> {
            if (e.isEntryOfType(GrowthLogicKit.class)) {
                ((Registry<GrowthLogicKit>) e.getRegistry()).register(KIT);
            }
        });
        NeoForge.EVENT_BUS.addListener(DynamicTreesGrove::onSeedDrop);
        NeoForge.EVENT_BUS.addListener(DynamicTreesGrove::onSeedPlant);
        NeoForge.EVENT_BUS.addListener(DynamicTreesGrove::onPickup);
        NeoForge.EVENT_BUS.addListener(DynamicTreesGrove::onTransition);
    }

    // ---------------------------------------------------------------- species classes
    @Nullable
    public static GroveHill.Kind kindOf(Species species) {
        ResourceLocation id = species.getRegistryName();
        if (id == null || !NS.equals(id.getNamespace())) {
            return null;
        }
        String p = id.getPath();
        return p.startsWith("elder_") ? GroveHill.Kind.HERO : p.startsWith("grove_") ? GroveHill.Kind.FINALE : p.startsWith("glade_") ? GroveHill.Kind.GLADE : null;
    }

    public static boolean silver(Species species) {
        ResourceLocation id = species.getRegistryName();
        return id != null && id.getPath().endsWith("silverwood");
    }

    public static ResourceLocation speciesId(GroveHill.Kind kind, boolean silver) {
        String prefix = switch (kind) {
            case HERO -> "elder_";
            case FINALE -> "grove_";
            case GLADE -> "glade_";
        };
        return ResourceLocation.fromNamespaceAndPath(NS, prefix + (silver ? "silverwood" : "greatwood"));
    }

    public static Species species(GroveHill.Kind kind, boolean silver) {
        return Species.REGISTRY.get(speciesId(kind, silver));
    }

    /** Plants this class's own sapling (an air block on suitable soil); false when it cannot stand there. */
    public static boolean plantSapling(net.minecraft.world.level.Level level, BlockPos at, GroveHill.Kind kind, boolean silver) {
        Species sp = species(kind, silver);
        return sp.isValid() && sp.plantSapling(level, at, false);
    }

    // ---------------------------------------------------------------- templates
    /** A tree's template: its shape code and facing, a pure function of the root (soil) column and the class. */
    public record Pick(String code, Direction facing, int trunkCap) {
        public Pick(String code, Direction facing) {
            this(code, facing, 13);
        }
    }

    @Nullable
    public static Pick pick(GroveHill.Kind kind, boolean silver, int x, int z) {
        List<LandmarkShapes.Shape> list = LandmarkShapes.shapes(kind == GroveHill.Kind.HERO ? "elders" : "grove", silver);
        if (list.isEmpty()) {
            return null;
        }
        int v = Math.min(list.size() - 1, (int) (GroveLife.roll(0x7E3E1DL, x, z, 61) * list.size()));
        int f = Math.min(3, (int) (GroveLife.roll(0x7E3E1DL, x, z, 62) * 4));
        return new Pick(list.get(v).code(), Direction.from2DDataValue(f), list.get(v).trunkCap());
    }

    /** DT's JoCode direction maps ({@code JoCode.dirmap}), indexed by the facing's 3D value with west and east swapped, as {@code setFacing} does. */
    private static final byte[][] DIRMAP = {
            {0, 1, 2, 3, 4, 5, 6, 7}, {0, 1, 2, 3, 4, 5, 6, 7}, {0, 1, 2, 3, 4, 5, 6, 7},
            {0, 1, 3, 2, 5, 4, 6, 7}, {0, 1, 5, 4, 2, 3, 6, 7}, {0, 1, 4, 5, 3, 2, 6, 7}};

    /** A template's cells relative to the soil, rotated as generation rotates them. */
    public static final class Template {
        /** Cell (packed relative BlockPos) to its index. */
        public final Map<Long, Integer> index = new HashMap<>();
        public final List<BlockPos> cells = new ArrayList<>();
        public final List<List<Integer>> children = new ArrayList<>();
        public int maxDepth;
        /** Per cell: its parent's index (-1 for the first cell) and its path length from the soil. */
        public int[] parent = new int[0];
        public int[] depth = new int[0];
        private final Map<Integer, int[]> sizesByLimit = new ConcurrentHashMap<>();

        /** Per cell, how many cells of its subtree stand at or under {@code limitY} (the subtree is cut where a cell passes it). */
        public int[] sizes(int limitY) {
            return this.sizesByLimit.computeIfAbsent(limitY, l -> {
                int[] s = new int[this.cells.size()];
                for (int i = this.cells.size() - 1; i >= 0; i--) { // children always come after their parent
                    if (this.cells.get(i).getY() > l) {
                        continue;
                    }
                    s[i] = 1;
                    for (int c : this.children.get(i)) {
                        s[i] += s[c];
                    }
                }
                return s;
            });
        }
    }

    private static final Map<String, Template> TEMPLATES = new ConcurrentHashMap<>();

    public static Template template(Pick pick) {
        return TEMPLATES.computeIfAbsent(pick.code() + "@" + pick.facing().get3DDataValue(), k -> build(pick)); // the cells do not depend on the trunk cap
    }

    private static Template build(Pick pick) {
        byte[] ins = LandmarkShapes.decode(pick.code());
        int f = pick.facing().get3DDataValue();
        byte[] map = DIRMAP[f == 4 ? 5 : f == 5 ? 4 : f];
        Template t = new Template();
        java.util.ArrayDeque<int[]> stack = new java.util.ArrayDeque<>(); // {x, y, z, parent, depth}
        List<Integer> parents = new ArrayList<>();
        List<Integer> depths = new ArrayList<>();
        int x = 0, y = 0, z = 0, parent = -1, depth = 0;
        for (byte b : ins) {
            if (b == 6) {
                stack.push(new int[] {x, y, z, parent, depth});
            } else if (b == 7) {
                if (!stack.isEmpty()) {
                    int[] s = stack.pop();
                    x = s[0]; y = s[1]; z = s[2]; parent = s[3]; depth = s[4];
                }
            } else {
                Direction d = Direction.from3DDataValue(map[b]);
                x += d.getStepX();
                y += d.getStepY();
                z += d.getStepZ();
                depth++;
                BlockPos p = new BlockPos(x, y, z);
                int i = t.cells.size();
                t.cells.add(p);
                t.children.add(new ArrayList<>());
                t.index.put(p.asLong(), i);
                if (parent >= 0) {
                    t.children.get(parent).add(i);
                }
                parents.add(parent);
                depths.add(depth);
                parent = i;
                t.maxDepth = Math.max(t.maxDepth, depth);
            }
        }
        t.parent = parents.stream().mapToInt(Integer::intValue).toArray();
        t.depth = depths.stream().mapToInt(Integer::intValue).toArray();
        return t;
    }

    // ---------------------------------------------------------------- lineage
    private static boolean tagged(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(LINEAGE);
    }

    private static void onSeedDrop(VoluntarySeedDropEvent e) {
        if (kindOf(e.getSpecies()) != GroveHill.Kind.FINALE) {
            return;
        }
        String id = speciesId(GroveHill.Kind.FINALE, silver(e.getSpecies())).toString();
        for (ItemStack stack : e.getDropList()) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(LINEAGE, id));
        }
    }

    private static void onSeedPlant(SeedVoluntaryPlantEvent e) {
        ItemEntity item = e.getEntityItem();
        if (item == null || !tagged(item.getItem()) || !(item.level() instanceof ServerLevel level)) {
            return;
        }
        CustomData data = item.getItem().get(DataComponents.CUSTOM_DATA);
        Species descendant = Species.REGISTRY.get(ResourceLocation.parse(data.copyTag().getString(LINEAGE)));
        if (e.getWillPlant() && descendant.isValid()) {
            e.setCanceled(true); // DT would plant the seed's own (Thaumaturge) species: plant the lineage's instead
            if (descendant.plantSapling(level, e.getPos(), false)) {
                descendantsPlanted++;
            }
        }
    }

    private static void onPickup(ItemEntityPickupEvent.Pre e) {
        ItemStack stack = e.getItemEntity().getItem();
        if (tagged(stack)) {
            CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
            tag.remove(LINEAGE);
            if (tag.isEmpty()) {
                stack.remove(DataComponents.CUSTOM_DATA);
            } else {
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
        }
    }

    private static void onTransition(TransitionSaplingToTreeEvent e) {
        ResourceLocation id = e.getSpecies().getRegistryName();
        if (id == null || !"thaumaturge".equals(id.getNamespace()) || !(e.getLevel() instanceof ServerLevel level)
                || !GroveColumns.isGrove(level, e.getPos())) {
            return;
        }
        boolean silver = "silverwood".equals(id.getPath());
        if (!silver && !"greatwood".equals(id.getPath())) {
            return;
        }
        Species glade = species(GroveHill.Kind.GLADE, silver);
        if (!glade.isValid()) {
            return;
        }
        e.setCanceled(true);
        if (glade.transitionToTree(level, e.getPos())) {
            gladeConversions++;
        }
    }
}
