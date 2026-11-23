package dev.movingcastle.thaumicconcilium.compat;

import com.dtteam.dynamictrees.api.network.MapSignal;
import com.dtteam.dynamictrees.api.network.NodeInspector;
import com.dtteam.dynamictrees.api.treedata.TreePart;
import com.dtteam.dynamictrees.api.worldgen.LevelContext;
import com.dtteam.dynamictrees.block.branch.BranchBlock;
import com.dtteam.dynamictrees.block.branch.TrunkShellBlock;
import com.dtteam.dynamictrees.tree.TreeHelper;
import com.dtteam.dynamictrees.tree.species.Species;
import com.dtteam.dynamictrees.worldgen.DynamicTreeGenerationContext;
import com.dtteam.dynamictrees.worldgen.JoCode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Phase 5d cycle 2d: grows a landmark tree from one of our own JoCodes (see {@code LandmarkShapes}) as a living Dynamic Trees tree, and slims the
 * gum's trunk afterwards. With {@link DynamicTreesSaplings}, the only classes that name Dynamic Trees. Never touch this class unless Dynamic Trees
 * is loaded.
 */
public final class DynamicTreesLandmarks {
    /** The leaf map is 2 * radius + 7 wide; radius 8 is the most Dynamic Trees allows. */
    private static final int RADIUS = 8;

    private DynamicTreesLandmarks() {
    }

    public static Species species(boolean silver) {
        return Species.REGISTRY.get(ResourceLocation.fromNamespaceAndPath("thaumaturge", silver ? "silverwood" : "greatwood"));
    }

    /**
     * Grows the tree of {@code code} on the soil block {@code soil}, rotated to {@code facing}. The plants in the 3 x 3 columns of the trunk are
     * cleared first (a trunk 3 blocks wide needs the ring around it). False when the first branch did not stand afterwards (the soil is put back
     * by Dynamic Trees); true otherwise, with the tree complete and every changed block sent to the players who watch it.
     */
    public static boolean grow(ServerLevel level, BlockPos soil, boolean silver, String code, Direction facing) {
        return grow(level, soil, silver, code, facing, 13);
    }

    /** As above; {@code trunkCap} is the silverwood shape's own cap on the column radius (13 polished, 14 round 2). */
    public static boolean grow(ServerLevel level, BlockPos soil, boolean silver, String code, Direction facing, int trunkCap) {
        Species sp = species(silver);
        if (sp == null || !sp.isValid()) {
            return false;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 1; dy <= 4; dy++) {
                    BlockPos p = soil.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(p);
                    if (!s.isAir() && !(s.getBlock() instanceof DoublePlantBlock) && (s.getBlock() instanceof BushBlock || s.canBeReplaced())
                            && s.getFluidState().isEmpty()) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 18);
                    }
                }
            }
        }
        JoCode jo = sp.getJoCode(code).setCareful(true);
        jo.generate(new DynamicTreeGenerationContext(LevelContext.create(level), sp, soil.above(), soil.mutable(), level.getBiome(soil), facing, RADIUS, true));
        boolean placed = TreeHelper.getBranchOpt(level.getBlockState(soil.above())).filter(b -> b.getFamily() == sp.getFamily()).isPresent();
        if (!placed) {
            return false;
        }
        if (silver) {
            slim(level, soil, sp, trunkCap);
        }
        resend(level, soil);
        return true;
    }

    // ---------------------------------------------------------------- staged growth (cycle 4e)
    /**
     * A JoCode that can be applied in stages: a cell where a branch of the tree's family already stands is left as it is (and does not stop the
     * walk), and {@link #generateStage} is {@code generate}'s own body with the rot test, the post-generation features and their event only at the
     * last stage. The generator keeps non-parent cells apart (careful), so any placement order is safe.
     */
    private static final class StageCode extends JoCode {
        StageCode() {
            super("");
        }

        @Override
        protected boolean setBlockForGeneration(LevelAccessor level, Species species, BlockPos pos, Direction dir, boolean careful, boolean isLast) {
            BlockState there = level.getBlockState(pos);
            if (TreeHelper.getBranchOpt(there).filter(b -> b.getFamily() == species.getFamily()).isPresent()) {
                return false;
            }
            if (there.getBlock() instanceof TrunkShellBlock) {
                // an inflated trunk's shell where a later stage puts a limb: the one-shot tree grew its wood thin and inflated after, so the limb
                // came first; the shell goes and is rebuilt by the inflation where it still has room
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            } else if (TreeHelper.isLeaves(there)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2); // an earlier stage's crown: the new wood takes its place
            }
            return super.setBlockForGeneration(level, species, pos, dir, careful, isLast);
        }

        /** As {@code JoCode.generate}; returns whether the first branch stands afterwards. */
        boolean generateStage(DynamicTreeGenerationContext context, boolean last) {
            LevelAccessor level = context.level();
            Species species = context.species();
            int radius = context.radius();
            this.setFacing(context.facing());
            context.rootPos().set(species.preGeneration(level, context.rootPos(), radius, context.facing(), false, this));
            BlockPos rootPos = context.rootPos();
            if (rootPos == BlockPos.ZERO) {
                return false;
            }
            BlockState initialDirtState = level.getBlockState(rootPos);
            species.placeRootyDirtBlock(level, rootPos, 0);
            this.generateFork(level, species, 0, rootPos, false);
            BlockPos treePos = rootPos.above();
            BlockState treeState = level.getBlockState(treePos);
            BranchBlock firstBranch = TreeHelper.getBranch(treeState);
            if (firstBranch == null) {
                level.setBlock(rootPos, initialDirtState, 3);
                return false;
            }
            com.dtteam.dynamictrees.api.voxmap.SimpleVoxmap leafCluster = species.getLeavesProperties().getCellKit().getLeafCluster();
            int xLen = radius * 2 + leafCluster.getLenX();
            int zLen = radius * 2 + leafCluster.getLenZ();
            com.dtteam.dynamictrees.api.voxmap.SimpleVoxmap leafMap = new com.dtteam.dynamictrees.api.voxmap.SimpleVoxmap(xLen, species.getWorldGenLeafMapHeight(), zLen)
                    .setMapAndCenter(treePos, new BlockPos(xLen / 2, 0, zLen / 2));
            NodeInspector inflator = species.getNodeInflator(leafMap);
            com.dtteam.dynamictrees.systems.nodemapper.FindEndsNode endFinder = new com.dtteam.dynamictrees.systems.nodemapper.FindEndsNode();
            MapSignal signal = new MapSignal(inflator, endFinder);
            signal.destroyLoopedNodes = true;
            firstBranch.analyse(treeState, level, treePos, Direction.DOWN, signal);
            if (signal.foundRoot || signal.overflow) {
                return true; // a looped or overgrown network: leave it as it stands (the tests look for this)
            }
            this.generateAndAgeLeaves(context, leafMap, true);
            if (last) {
                java.util.List<BlockPos> ends = endFinder.getEnds();
                if (!species.handleRot(level, ends, rootPos, treePos, 0, true)) {
                    com.dtteam.dynamictrees.systems.genfeature.context.PostGenerationContext pg =
                            new com.dtteam.dynamictrees.systems.genfeature.context.PostGenerationContext(context, ends, initialDirtState);
                    species.postGeneration(pg);
                    com.dtteam.dynamictrees.platform.Services.EVENT.postSpeciesPostGenerationEvent(pg);
                    this.addSnow(leafMap, level, rootPos, context.biome());
                }
            }
            return true;
        }
    }

    /** The first branch of the landmark at {@code soil} still stands, of the right family. */
    public static boolean stands(ServerLevel level, BlockPos soil, boolean silver) {
        Species sp = species(silver);
        return sp != null && TreeHelper.getBranchOpt(level.getBlockState(soil.above())).filter(b -> b.getFamily() == sp.getFamily()).isPresent();
    }

    /**
     * Grows stage {@code stage} (1 to 8) of the landmark of {@code code} on {@code soil}: only wood and leaves are added, the tree stays a living
     * Dynamic Trees tree. Stage 1 also clears the plants round the trunk; the last stage runs the rot test and the post-generation features, and
     * the silverwood's trunk is slimmed after every stage. False when the first branch does not stand afterwards.
     */
    public static boolean growStage(ServerLevel level, BlockPos soil, boolean silver, String code, Direction facing, int trunkCap, int stage) {
        Species sp = species(silver);
        if (sp == null || !sp.isValid()) {
            return false;
        }
        if (stage <= 1) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = 1; dy <= 4; dy++) {
                        BlockPos p = soil.offset(dx, dy, dz);
                        BlockState s = level.getBlockState(p);
                        if (!s.isAir() && !(s.getBlock() instanceof DoublePlantBlock) && (s.getBlock() instanceof BushBlock || s.canBeReplaced())
                                && s.getFluidState().isEmpty()) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 18);
                        }
                    }
                }
            }
        }
        StageCode jo = new StageCode();
        jo.instructions = dev.movingcastle.thaumicconcilium.world.LandmarkShapes.stage(dev.movingcastle.thaumicconcilium.world.LandmarkShapes.decode(code), stage);
        jo.setCareful(true);
        jo.generateStage(new DynamicTreeGenerationContext(LevelContext.create(level), sp, soil.above(), soil.mutable(), level.getBiome(soil), facing, RADIUS, true),
                stage >= dev.movingcastle.thaumicconcilium.world.LandmarkShapes.STAGES);
        if (!stands(level, soil, silver)) {
            return false;
        }
        if (silver) {
            slim(level, soil, sp, trunkCap);
        }
        resend(level, soil);
        return true;
    }

    /**
     * The gum's trunk (round 2): radius = min(cap, sqrt(sum of the children's radii squared) + 0.5) per block, cap 13 (polish round; 14 before) on the straight column from the
     * root (the only place Dynamic Trees allows more than 8) and 8 everywhere else, instead of Dynamic Trees' own 24 (a 3 block base). The same walk
     * Dynamic Trees' inflator makes. Lowering a thick cell keeps its shell ring (ThickBranchBlock.updateTrunkShells leaves a shell whose core is this
     * cell as it is); only the shells of a cell now at 8 or less are removed here.
     */
    public static void slim(ServerLevel level, BlockPos soil, Species sp) {
        slim(level, soil, sp, 13);
    }

    public static void slim(ServerLevel level, BlockPos soil, Species sp, int cap) {
        BlockPos base = soil.above();
        BlockState first = level.getBlockState(base);
        BranchBlock branch = TreeHelper.getBranch(first);
        if (branch == null) {
            return;
        }
        int topY = base.getY();
        while (TreeHelper.getBranch(level.getBlockState(new BlockPos(base.getX(), topY + 1, base.getZ()))) != null) {
            topY++;
        }
        branch.analyse(first, level, base, Direction.DOWN, new MapSignal(new Slimmer(sp.getFamily().getPrimaryThickness(), base, topY, cap)));
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = base.getY(); y <= topY + 1; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    p.set(base.getX() + dx, y, base.getZ() + dz);
                    BlockState s = level.getBlockState(p);
                    if (s.getBlock() instanceof TrunkShellBlock) {
                        BlockPos core = p.offset(s.getValue(TrunkShellBlock.CORE_DIR).getOffset());
                        BlockState cs = level.getBlockState(core);
                        if (!(cs.getBlock() instanceof BranchBlock cb) || cb.getRadius(cs) <= 8) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }
    }

    /** Leaves are placed with flag 16 during worldgen mode (no client update): tell the chunk source about every block of the tree's box. */
    private static void resend(ServerLevel level, BlockPos soil) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy <= 33; dy++) {
            for (int dx = -11; dx <= 11; dx++) {
                for (int dz = -11; dz <= 11; dz++) {
                    p.set(soil.getX() + dx, soil.getY() + dy, soil.getZ() + dz);
                    if (!level.getBlockState(p).isAir()) {
                        level.getChunkSource().blockChanged(p);
                    }
                }
            }
        }
    }

    /** Mirrors Dynamic Trees InflatorNode, with a flat 0.5 per block, a cap of 13 on the column and 8 elsewhere. */
    private static final class Slimmer implements NodeInspector {
        private final float primary;
        private float radius;
        private BlockPos last = BlockPos.ZERO;
        private final BlockPos base;
        private final int topY;

        private final float cap;

        Slimmer(int primary, BlockPos base, int topY, int cap) {
            this.cap = cap;
            this.base = base;
            this.topY = topY;
            this.primary = primary;
        }

        @Override
        public boolean run(BlockState state, LevelAccessor level, BlockPos pos, Direction fromDir) {
            if (TreeHelper.getBranch(state) != null) {
                this.radius = this.primary;
            }
            return false;
        }

        @Override
        public boolean returnRun(BlockState state, LevelAccessor level, BlockPos pos, Direction fromDir) {
            BranchBlock branch = TreeHelper.getBranch(state);
            if (branch == null) {
                return false;
            }
            float area = this.radius * this.radius;
            boolean twig = true;
            for (Direction dir : Direction.values()) {
                if (dir.equals(fromDir)) {
                    continue;
                }
                BlockPos d = pos.relative(dir);
                if (d.equals(this.last)) {
                    twig = false;
                } else {
                    BlockState ds = level.getBlockState(d);
                    TreePart part = TreeHelper.getTreePart(ds);
                    if (branch.isSameTree(part)) {
                        int r = part.getRadius(ds);
                        area += (float) (r * r);
                    }
                }
            }
            if (!twig) {
                boolean column = pos.getX() == this.base.getX() && pos.getZ() == this.base.getZ() && pos.getY() <= this.topY;
                this.radius = Math.min(column ? this.cap : 8.0F, (float) Math.sqrt(area) + 0.5F);
                branch.setRadius(level, pos, (int) Math.floor(this.radius), null);
            }
            this.last = pos;
            return false;
        }
    }
}
