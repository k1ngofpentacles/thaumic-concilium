package dev.movingcastle.thaumicconcilium.client;

import com.leclowndu93150.thaumaturge.api.casters.FocusCaster;
import com.leclowndu93150.thaumaturge.client.input.TTKeybinds;
import com.mojang.blaze3d.platform.InputConstants;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.network.BraceletCastPayload;
import dev.movingcastle.thaumicconcilium.spell.BraceletCasting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * The bracelet's controls. The cast key (V by default, under the thumb so the fingers stay on WASD) sends its press and
 * release to the server. Thaumaturge's change-focus key opens the bracelet radial ({@link BraceletRadial}) while a
 * bracelet is worn and no wand is held; held wands keep the key.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID, value = Dist.CLIENT)
public final class BraceletInput {
    public static final KeyMapping CAST = new KeyMapping("key.thaumicconcilium.bracelet_cast", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.thaumicconcilium");
    private static boolean castDown;
    private static boolean focusDown;

    private BraceletInput() {
    }

    /** A bracelet is worn and neither hand holds a wand, so the bracelet owns the focus key. */
    public static boolean braceletOwnsFocusKey(LocalPlayer player) {
        return !(player.getMainHandItem().getItem() instanceof FocusCaster)
                && !(player.getOffhandItem().getItem() instanceof FocusCaster)
                && !BraceletCasting.worn(player).isEmpty();
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            castDown = false;
            BraceletRadial.close(mc, false);
            return;
        }
        boolean worn = !BraceletCasting.worn(player).isEmpty();

        // Cast key: tell the server about each edge. Opening a screen counts as a release.
        boolean cast = worn && mc.screen == null && CAST.isDown();
        while (CAST.consumeClick()) {
            // edges are read from isDown; drain the click queue
        }
        if (cast != castDown) {
            castDown = cast;
            PacketDistributor.sendToServer(new BraceletCastPayload(cast));
        }

        // Focus key. Vanilla's offhand swap shares F by default; Thaumaturge drains it only while a wand is held.
        boolean owns = braceletOwnsFocusKey(player);
        if (owns && mc.screen == null && TTKeybinds.CHANGE_FOCUS.same(mc.options.keySwapOffhand)) {
            while (mc.options.keySwapOffhand.consumeClick()) {
                // swallowed: F belongs to the bracelet radial
            }
        }
        boolean focus = owns && TTKeybinds.CHANGE_FOCUS.isDown();
        if (focus && !focusDown && mc.screen == null) {
            if (player.isShiftKeyDown()) {
                BraceletRadial.remove();
            } else {
                BraceletRadial.open(mc);
            }
        } else if (!focus && focusDown) {
            BraceletRadial.close(mc, true);
        }
        focusDown = focus;
    }

    @SubscribeEvent
    public static void mouse(InputEvent.MouseButton.Pre event) {
        if (BraceletRadial.isOpen() && Minecraft.getInstance().screen == null) {
            event.setCanceled(true);
            if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT && event.getAction() == GLFW.GLFW_PRESS) {
                BraceletRadial.close(Minecraft.getInstance(), true);
            }
        }
    }
}
