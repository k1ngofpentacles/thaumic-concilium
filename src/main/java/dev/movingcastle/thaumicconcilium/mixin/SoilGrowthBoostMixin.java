package dev.movingcastle.thaumicconcilium.mixin;

import dev.movingcastle.thaumicconcilium.world.GroveBoost;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Phase 5d cycle 4e, the grove's growth boost for Dynamic Trees: its rooty soil's random tick is the only place a tree grows and it posts no
 * event, so at the tail of {@code SoilBlock.randomTick} a grove column gets a {@link GroveBoost#chance} of one more tick (re-entry guarded). Optional
 * ({@code thaumicconcilium.growth.mixins.json}, {@code required: false}, {@code defaultRequire: 0}): without Dynamic Trees, or if it renames the
 * method, the mixin is skipped and only the crops and vanilla saplings are boosted.
 */
@Mixin(targets = "com.dtteam.dynamictrees.block.soil.SoilBlock", remap = false)
public abstract class SoilGrowthBoostMixin {
    @Inject(method = "randomTick", at = @At("TAIL"), require = 0)
    private void thaumicconcilium$groveBoost(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        GroveBoost.extra(level, pos);
    }
}
