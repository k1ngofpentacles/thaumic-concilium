package dev.movingcastle.thaumicconcilium.mixin;

import com.dtteam.dynamictrees.tree.TreeHelper;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.movingcastle.thaumicconcilium.world.GroveDecay;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 5d follow-up, no decay in a living grove ({@link GroveDecay}): Dynamic Trees' leaves die in two places, and both are held off here.
 * <ul>
 * <li>{@code updateHydro}: a leaf whose neighbours give it hydration 0 is set to air (this is also how {@code ageVolume} thins a crown). In a
 * protected grove a leaf with a branch within {@link #BRANCH_REACH} blocks keeps its old hydration instead; one with no branch near (left
 * floating by a felled tree) still goes.</li>
 * <li>{@code removeIfInvalid}: a leaf without enough light is removed; in a protected grove it stays.</li>
 * </ul>
 * The grove test runs only on those paths (hydration 0, too dark), never on an ordinary leaf tick. Optional, like the boost mixins
 * ({@code thaumicconcilium.growth.mixins.json}, {@code required: false}): without Dynamic Trees it is skipped.
 */
@Mixin(targets = "com.dtteam.dynamictrees.block.leaves.DynamicLeavesBlock", remap = false)
public abstract class GroveLeavesNoDecayMixin {
    private static final int BRANCH_REACH = 3;

    @ModifyExpressionValue(method = "updateHydro", require = 0, at = @At(value = "INVOKE",
            target = "Lcom/dtteam/dynamictrees/block/leaves/DynamicLeavesBlock;getHydrationLevelFromNeighbors(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lcom/dtteam/dynamictrees/block/leaves/LeavesProperties;)I"))
    private int thaumicconcilium$keepHydration(int hydro, @Local(argsOnly = true) LevelAccessor level, @Local(argsOnly = true) BlockPos pos,
            @Local(argsOnly = true) BlockState state) {
        if (hydro != 0 || !state.hasProperty(LeavesBlock.DISTANCE) || !GroveDecay.protects(level, pos) || !branchNear(level, pos)) {
            return hydro;
        }
        GroveDecay.kept++;
        return Math.max(1, state.getValue(LeavesBlock.DISTANCE));
    }

    @Inject(method = "removeIfInvalid", require = 0, cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/LevelAccessor;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private void thaumicconcilium$keepInShade(BlockState state, LevelAccessor level, BlockPos pos, net.minecraft.util.RandomSource rand,
            CallbackInfoReturnable<Boolean> cir) {
        if (GroveDecay.protects(level, pos)) {
            GroveDecay.kept++;
            cir.setReturnValue(false);
        }
    }

    private static boolean branchNear(LevelAccessor level, BlockPos pos) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -BRANCH_REACH; dx <= BRANCH_REACH; dx++) {
            for (int dy = -BRANCH_REACH; dy <= BRANCH_REACH; dy++) {
                for (int dz = -BRANCH_REACH; dz <= BRANCH_REACH; dz++) {
                    if (TreeHelper.isBranch(level.getBlockState(p.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz)))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
