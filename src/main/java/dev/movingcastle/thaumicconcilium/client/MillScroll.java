package dev.movingcastle.thaumicconcilium.client;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.item.ShardMill;
import dev.movingcastle.thaumicconcilium.network.MillSelectPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Sneak + mouse wheel while holding a Shard Mill chooses its shard instead of scrolling the hotbar. */
@EventBusSubscriber(modid = ThaumicConcilium.MODID, value = Dist.CLIENT)
public final class MillScroll {
    private MillScroll() {
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isShiftKeyDown() || player.isUsingItem() || event.getScrollDeltaY() == 0
                || !(player.getMainHandItem().getItem() instanceof ShardMill)) {
            return;
        }
        event.setCanceled(true);
        PacketDistributor.sendToServer(new MillSelectPayload(event.getScrollDeltaY() > 0 ? -1 : 1));
    }
}
