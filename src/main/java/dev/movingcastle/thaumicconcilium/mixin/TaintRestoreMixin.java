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
 * Phase 5d cycle 2b: when taint is cleared Thaumaturge restores a column to its worldgen biome. Taint blocks reach
 * {@code restoreColumn} through {@code restoreIfCleared}; since Thaumaturge 1.1.1 the Ethereal Bloom uses
 * {@code restoreProtectedBiomeSamples}. No event exists, so a RETURN injection on each is the smallest hook: when a restore
 * happened on a column that was painted grove, the column goes back to grove instead. If Thaumaturge renames either method,
 * {@code defaultRequire: 1} fails loudly at startup.
 */
@Mixin(value = TaintBiomeManager.class, remap = false)
public abstract class TaintRestoreMixin {
    @Inject(method = "restoreColumn", at = @At("RETURN"))
    private static void thaumicconcilium$restoreToGrove(ServerLevel level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        thaumicconcilium$repaint(level, pos, cir.getReturnValueZ());
    }

    @Inject(method = "restoreProtectedBiomeSamples", at = @At("RETURN"))
    private static void thaumicconcilium$bloomRestoreToGrove(ServerLevel level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        thaumicconcilium$repaint(level, pos, cir.getReturnValueZ());
    }

    private static void thaumicconcilium$repaint(ServerLevel level, BlockPos pos, boolean restored) {
        if (restored && GroveColumns.isGrove(level, pos)) {
            TaintBiomeManager.replaceColumn(level, pos, GroveColumns.KEY);
        }
    }
}
