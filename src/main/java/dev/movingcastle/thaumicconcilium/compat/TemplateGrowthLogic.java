package dev.movingcastle.thaumicconcilium.compat;

import com.dtteam.dynamictrees.block.branch.BranchBlock;
import com.dtteam.dynamictrees.systems.GrowSignal;
import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKit;
import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKitConfiguration;
import com.dtteam.dynamictrees.systems.growthlogic.context.DirectionSelectionContext;
import com.dtteam.dynamictrees.systems.growthlogic.context.PositionalSpeciesContext;
import com.dtteam.dynamictrees.tree.TreeHelper;
import com.dtteam.dynamictrees.tree.species.Species;
import dev.movingcastle.thaumicconcilium.world.GroveHill;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 5d follow-up: natural growth of the grove's species follows its template ({@link DynamicTreesGrove#template}) up to the tree's cap
 * ({@link GroveHill#cap}). Dynamic Trees sends a grow signal from the root up the tree; at each branch this logic picks the next direction:
 * <ul>
 * <li>each signal is planned once at the root ({@link #plan}): its target is the shallowest missing template cell under the cap that DT can
 * grow, and it walks the template's path there (DT turns the leaf at the target into wood, or grows leaves into air first, as it always does);
 * so the template unfolds level by level;</li>
 * <li>at a finished tip a twig only grows leaves, and only into air; a thicker cell whose template children are all over the cap ends the signal (it turns back
 * into its parent with no energy), so no wood ever grows off the template;</li>
 * <li>off the template (wood placed some other way) Dynamic Trees' default logic runs.</li>
 * </ul>
 * The signal's energy is the template's depth plus a margin, so it can always reach the frontier. Registered by {@link DynamicTreesGrove#init}.
 */
public final class TemplateGrowthLogic extends GrowthLogicKit {
    public TemplateGrowthLogic(ResourceLocation name) {
        super(name);
    }

    private record Tree(GroveHill.Kind kind, DynamicTreesGrove.Template template, int limitY) {
    }

    private static Tree tree(Level level, Species species, BlockPos root) {
        GroveHill.Kind kind = DynamicTreesGrove.kindOf(species);
        if (kind == null) {
            return null;
        }
        DynamicTreesGrove.Pick pick = DynamicTreesGrove.pick(kind, DynamicTreesGrove.silver(species), root.getX(), root.getZ());
        if (pick == null) {
            return null;
        }
        int cap = GroveHill.cap(kind, level instanceof ServerLevel sl ? sl : null, root);
        return new Tree(kind, DynamicTreesGrove.template(pick), GroveHill.branchLimit(cap));
    }

    @Override
    public Direction selectNewDirection(GrowthLogicKitConfiguration config, DirectionSelectionContext ctx) {
        GrowSignal signal = ctx.signal();
        Tree t = tree(ctx.level(), ctx.species(), signal.rootPos);
        if (t == null) {
            return super.selectNewDirection(config, ctx);
        }
        BlockPos pos = ctx.pos();
        BlockPos rel = pos.subtract(signal.rootPos);
        Integer at = t.template().index.get(rel.asLong());
        if (at == null) {
            offTemplate++;
            return super.selectNewDirection(config, ctx);
        }
        Level level = ctx.level();
        DynamicTreesGrove.Template tp = t.template();
        int[] plan = PLANS.computeIfAbsent(signal, s -> plan(level, s.rootPos, t, s.rand));
        // the next cell on the path from the root to this signal's target
        for (int k = 0; k < plan.length - 1; k++) {
            if (plan[k] == at) {
                int next = plan[k + 1];
                if (next == plan[plan.length - 1] && !TreeHelper.isBranch(level.getBlockState(signal.rootPos.offset(tp.cells.get(next))))) {
                    toMissing++;
                } else {
                    toExisting++;
                }
                return dirTo(pos, signal.rootPos.offset(tp.cells.get(next)));
            }
        }
        // a finished tip: a twig grows leaves only; a thicker cell ends the signal so no wood grows off the template
        BranchBlock branch = ctx.branch();
        BlockState here = level.getBlockState(pos);
        if (branch.getRadius(here) <= ctx.species().getFamily().getPrimaryThickness()) {
            for (Direction d : new Direction[] {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
                // only into air: a twig grows leaves into air, but turns a leaf block into wood (DynamicLeavesBlock.branchOut)
                if (level.isEmptyBlock(pos.relative(d)) && !t.template().index.containsKey(rel.relative(d).asLong())) {
                    tipLeaves++;
                    return d;
                }
            }
        }
        ended++;
        signal.energy = 0.0F;
        return signal.dir.getOpposite();
    }

    /** Tries before a template cell DT keeps refusing is left out (it may grow later, when the memory is cleared or the server restarts). */
    private static final int MAX_TRIES = 6;
    /** Per tree root: the last planned target {cell, 1 when it was a growth target}, and how often each cell was refused. */
    private static final java.util.Map<Long, int[]> LAST = new java.util.HashMap<>();
    private static final java.util.Map<Long, java.util.Map<Integer, Integer>> FAILS = new java.util.HashMap<>();
    public static long skipped;

    /** Tests: the cells a tree's growth has given up on (relative to its root), for the log. */
    public static java.util.Map<Integer, Integer> refused(BlockPos root, DynamicTreesGrove.Template template) {
        for (java.util.Map.Entry<Long, java.util.Map<Integer, Integer>> e : FAILS.entrySet()) {
            if (e.getKey() - root.asLong() * 31L == System.identityHashCode(template)) {
                return e.getValue();
            }
        }
        return java.util.Map.of();
    }

    /** Each live signal's path (template cell indices from the first cell to its target); weak, so a finished signal drops out. */
    private static final java.util.Map<GrowSignal, int[]> PLANS = new java.util.WeakHashMap<>();

    /**
     * Plans one signal: the target is the shallowest frontier cell (a missing template cell under the cap whose parent is wood and that DT
     * can grow), so the template unfolds level by level as the finale's stages do (low limbs sprout before the trunk thickens into shells,
     * which DT can never branch through). With no frontier left the target is a random finished tip (its leaves). One scan of the template
     * (at most a few hundred block reads) per signal, and signals come only when Dynamic Trees grows the tree.
     */
    private static int[] plan(Level level, BlockPos root, Tree t, net.minecraft.util.RandomSource rand) {
        DynamicTreesGrove.Template tp = t.template();
        // the last target of this tree: still not wood means DT refused it (no light for its leaves, mostly): count it, skip it after MAX_TRIES
        long key = root.asLong() * 31L + System.identityHashCode(tp); // a different tree on the same root starts afresh
        int[] last = LAST.get(key);
        java.util.Map<Integer, Integer> fails = FAILS.computeIfAbsent(key, k -> new java.util.HashMap<>());
        if (last != null && last[1] == 1 && !TreeHelper.isBranch(level.getBlockState(root.offset(tp.cells.get(last[0]))))) {
            fails.merge(last[0], 1, Integer::sum);
        }
        int n = tp.cells.size();
        int[] sizes = tp.sizes(t.limitY());
        boolean[] wood = new boolean[n];
        int best = Integer.MAX_VALUE, ties = 0, target = -1;
        List<Integer> tips = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (sizes[i] == 0) {
                continue; // over the cap (and so is its subtree)
            }
            BlockPos at = root.offset(tp.cells.get(i));
            int p = tp.parent[i];
            wood[i] = TreeHelper.isBranch(level.getBlockState(at));
            if (wood[i]) {
                boolean tip = true;
                for (int c : tp.children.get(i)) {
                    tip &= sizes[c] == 0;
                }
                if (tip) {
                    tips.add(i);
                }
                continue;
            }
            if (p >= 0 && !wood[p]) {
                continue; // not on the frontier yet
            }
            if (fails.getOrDefault(i, 0) >= MAX_TRIES) {
                skipped++;
                continue;
            }
            if (!growable(level, at, p >= 0 ? root.offset(tp.cells.get(p)) : root)) {
                blocked++;
                continue;
            }
            int d = tp.depth[i];
            if (d < best) {
                best = d;
                ties = 1;
                target = i;
            } else if (d == best && rand.nextInt(++ties) == 0) {
                target = i; // a fair pick among the shallowest
            }
        }
        boolean grow = target >= 0;
        if (!grow) {
            target = tips.isEmpty() ? 0 : tips.get(rand.nextInt(tips.size()));
        }
        if (LAST.size() > 50_000) { // transient memory: a long-running server forgets it now and then, harmlessly
            LAST.clear();
            FAILS.clear();
        }
        LAST.put(key, new int[] {target, grow ? 1 : 0});
        java.util.ArrayDeque<Integer> path = new java.util.ArrayDeque<>();
        for (int i = target; i >= 0; i = tp.parent[i]) {
            path.push(i);
        }
        return path.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Whether DT can put wood at {@code target} grown from {@code from}: air or leaves there, and no branch beside it but {@code from}. */
    private static boolean growable(Level level, BlockPos target, BlockPos from) {
        BlockState s = level.getBlockState(target);
        if (!s.isAir() && !TreeHelper.isLeaves(s)) { // a trunk shell counts as air to DT's signal, but branchOut can never put wood there
            return false;
        }
        for (Direction d : Direction.values()) {
            BlockPos n = target.relative(d);
            if (!n.equals(from) && TreeHelper.isBranch(level.getBlockState(n))) {
                return false;
            }
        }
        return true;
    }

    /** Tests: where the signals went since the game started. */
    public static long toMissing;
    public static long toExisting;
    public static long blocked;
    public static long tipLeaves;
    public static long ended;
    public static long offTemplate;

    private static Direction dirTo(BlockPos from, BlockPos to) {
        return Direction.fromDelta(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ());
    }

    @Override
    public float getEnergy(GrowthLogicKitConfiguration config, PositionalSpeciesContext ctx) {
        float base = super.getEnergy(config, ctx);
        Tree t = tree(ctx.level(), ctx.species(), ctx.pos());
        return t == null ? base : Math.max(base, t.template().maxDepth + 4.0F);
    }
}
