package dev.movingcastle.thaumicconcilium.item;

import com.leclowndu93150.thaumaturge.api.warp.IPlayerWarp;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import dev.movingcastle.thaumicconcilium.spell.Looking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;

/**
 * Burdening Amulet (necklace). Once a second, the creatures you own within 32 blocks attack whatever living thing
 * you are looking at, up to 32 blocks away. Temporary warp becomes Death Gaze, measured by temporary warp, and is
 * burned at about 1 in 10 per tick.
 */
public class BurdeningAmulet extends WarpBauble {
    public BurdeningAmulet(Properties properties) {
        super(properties, TTMobEffects.DEATH_GAZE, 10, "item.thaumicconcilium.burdening_amulet.desc");
    }

    @Override
    protected int levelWarp(IPlayerWarp warp) {
        return warp.get(WarpType.TEMPORARY);
    }

    @Override
    protected void wornTick(ServerPlayer player) {
        if (player.tickCount % 20 != 0) {
            return;
        }
        Entity look = Looking.at(player, 32.0, 0.1);
        if (!(look instanceof LivingEntity victim)) {
            return;
        }
        for (Mob pet : player.serverLevel().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32.0),
                m -> m != victim && m instanceof OwnableEntity owned && owned.getOwner() == player)) {
            pet.setTarget(victim);
        }
    }
}
