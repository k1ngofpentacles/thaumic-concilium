package dev.movingcastle.thaumicconcilium.mixin;

import dev.movingcastle.thaumicconcilium.world.GroveBoost;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Phase 5d cycle 4e, the grove's growth boost for vanilla-style saplings: {@code SaplingBlock.randomTick} posts no event, so at its tail a grove
 * sapling gets a {@link GroveBoost#chance} of one more tick (re-entry guarded). Optional, see {@link SoilGrowthBoostMixin}.
 */
@Mixin(SaplingBlock.class)
public abstract class SaplingGrowthBoostMixin {
    @Inject(method = "randomTick", at = @At("TAIL"), require = 0)
    private void thaumicconcilium$groveBoost(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        GroveBoost.extra(level, pos);
    }
}
