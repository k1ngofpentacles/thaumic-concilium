package dev.movingcastle.thaumicconcilium.client;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.GolemBydlo;
import dev.movingcastle.thaumicconcilium.network.BydloSwingPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** A click at nothing while riding a Golem Bydlo is a wand command (the server hears it through this packet). */
@EventBusSubscriber(modid = ThaumicConcilium.MODID, value = Dist.CLIENT)
public final class BydloInput {
    private BydloInput() {
    }

    @SubscribeEvent
    public static void onClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (event.getEntity().getVehicle() instanceof GolemBydlo) {
            PacketDistributor.sendToServer(new BydloSwingPayload());
        }
    }
}
