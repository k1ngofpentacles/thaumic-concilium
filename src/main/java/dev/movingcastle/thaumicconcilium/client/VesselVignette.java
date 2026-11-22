package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * "The light dims near it", the personal half (memo section 4, V4): the screen's edges darken as the local player nears an active
 * Vessel, 0 at 16 blocks to 0.6 at 3 (x1.3 in a gulp), drawn with vanilla's own vignette texture and blend. A GUI layer: hidden with F1,
 * no shader. The halo billboard dims the world around the hole for everyone; this only dims your own view.
 */
public final class VesselVignette implements LayeredDraw.Layer {
    private static final ResourceLocation VIGNETTE = ResourceLocation.withDefaultNamespace("textures/misc/vignette.png");

    /** The darkening for a distance in blocks (pure, for the harness notes). */
    static float amount(float distance, boolean gulp) {
        float a = Mth.clamp((16.0F - distance) / 13.0F, 0.0F, 1.0F) * 0.6F;
        return Math.min(1.0F, gulp ? a * 1.3F : a);
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.options.hideGui || mc.level.getGameTime() - VesselFx.nearestAt > 2L) {
            return;
        }
        float a = amount(VesselFx.nearest, VesselFx.nearestGulp);
        if (a <= 0.001F) {
            return;
        }
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        graphics.setColor(a, a, a, 1.0F);
        graphics.blit(VIGNETTE, 0, 0, -90, 0.0F, 0.0F, w, h, w, h);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
    }
}
