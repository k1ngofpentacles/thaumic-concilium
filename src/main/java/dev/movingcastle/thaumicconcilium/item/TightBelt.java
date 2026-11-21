package dev.movingcastle.thaumicconcilium.item;

import com.leclowndu93150.thaumaturge.api.warp.IPlayerWarp;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.leclowndu93150.thaumaturge.registry.TTBlocks;
import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * Tight Belt (belt). Temporary warp becomes Thaumarhia, measured by total warp, and is burned at about 1 in 25 per
 * tick; each burned point leaks a pocket of flux gas at the wearer's head.
 */
public class TightBelt extends WarpBauble {
    public TightBelt(Properties properties) {
        super(properties, TTMobEffects.THAUMARHIA, 25, "item.thaumicconcilium.tight_belt.desc");
    }

    @Override
    protected int levelWarp(IPlayerWarp warp) {
        return warp.get(WarpType.PERMANENT) + warp.get(WarpType.NORMAL) + warp.get(WarpType.TEMPORARY);
    }

    @Override
    protected void onDrain(ServerPlayer player) {
        BlockPos head = player.blockPosition().above();
        if (player.level().getBlockState(head).isAir()) {
            player.level().setBlock(head, TTBlocks.FLUX_GAS.get().defaultBlockState(), 3);
        }
    }
}
