package dev.movingcastle.thaumicconcilium.mixin;

import com.leclowndu93150.thaumaturge.content.aura.node.BlockEntityNode;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.movingcastle.thaumicconcilium.world.GroveColumns;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Phase 5d cycle 2b: a pure node in a healthy magical forest slowly brightens (Thaumaturge tests the biome key
 * {@code magical_forest}, with no tag or event to hook), so the grove, which is built as a magical forest, must pass the same test.
 * The one {@code Holder.is(ResourceKey)} call in {@code driftFromChunkHealth} is wrapped to also accept our grove tag. If Thaumaturge
 * renames the method or restructures the call, {@code defaultRequire: 1} fails loudly at startup.
 */
@Mixin(value = BlockEntityNode.class, remap = false)
public abstract class NodeDriftMixin {
    @WrapOperation(method = "driftFromChunkHealth", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Holder;is(Lnet/minecraft/resources/ResourceKey;)Z"))
    private boolean thaumicconcilium$groveCountsAsMagicalForest(Holder<Biome> holder, ResourceKey<Biome> key, Operation<Boolean> original) {
        return original.call(holder, key) || holder.is(GroveColumns.TAG);
    }
}
