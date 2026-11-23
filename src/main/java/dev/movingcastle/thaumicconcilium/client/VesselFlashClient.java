package dev.movingcastle.thaumicconcilium.client;

import dev.movingcastle.thaumicconcilium.network.VesselFlashPayload;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The client's record of the last finale flash (phase 5d cycle 4e): when, where, how wide, and whether this player was inside the swell;
 * and the outside observer's white-out drawn from it (a GUI layer: a plain fill of the screen, {@link VesselFinale#bang}). A player with
 * {@code inside()} true is spared the white-out; one looking away from the hole at the collapse gets less or none of it. The escaped
 * light in the world is {@link VesselFinaleDraw#onRender}.
 */
public final class VesselFlashClient implements LayeredDraw.Layer {
    @Nullable
    private static VesselFlashPayload last;
    /** How much of the white-out this eye gets, fixed when the flash arrives (it was looking at the hole, or not). */
    private static float facing;

    public static void receive(VesselFlashPayload flash) {
        last = flash;
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 to = new Vec3(flash.x(), flash.y(), flash.z()).subtract(camera.getPosition());
        Vector3f look = camera.getLookVector();
        double len = to.length();
        double cos = len < 1.0e-3 ? 1.0 : (to.x * look.x() + to.y * look.y() + to.z * look.z()) / len;
        facing = VesselFinale.looking((float) Math.toDegrees(Math.acos(Mth.clamp(cos, -1.0, 1.0))));
    }

    /** The newest flash received this session, or null. */
    @Nullable
    public static VesselFlashPayload last() {
        return last;
    }

    /** Forgets the flash (the render harness, between stills). */
    public static void clear() {
        last = null;
    }

    /** Ticks since the last flash's collapse, or -1 when there is none to draw (none yet, over, or another world's). */
    public static float age(float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (last == null || mc.level == null) {
            return -1.0F;
        }
        float u = VesselFinale.pinnedAge >= 0.0F ? VesselFinale.pinnedAge - VesselFinale.pinnedC : mc.level.getGameTime() - last.time() + partial;
        return u >= 0.0F && u <= VesselFinale.flashTicks() ? u : -1.0F;
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker delta) {
        whiteOut(graphics, delta);
    }

    /** Mod GUI layers are drawn with the HUD hidden too (only vanilla's are held back by F1), so the white-out spares no one. */
    private static void whiteOut(GuiGraphics graphics, DeltaTracker delta) {
        float u = age(delta.getGameTimeDeltaPartialTick(false));
        if (u < 0.0F || last == null || last.inside()) {
            return;
        }
        int a = Mth.clamp((int) (VesselFinale.bang(u) * facing * 255.0F), 0, 255);
        if (a > 0) {
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), a << 24 | VesselFinale.BANG_COLOR);
        }
    }
}
