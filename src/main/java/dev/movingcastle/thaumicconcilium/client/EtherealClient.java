package dev.movingcastle.thaumicconcilium.client;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.world.CrimsonState;
import dev.movingcastle.thaumicconcilium.world.Ethereal;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;

/**
 * Client half of the ethereal state: no block overlay while inside blocks (original ClientEvents:439), and the no-clip flag set
 * after vanilla's per-tick reset and before the local player moves (the server-side tick hook runs too late for the client).
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID, value = Dist.CLIENT)
public final class EtherealClient {
    private EtherealClient() {
    }

    @SubscribeEvent
    public static void overlay(RenderBlockScreenEffectEvent event) {
        if (CrimsonState.isEthereal(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void beforeMove(MovementInputUpdateEvent event) {
        Ethereal.flight(event.getEntity());
    }
}
