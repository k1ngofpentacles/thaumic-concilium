package dev.movingcastle.thaumicconcilium.client;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.item.CrimsonDagger;
import dev.movingcastle.thaumicconcilium.network.DaggerStabPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The stab input (decision D9-b, {@link CrimsonDagger#STAB_WHILE_RAISED}). Vanilla discards attack clicks while an item is in
 * use, so no swing and no attack event fire while the dagger is raised; the attack key is polled here and its press (the
 * edge, not the hold) is sent to the server. No vanilla swing plays, which is wanted.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID, value = Dist.CLIENT)
public final class DaggerInput {
    private static boolean wasDown;

    private DaggerInput() {
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Pre event) {
        if (!CrimsonDagger.STAB_WHILE_RAISED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        boolean down = player != null && mc.screen == null && mc.options.keyAttack.isDown();
        boolean pressed = down && !wasDown;
        wasDown = down;
        if (!pressed || !player.isUsingItem() || !(player.getUseItem().getItem() instanceof CrimsonDagger)) {
            return;
        }
        DaggerAnim.Frame frame = DaggerAnim.frame(player, 0.0F);
        if (frame.mode() == DaggerAnim.Mode.RAISE && frame.r() >= 1.0F) {
            PacketDistributor.sendToServer(new DaggerStabPayload());
        }
    }
}
