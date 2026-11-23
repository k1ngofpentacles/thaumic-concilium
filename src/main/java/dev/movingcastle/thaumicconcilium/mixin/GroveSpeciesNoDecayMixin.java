package dev.movingcastle.thaumicconcilium.mixin;

import dev.movingcastle.thaumicconcilium.world.GroveDecay;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 5d follow-up, no decay in a living grove ({@link GroveDecay}): Dynamic Trees' branch rot ({@code Species.rot}, a twig end that cannot grow
 * withers) and disease ({@code Species.handleDisease}, a young tree dies) are held off in a protected grove. Rapid rot (the clean-up of wood cut
 * off from its tree) is left alone, so a felled tree still clears. Optional ({@code thaumicconcilium.growth.mixins.json}).
 */
@Mixin(targets = "com.dtteam.dynamictrees.tree.species.Species", remap = false)
public abstract class GroveSpeciesNoDecayMixin {
    @Inject(method = "rot", at = @At("HEAD"), cancellable = true, require = 0)
    private void thaumicconcilium$noRot(LevelAccessor level, BlockPos pos, int neighborCount, int radius, int fertility, RandomSource random,
            boolean rapid, boolean growLeaves, CallbackInfoReturnable<Boolean> cir) {
        if (!rapid && GroveDecay.protects(level, pos)) {
            GroveDecay.kept++;
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "handleDisease", at = @At("HEAD"), cancellable = true, require = 0)
    private void thaumicconcilium$noDisease(Level level, com.dtteam.dynamictrees.api.treedata.TreePart baseTreePart, BlockPos treePos,
            RandomSource random, int fertility, CallbackInfoReturnable<Boolean> cir) {
        if (GroveDecay.protects(level, treePos)) {
            cir.setReturnValue(false);
        }
    }
}
