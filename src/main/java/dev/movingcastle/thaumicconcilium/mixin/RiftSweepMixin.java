package dev.movingcastle.thaumicconcilium.mixin;

import com.leclowndu93150.thaumaturge.content.entity.EntityFluxRift;
import dev.movingcastle.thaumicconcilium.rift.ConciliumRifts;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Phase 5c: a rift held by a hex eats no blocks and hurts nothing as it grows with the hex's burden. Thaumaturge runs its block-eating
 * sweep from the rift's private tick with no event and no switch, so the one call is skipped here (HEAD, cancel) for rifts Concilium
 * marked TAMED (a native rift held by a lesser hex). Chained rifts (the Brightest One's), wild, spatial and gem rifts run the sweep as before. If Thaumaturge renames
 * {@code RiftSegmentSweep.tick}, {@code defaultRequire: 1} fails loudly at startup.
 */
@Mixin(targets = "com.leclowndu93150.thaumaturge.content.entity.RiftSegmentSweep", remap = false)
public abstract class RiftSweepMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void thaumicconcilium$noSweepWhenHexed(ServerLevel level, EntityFluxRift rift, CallbackInfo ci) {
        ConciliumRifts.Kind kind = ConciliumRifts.kind(rift);
        if (kind == ConciliumRifts.Kind.TAMED) {
            ci.cancel();
        }
    }
}
