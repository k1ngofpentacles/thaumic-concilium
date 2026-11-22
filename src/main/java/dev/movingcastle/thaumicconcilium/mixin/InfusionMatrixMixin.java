package dev.movingcastle.thaumicconcilium.mixin;

import com.leclowndu93150.thaumaturge.content.infusion.BlockEntityInfusionMatrix;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.movingcastle.thaumicconcilium.world.BindingSessions;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The Crimson Dagger's binding replaces the native instability outcome of its own craft with a strike. Thaumaturge fires no event
 * for an instability trigger and its outcome registry is global (a datapack cannot scope an outcome to one recipe), so the one call
 * in the matrix's craft cycle is wrapped: with a binding at this matrix {@link BindingSessions#onInstability} takes it, otherwise the
 * original runs untouched. If Thaumaturge renames {@code craftCycle} or the call, {@code defaultRequire: 1} fails loudly at startup.
 */
@Mixin(value = BlockEntityInfusionMatrix.class, remap = false)
public abstract class InfusionMatrixMixin {
    @WrapOperation(method = "craftCycle", at = @At(value = "INVOKE",
            target = "Lcom/leclowndu93150/thaumaturge/content/infusion/InstabilityEvents;trigger(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Ljava/util/List;F)V"))
    private void thaumicconcilium$strikeInstead(ServerLevel level, BlockPos matrix, List<BlockPos> pedestals, float stability, Operation<Void> original) {
        if (BindingSessions.onInstability(level, matrix)) {
            original.call(level, matrix, pedestals, stability);
        }
    }
}
