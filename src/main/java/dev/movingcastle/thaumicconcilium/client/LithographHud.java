package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.lithograph.Lithograph;
import dev.movingcastle.thaumicconcilium.lithograph.PartyStats;
import dev.movingcastle.thaumicconcilium.network.PartyStatusPayload;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * The lithograph HUD (original: {@code ClientEvents.renderAstralHole}): while the player is lithographed and not
 * relieved, a hole into the astral drawn in the top left corner, a slowly turning star in it, and for each of the four
 * watched players their health, blood, mana and runic shielding as constellation glyphs. The original blinked the star
 * and the glyphs with a shader; here the blink is an alpha pulse. The original's dialBottom option (Thaumcraft's) is
 * ignored: it always draws at the top. The art is always drawn at the original's full size.
 */
public final class LithographHud implements LayeredDraw.Layer {
    private static final ResourceLocation HOLE = ThaumicConcilium.id("textures/gui/astral_hole.png");
    private static final ResourceLocation STAR = ThaumicConcilium.id("textures/gui/star.png");
    public static final ResourceLocation CONSTELLATIONS = ThaumicConcilium.id("textures/gui/constellations.png");
    /** The original hole is 20 x 15.2 sprite blocks of 16: 320 x 243 GUI pixels. */
    private static final int HOLE_W = 320;
    private static final int HOLE_H = 243;
    private static final int STAR_SIZE = 112;
    // Glyph positions per watched player (original: the numbers in renderAstralHole): health, blood, mana, runes.
    private static final int[][] HEALTH = {{0, 20}, {20, 0}, {100, 10}, {15, 100}};
    private static final int[][] BLOOD = {{14, 40}, {37, 17}, {90, 37}, {37, 98}};
    private static final int[][] MANA = {{2, 55}, {53, 12}, {85, 56}, {53, 90}};
    private static final int[][] RUNES = {{5, 70}, {73, 2}, {95, 72}, {73, 108}};
    /** Sprite sheet columns (x in pixels). */
    private static final int COL_RUNES = 0;
    private static final int COL_MANA = 16;
    private static final int COL_BLOOD = 32;
    private static final int COL_HEALTH = 48;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || mc.screen != null || !Lithograph.get(player).hudVisible()) {
            return;
        }
        // The original's blink: a counter that walks 0 to 60 and back, one step every 3 ticks.
        int step = (player.tickCount / 3) % 120;
        int blink = step <= 60 ? step : 120 - step;
        float pulse = 0.45F + 0.55F * (blink / 60.0F);

        graphics.pose().pushPose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(HOLE, 0, 0, HOLE_W, HOLE_H, 0.0F, 0.0F, 512, 384, 512, 384);

        graphics.pose().pushPose();
        graphics.pose().translate(STAR_SIZE / 2.0F, STAR_SIZE / 2.0F, 0.0F);
        graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees((player.tickCount + delta.getGameTimeDeltaPartialTick(false)) * 0.5F));
        graphics.pose().translate(-STAR_SIZE / 2.0F, -STAR_SIZE / 2.0F, 0.0F);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, pulse);
        graphics.blit(STAR, 0, 0, STAR_SIZE, STAR_SIZE, 0.0F, 0.0F, 190, 190, 190, 190);
        graphics.pose().popPose();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, pulse);
        List<PartyStats> party = PartyStatusPayload.current;
        for (int i = 0; i < Math.min(4, party.size()); i++) {
            PartyStats s = party.get(i);
            if (!s.present()) {
                continue;
            }
            glyph(graphics, HEALTH[i], COL_HEALTH, s.health());
            glyph(graphics, BLOOD[i], COL_BLOOD, s.blood());
            glyph(graphics, MANA[i], COL_MANA, s.mana());
            glyph(graphics, RUNES[i], COL_RUNES, s.runes());
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        graphics.pose().popPose();
    }

    private static void glyph(GuiGraphics graphics, int[] at, int column, int value) {
        if (value >= 0) {
            graphics.blit(CONSTELLATIONS, at[0], at[1], 16, 16, column, 16.0F * value, 16, 16, 256, 256);
        }
    }
}
