package dev.movingcastle.thaumicconcilium.mixin.witchery;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Phase 10-3 (R10, user answer: both of Trup Ehsolvi's strokes as the original drew them). The original's level-2 stroke goes five cells
 * down from the start, and the remake's drawing screen clamps the cursor cell to +-4 ({@code coerceIn(.., -4, 4)} in {@code screenToCell}).
 * That clamp is the only limit on drawing it: the screen draws no grid, and the server matches whatever path arrives. This widens it to +-5,
 * touching nothing else in the screen (tier C: no remake code is copied; the target is named by string so nothing loads without the remake).
 * Optional ({@code defaultRequire 0}): a remake update that moves the clamp leaves the long stroke undrawable and breaks nothing.
 * Bytecode checked with javap -c on witchery 0.5.41: {@code bipush -4, iconst_4} twice, in this method only.
 */
@Mixin(targets = "dev.sterner.witchery.client.screen.SymbologyDrawScreen", remap = false)
public abstract class SymbologyGridWidenMixin {
    @ModifyConstant(method = "screenToCell", constant = @Constant(intValue = 4), require = 0, remap = false)
    private int thaumicconcilium$upper(int radius) {
        return 5;
    }

    @ModifyConstant(method = "screenToCell", constant = @Constant(intValue = -4), require = 0, remap = false)
    private int thaumicconcilium$lower(int radius) {
        return -5;
    }
}
