package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.world.CrimsonState;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import com.mojang.blaze3d.platform.NativeImage;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * First person while chained (original ClientEvents renderEmptyHand path with ethereal.frag): both forearms rise from the bottom of
 * the screen, a cuff around each, a chain between. The shader mix {@code mix(texel, (0, 0.749, 1, 0.5), t)}, {@code t = (100 -
 * chainedTime) / 150}, is baked into two small dynamic textures whenever the synced {@code chainedTime} changes and drawn with the
 * vanilla eyes program (see {@link TCRenderTypes#ethereal}). Spec: docs/design/5b-peeler-shackles-render.md section 2.
 * Development override: system property {@code thaumicconcilium.chained} freezes chainedTime (render harness only).
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID, value = Dist.CLIENT)
public final class ShacklesFirstPerson {
    private static final ResourceLocation SHACKLES_DYN = ThaumicConcilium.id("dynamic/shackles_ethereal");
    private static final ResourceLocation CHAIN_DYN = ThaumicConcilium.id("dynamic/chain_ethereal");

    private static DynamicTexture shacklesTex;
    private static DynamicTexture chainTex;
    private static NativeImage shacklesSrc;
    private static NativeImage chainSrc;
    private static int baked = -1;
    private static boolean mainSeen;

    private ShacklesFirstPerson() {
    }

    /** The local player's chained ticks (or the harness override). */
    static int chainedTime() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return 0;
        }
        String dev = System.getProperty("thaumicconcilium.chained");
        if (dev != null) {
            try {
                return Integer.parseInt(dev.trim());
            } catch (NumberFormatException ignored) {
                // fall through to the real value
            }
        }
        return CrimsonState.get(player).chainedTime();
    }

    /** True while the chained hand replaces every vanilla hand render. */
    public static boolean active() {
        return chainedTime() > 0 && !Minecraft.getInstance().player.isInvisible();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        int chained = chainedTime();
        if (player == null || chained <= 0) {
            mainSeen = false;
            return;
        }
        event.setCanceled(true);
        if (player.isInvisible() || !(mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) {
            return;
        }
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            mainSeen = true;
        } else {
            boolean skip = mainSeen;
            mainSeen = false;
            if (skip) {
                return;
            }
        }
        bake(chained);
        PoseStack pose = event.getPoseStack();
        var buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(1.0F));
        pose.pushPose(); // arm on the right of the screen
        pose.translate(0.53F, -0.8F, -0.8F);
        pose.mulPose(Axis.XP.rotationDegrees(-20.0F));
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(-5.0F));
        pose.translate(0.5F, 0.0F, 0.1F);
        renderer.renderRightHand(pose, buffers, light, player);
        pose.popPose();
        pose.pushPose(); // arm on the left of the screen: the right arm again, as the original
        pose.translate(-0.05F, -0.8F, -0.8F);
        pose.mulPose(Axis.ZP.rotationDegrees(-5.0F));
        pose.mulPose(Axis.XP.rotationDegrees(-30.0F));
        renderer.renderRightHand(pose, buffers, light, player);
        pose.popPose();
        pose.pushPose();
        pose.translate(0.0F, 0.0F, -1.2F);
        pose.mulPose(Axis.XP.rotationDegrees(-30.0F));
        float k = chained < 10 ? (10 - chained) / 30.0F : 0.0F; // integer ticks: 0.033 .. 0.3
        pose.scale(1.05F + k, 1.05F, 1.05F + k);
        ObjMesh.getFlat(EtherealShacklesRenderer.SHACKLES_OBJ).render(pose,
                buffers.getBuffer(TCRenderTypes.ethereal(SHACKLES_DYN)), 0xFFFFFFFF, light);
        ObjMesh.getFlat(EtherealShacklesRenderer.CHAIN_OBJ).render(pose,
                buffers.getBuffer(TCRenderTypes.ethereal(CHAIN_DYN)), 0xFFFFFFFF, light);
        pose.popPose();
        pose.popPose();
    }

    /** Rewrites both dynamic textures when chainedTime changed since the last bake (at most 20 times a second, only while chained). */
    private static void bake(int chained) {
        if (chained == baked && shacklesTex != null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (shacklesTex == null) {
            shacklesSrc = read(EtherealShacklesRenderer.SHACKLES_TEX);
            chainSrc = read(EtherealShacklesRenderer.CHAIN_TEX);
            shacklesTex = new DynamicTexture(new NativeImage(shacklesSrc.getWidth(), shacklesSrc.getHeight(), true));
            chainTex = new DynamicTexture(new NativeImage(chainSrc.getWidth(), chainSrc.getHeight(), true));
            mc.getTextureManager().register(SHACKLES_DYN, shacklesTex);
            mc.getTextureManager().register(CHAIN_DYN, chainTex);
        }
        float t = Math.max(0.0F, Math.min(1.0F, (100 - chained) / 150.0F));
        mix(shacklesSrc, shacklesTex, t);
        mix(chainSrc, chainTex, t);
        baked = chained;
    }

    private static void mix(NativeImage src, DynamicTexture dst, float t) {
        NativeImage out = dst.getPixels();
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int p = src.getPixelRGBA(x, y); // ABGR
                float r = (p & 0xFF) / 255.0F;
                float g = ((p >> 8) & 0xFF) / 255.0F;
                float b = ((p >> 16) & 0xFF) / 255.0F;
                float a = ((p >>> 24) & 0xFF) / 255.0F;
                a = a + (0.5F - a) * t;
                int px;
                if (a <= 0.1F) {
                    px = 0; // dropped: rgb 0 too, SRC_COLOR would still add its colour
                } else {
                    r = r + (0.0F - r) * t;
                    g = g + (0.749F - g) * t;
                    b = b + (1.0F - b) * t;
                    px = (Math.round(a * 255) << 24) | (Math.round(b * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(r * 255);
                }
                out.setPixelRGBA(x, y, px);
            }
        }
        dst.upload();
    }

    private static NativeImage read(ResourceLocation location) {
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(location)) {
            return NativeImage.read(in);
        } catch (IOException e) {
            ThaumicConcilium.LOG.warn("Missing shackles texture {}", location, e);
            return new NativeImage(1, 1, true);
        }
    }

    /** Dropped on a resource reload so the textures are rebuilt from the new sources. */
    static void reset() {
        baked = -1;
        if (shacklesTex != null) {
            shacklesTex.close();
            chainTex.close();
            shacklesTex = null;
            chainTex = null;
        }
        if (shacklesSrc != null) {
            shacklesSrc.close();
            chainSrc.close();
            shacklesSrc = null;
            chainSrc = null;
        }
    }
}
