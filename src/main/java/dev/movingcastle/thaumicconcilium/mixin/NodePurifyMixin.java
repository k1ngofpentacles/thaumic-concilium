package dev.movingcastle.thaumicconcilium.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.movingcastle.thaumicconcilium.world.GroveColumns;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Phase 5d cycle 2b: a pure node paints {@code magical_forest} over tainted columns; the biome is a hardcoded key inside
 * {@code purify} with no event. For a column that was painted grove the call is made with the grove's key instead, so a purified
 * grove column stays grove (a column that is already grove is left alone; a tainted grove column returns to grove). Other columns are
 * untouched. If Thaumaturge renames {@code purify} or {@code replaceColumn}, {@code defaultRequire: 1} fails loudly at startup.
 */
@Mixin(targets = "com.leclowndu93150.thaumaturge.content.aura.node.NodeBiomeSpread", remap = false)
public abstract class NodePurifyMixin {
    @WrapOperation(method = "purify", at = @At(value = "INVOKE",
            target = "Lcom/leclowndu93150/thaumaturge/content/taint/ecology/TaintBiomeManager;replaceColumn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/resources/ResourceKey;)Z"))
    private static boolean thaumicconcilium$purifyKeepsGrove(ServerLevel level, BlockPos sample, ResourceKey<Biome> biome, Operation<Boolean> original) {
        return original.call(level, sample, GroveColumns.isGrove(level, sample) ? GroveColumns.KEY : biome);
    }
}
