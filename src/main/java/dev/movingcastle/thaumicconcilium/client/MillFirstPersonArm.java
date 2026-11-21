package dev.movingcastle.thaumicconcilium.client;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.item.ShardMill;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * First person: while grinding with a Shard Mill, the free hand is busy on the crank ({@link MillRenderer} draws it),
 * so whatever it holds is hidden.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID, value = Dist.CLIENT)
public final class MillFirstPersonArm {
    private MillFirstPersonArm() {
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        InteractionHand used = player.getUsedItemHand();
        ItemStack mill = player.getItemInHand(used);
        if (event.getHand() != used && mill.getItem() instanceof ShardMill && MillRenderer.isGrinding(player, mill)) {
            event.setCanceled(true);
        }
    }
}
