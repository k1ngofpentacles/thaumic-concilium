package dev.movingcastle.thaumicconcilium.client;

import com.leclowndu93150.thaumaturge.api.spell.Spell;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.network.EtherealRangePayload;
import dev.movingcastle.thaumicconcilium.spell.ChannelEvents;
import dev.movingcastle.thaumicconcilium.spell.SpellTrees;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** While channelling the Ethereal Manipulator the mouse wheel moves the held object instead of the hotbar. */
@EventBusSubscriber(modid = ThaumicConcilium.MODID, value = Dist.CLIENT)
public final class EtherealScroll {
    private EtherealScroll() {
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isUsingItem() || event.getScrollDeltaY() == 0) {
            return;
        }
        Spell spell = ChannelEvents.channelledSpell(player.getUseItem());
        if (SpellTrees.contains(spell, SpellTrees.part("ethereal_manipulator"))) {
            event.setCanceled(true);
            PacketDistributor.sendToServer(new EtherealRangePayload((int) Math.signum(event.getScrollDeltaY())));
        }
    }
}
