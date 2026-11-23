package dev.movingcastle.thaumicconcilium.entity;

import dev.movingcastle.thaumicconcilium.registry.TCBlocks;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The blast crater a breaking hex leaves (phase 5c cycle 3c, docs/design/5c-design.md "Crater and lesser-only"): a shallow bowl
 * round the hex, {@link #DEPTH} deep at the centre and tapering to nothing at a ragged rim, with a mound of debris raised on the
 * rim. Everything above the bowl's floor goes, up to {@link #CLEAR} blocks over the hex's level; destroyed blocks are erased (no
 * drops) except that block entities spill their contents; blocks as blast-proof as obsidian are spared.
 *
 * <p>Carved by a queue owned by the Vessel, strictly from the centre outward: a ring that widens {@link #SPEED} blocks a tick
 * from the burst, each column (bowl or mound) done when the ring reaches it, ties in distance taken in order of angle. Nothing
 * is touched ahead of the ring; where an anchor will stand is worked out beforehand ({@link #surfaceAfter}), not dug. The state
 * is the next column's index, the ring's age and a tally of what was broken (the mound is made of the commonest), all saved, so a
 * Vessel that loads with a crater unfinished finishes it at once.
 */
public final class VesselCrater {
    /** Blocks deep at the centre. */
    public static final double DEPTH = 4.0;
    /** How far above the hex's level the blast clears. */
    public static final int CLEAR = 10;
    /** How fast the ring spreads, blocks per tick: a 14-block crater is cut in about a second and a half, its rim's mound just after. */
    public static final double SPEED = 0.5;
    /** The rim swells out by up to this share of the radius, never in: the crater is at least its radius everywhere. */
    public static final double RAGGED = 0.12;
    /** The mound stands from this far inside the rim to this far outside it. */
    private static final double MOUND_IN = 0.5;
    public static final double MOUND_OUT = 2.2;

    private final BlockPos centre;
    private final double radius;
    private final long seed;
    private int index;
    private int age;
    private final Map<Block, Integer> debris = new HashMap<>();
    private List<int[]> columns;
    private List<Block> palette;

    public VesselCrater(BlockPos centre, double radius, long seed) {
        this.centre = centre;
        this.radius = radius;
        this.seed = seed;
    }

    public double radius() {
        return this.radius;
    }

    public long seed() {
        return this.seed;
    }

    public boolean done() {
        return this.index >= this.columns().size();
    }

    // ---------------------------------------------------------------- shape (pure)
    /** The rim's distance from the centre in a direction (radians): the radius, swollen by a seeded wobble. */
    public double rimAt(double angle) {
        double p1 = (this.seed & 1023L) / 1023.0 * Mth.TWO_PI;
        double p2 = ((this.seed >> 10) & 1023L) / 1023.0 * Mth.TWO_PI;
        double p3 = ((this.seed >> 20) & 1023L) / 1023.0 * Mth.TWO_PI;
        double n = 0.5 * Math.sin(3.0 * angle + p1) + 0.3 * Math.sin(5.0 * angle + p2) + 0.2 * Math.sin(9.0 * angle + p3);
        return this.radius * (1.0 + RAGGED * (0.5 + 0.5 * n));
    }

    private double hash(int dx, int dz, int salt) {
        long v = dx * 73856093L ^ dz * 19349663L ^ salt * 83492791L ^ this.seed;
        v = (v ^ (v >>> 13)) * 1274126177L;
        return ((v ^ (v >>> 16)) & 0xFFFFL) / 65535.0;
    }

    /** How many layers the bowl cuts below the hex's level in a column (0 at and beyond the rim, {@link #DEPTH} at the centre). */
    public int cut(int dx, int dz) {
        double r = Math.sqrt(dx * dx + dz * dz);
        double rim = this.rimAt(Math.atan2(dz, dx));
        if (r >= rim) {
            return 0;
        }
        double depth = DEPTH * (1.0 - (r / rim) * (r / rim));
        return (int) Math.round(Mth.clamp(depth + (r < 1.5 ? 0.0 : (this.hash(dx, dz, 1) - 0.5) * 0.8), 0.0, DEPTH));
    }

    /** Whether a column lies inside the rim (the bowl), and how high the mound stands on a column (0, 1 or 2; 0 inside the bowl proper). */
    public boolean inBowl(int dx, int dz) {
        return Math.sqrt(dx * dx + dz * dz) < this.rimAt(Math.atan2(dz, dx)) - MOUND_IN;
    }

    public int mound(int dx, int dz) {
        double off = Math.sqrt(dx * dx + dz * dz) - this.rimAt(Math.atan2(dz, dx));
        if (off < -MOUND_IN || off >= MOUND_OUT) {
            return 0;
        }
        double h = this.hash(dx, dz, 2);
        if (off >= 0.0 && off < 1.3) {
            return h < 0.6 ? 2 : 1; // the crest
        }
        return h < 0.75 ? 1 : 0;
    }

    /** Every column the crater touches, in the order it is carved: by distance from the centre, then by angle. */
    public List<int[]> order() {
        return java.util.Collections.unmodifiableList(this.columns());
    }

    /** How far the ring has spread, in blocks. */
    public double front() {
        return this.age * SPEED;
    }

    private List<int[]> columns() {
        if (this.columns == null) {
            int reach = (int) Math.ceil(this.radius * (1.0 + RAGGED) + MOUND_OUT) + 1;
            List<int[]> all = new ArrayList<>();
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    if (this.inBowl(dx, dz) || this.mound(dx, dz) > 0 || this.cut(dx, dz) > 0
                            || Math.sqrt(dx * dx + dz * dz) < this.rimAt(Math.atan2(dz, dx))) {
                        all.add(new int[] {dx, dz});
                    }
                }
            }
            all.sort(Comparator.<int[]>comparingInt(c -> c[0] * c[0] + c[1] * c[1]).thenComparingDouble(c -> Math.atan2(c[1], c[0])));
            this.columns = all;
        }
        return this.columns;
    }

    // ---------------------------------------------------------------- carving
    /** One tick of the queue: the ring widens and every column it has now reached is done. */
    public void tick(ServerLevel level) {
        this.age++;
        this.work(level, this.front());
    }

    /** Everything that is left, at once (a break finished on load). */
    public void finish(ServerLevel level) {
        this.work(level, Double.MAX_VALUE);
    }

    private void work(ServerLevel level, double front) {
        List<int[]> all = this.columns();
        while (this.index < all.size()) {
            int[] c = all.get(this.index);
            if (Math.sqrt(c[0] * c[0] + c[1] * c[1]) > front) {
                break;
            }
            this.index++;
            if (this.inBowl(c[0], c[1])) {
                this.carve(level, c[0], c[1], this.index % 3 == 0);
            } else {
                this.carve(level, c[0], c[1], false);
                this.raise(level, c[0], c[1]);
            }
        }
    }

    /**
     * Where the ground will be in a column once the ring has passed (nothing is changed): the y of the first free block above
     * whatever is left standing there, or {@code Integer.MIN_VALUE} when the column will hold nothing to stand on. The landing
     * spots are found with this at the burst, so a shard flies to ground that is only cut when the ring gets there.
     */
    public int surfaceAfter(ServerLevel level, int x, int z) {
        int dx = x - this.centre.getX();
        int dz = z - this.centre.getZ();
        boolean inside = Math.sqrt(dx * dx + dz * dz) < this.rimAt(Math.atan2(dz, dx));
        int floor = inside ? Math.max(this.centre.getY() - this.cut(dx, dz), level.getMinBuildHeight() - 1) : Integer.MAX_VALUE;
        int top = this.centre.getY() + CLEAR;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = Math.min(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), level.getMaxBuildHeight()) - 1; y >= level.getMinBuildHeight(); y--) {
            p.set(x, y, z);
            BlockState state = level.getBlockState(p);
            boolean goes = inside && y > floor && y <= top && !spared(state);
            if (!goes && state.blocksMotion()) {
                return y + 1;
            }
        }
        return Integer.MIN_VALUE;
    }

    /** Whether the blast leaves a block standing: air, anything as blast-proof as obsidian, and the Vessel's own anchors. */
    public static boolean spared(BlockState state) {
        return state.isAir() || state.is(TCBlocks.ANCHOR.get()) || state.getBlock().getExplosionResistance() >= Blocks.OBSIDIAN.getExplosionResistance();
    }

    private void carve(ServerLevel level, int dx, int dz, boolean puff) {
        if (Math.sqrt(dx * dx + dz * dz) >= this.rimAt(Math.atan2(dz, dx))) {
            return; // outside the rim nothing is cut (the mound's outer slope)
        }
        int x = this.centre.getX() + dx;
        int z = this.centre.getZ() + dz;
        int floor = Math.max(this.centre.getY() - this.cut(dx, dz), level.getMinBuildHeight() - 1);
        int top = Math.min(this.centre.getY() + CLEAR, level.getMaxBuildHeight() - 1);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        boolean broke = false;
        for (int y = top; y > floor; y--) {
            p.set(x, y, z);
            BlockState state = level.getBlockState(p);
            if (spared(state)) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(p);
            if (be != null) {
                spill(level, p, be);
            } else if (state.getFluidState().isEmpty() && state.isCollisionShapeFullBlock(level, p)) {
                this.debris.merge(state.getBlock(), 1, Integer::sum);
            }
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 3); // erased: no drops
            broke = true;
        }
        if (broke && puff) {
            level.sendParticles(ParticleTypes.POOF, x + 0.5, floor + 1.2, z + 0.5, 2, 0.3, 0.2, 0.3, 0.02);
        }
    }

    /** A destroyed block entity's contents fall out: a container's slots, or whatever its item handler holds. */
    private static void spill(ServerLevel level, BlockPos pos, BlockEntity be) {
        if (be instanceof Container container) {
            Containers.dropContents(level, pos, container);
            return;
        }
        IItemHandler items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (items != null) {
            for (int i = 0; i < items.getSlots(); i++) {
                ItemStack stack = items.extractItem(i, Integer.MAX_VALUE, false);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                }
            }
        }
    }

    /** The commonest blocks the blast broke (up to three, a share each by count), or cobblestone when it broke nothing solid. */
    private List<Block> palette() {
        if (this.palette == null) {
            List<Map.Entry<Block, Integer>> top = new ArrayList<>(this.debris.entrySet());
            top.sort(Map.Entry.<Block, Integer>comparingByValue().reversed().thenComparing(e -> BuiltInRegistries.BLOCK.getKey(e.getKey()).toString()));
            List<Block> out = new ArrayList<>();
            for (int i = 0; i < top.size() && i < 3; i++) {
                int share = Math.max(1, (int) Math.round(10.0 * top.get(i).getValue() / Math.max(1, top.get(0).getValue())));
                for (int k = 0; k < share; k++) {
                    out.add(top.get(i).getKey());
                }
            }
            if (out.isEmpty()) {
                out.add(Blocks.COBBLESTONE);
            }
            this.palette = out;
        }
        return this.palette;
    }

    private void raise(ServerLevel level, int dx, int dz) {
        int h = this.mound(dx, dz);
        if (h <= 0) {
            return;
        }
        int x = this.centre.getX() + dx;
        int z = this.centre.getZ() + dz;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (Math.abs(y - (this.centre.getY() + 1)) > 3) {
            return; // a wall, a roof or a drop: no mound there
        }
        List<Block> from = this.palette();
        for (int k = 0; k < h; k++) {
            BlockPos p = new BlockPos(x, y + k, z);
            if (!level.getBlockState(p).canBeReplaced()) {
                break;
            }
            level.setBlock(p, from.get((int) (this.hash(dx, dz, 5 + k) * from.size()) % from.size()).defaultBlockState(), 3);
        }
    }

    // ---------------------------------------------------------------- save and load
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("centre", this.centre.asLong());
        tag.putDouble("radius", this.radius);
        tag.putLong("seed", this.seed);
        tag.putInt("index", this.index);
        tag.putInt("age", this.age);
        ListTag list = new ListTag();
        this.debris.forEach((block, n) -> {
            CompoundTag d = new CompoundTag();
            d.putString("b", BuiltInRegistries.BLOCK.getKey(block).toString());
            d.putInt("n", n);
            list.add(d);
        });
        tag.put("debris", list);
        return tag;
    }

    public static VesselCrater load(CompoundTag tag) {
        VesselCrater c = new VesselCrater(BlockPos.of(tag.getLong("centre")), tag.getDouble("radius"), tag.getLong("seed"));
        c.index = tag.getInt("index");
        c.age = tag.getInt("age");
        ListTag list = tag.getList("debris", 10);
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getCompound(i).getString("b"));
            if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) {
                c.debris.put(BuiltInRegistries.BLOCK.get(id), list.getCompound(i).getInt("n"));
            }
        }
        return c;
    }
}
