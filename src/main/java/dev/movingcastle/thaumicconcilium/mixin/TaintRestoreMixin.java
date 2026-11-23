package dev.movingcastle.thaumicconcilium.mixin;

import com.leclowndu93150.thaumaturge.content.taint.ecology.TaintBiomeManager;
import dev.movingcastle.thaumicconcilium.world.GroveColumns;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Phase 5d cycle 2b: when taint is cleared Thaumaturge restores a column to its worldgen biome ({@code restoreColumn}). Five block classes
 * reach it through {@code restoreIfCleared} and no event exists, so a RETURN injection is the smallest hook: when a restore happened
 * on a column that was painted grove, the column goes back to grove instead. If Thaumaturge renames {@code restoreColumn},
 * {@code defaultRequire: 1} fails loudly at startup.
 */
@Mixin(value = TaintBiomeManager.class, remap = false)
public abstract class TaintRestoreMixin {
    @Inject(method = "restoreColumn", at = @At("RETURN"))
    private static void thaumicconcilium$restoreToGrove(ServerLevel level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && GroveColumns.isGrove(level, pos)) {
            TaintBiomeManager.replaceColumn(level, pos, GroveColumns.KEY);
        }
    }
}
