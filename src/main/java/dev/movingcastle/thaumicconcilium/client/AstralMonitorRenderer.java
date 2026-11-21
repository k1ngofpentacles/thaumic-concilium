package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.item.AstralMonitor;
import dev.movingcastle.thaumicconcilium.lithograph.PartyStats;
import dev.movingcastle.thaumicconcilium.registry.TCDataComponents;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * The Astral Monitor (original: {@code AstralMonitorRenderer}). The body is Thaumaturge's scanner mesh, referenced by a
 * model of ours ({@code models/item/astral_monitor_frame.json}) that swaps in the original's frame texture; the
 * original used Thaumcraft's scanner OBJ. On top of it:
 * <ul>
 *   <li>the screen, a hexagon in the scanner's opening showing the End portal sky (the original's shader; here a
 *   plain tinted texture);</li>
 *   <li>in first person, while the watched player is in the world, their health, blood, mana and runic shielding as
 *   constellation glyphs hovering over the screen, from the stats the server wrote into the item.</li>
 * </ul>
 */
public final class AstralMonitorRenderer extends BlockEntityWithoutLevelRenderer {
    public static final ModelResourceLocation FRAME = ModelResourceLocation.standalone(ThaumicConcilium.id("item/astral_monitor_frame"));
    private static final ResourceLocation SCREEN = ResourceLocation.withDefaultNamespace("textures/entity/end_portal.png");
    private static final ResourceLocation CONSTELLATIONS = LithographHud.CONSTELLATIONS;
    private static final int FULL_BRIGHT = 0xF000F0;
    /** The scanner body's top is at y 0.1 and its opening is a hexagon of radius 1 (original screen.obj). */
    private static final float SCREEN_Y = 0.101F;
    private static final float[][] HEXAGON = {{0.0F, -1.0F}, {0.866F, -0.5F}, {0.866F, 0.5F}, {0.0F, 1.0F}, {-0.866F, 0.5F}, {-0.866F, -0.5F}};

    private static AstralMonitorRenderer instance;

    private AstralMonitorRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static AstralMonitorRenderer get() {
        if (instance == null) {
            instance = new AstralMonitorRenderer();
        }
        return instance;
    }

    /** Client hook that gives the item this renderer. */
    public static final class Extensions implements IClientItemExtensions {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return AstralMonitorRenderer.get();
        }
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel frame = mc.getModelManager().getModel(FRAME);
        pose.pushPose();
        // The item renderer has moved the origin to the model's corner; the scanner mesh is baked around the middle of
        // that cube, and the nested render below moves it back by half again, so go to the middle first.
        pose.translate(0.5F, 0.5F, 0.5F);
        mc.getItemRenderer().render(stack, context, false, pose, buffers, light, overlay, frame);
        this.drawScreen(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(SCREEN)));
        if (context.firstPerson() && mc.level != null) {
            this.drawGlyphs(stack, pose, buffers, mc);
        }
        pose.popPose();
    }

    private void drawScreen(PoseStack pose, VertexConsumer out) {
        PoseStack.Pose last = pose.last();
        int tint = 0xFFB070FF;
        float[] c = {0.0F, 0.0F};
        for (int i = 0; i < 6; i += 2) {
            float[] a = HEXAGON[i];
            float[] b = HEXAGON[(i + 1) % 6];
            float[] d = HEXAGON[(i + 2) % 6];
            vertex(out, last, c, tint);
            vertex(out, last, a, tint);
            vertex(out, last, b, tint);
            vertex(out, last, d, tint);
        }
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose last, float[] p, int argb) {
        out.addVertex(last, p[0], SCREEN_Y, p[1]).setColor(argb).setUv(p[0] * 0.5F + 0.5F, p[1] * 0.5F + 0.5F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(last, 0.0F, 1.0F, 0.0F);
    }

    private void drawGlyphs(ItemStack stack, PoseStack pose, MultiBufferSource buffers, Minecraft mc) {
        Optional<UUID> target = AstralMonitor.target(stack);
        PartyStats stats = stack.get(TCDataComponents.MONITOR_STATS.get());
        if (target.isEmpty() || stats == null || !stats.present() || mc.level.getPlayerByUUID(target.get()) == null) {
            return;
        }
        VertexConsumer out = buffers.getBuffer(RenderType.entityTranslucent(CONSTELLATIONS));
        pose.pushPose();
        // Original: rotate 90 about X, then 90 about Z, step half a unit back, and draw 0.02 scale sprites at z -0.1,
        // which leaves them floating just over half a unit above the scanner's centre.
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
        pose.translate(0.0F, 0.0F, -0.5F);
        pose.scale(0.02F, 0.02F, 0.02F);
        sprite(out, pose, -32, -16, 48, stats.health());
        sprite(out, pose, -10, 5, 32, stats.blood());
        sprite(out, pose, 0, -24, 16, stats.mana());
        sprite(out, pose, 16, 0, 0, stats.runes());
        pose.popPose();
    }

    /** One 16 pixel frame of the sprite sheet (original: UtilsFX.drawTexturedQuad at z -0.1). */
    private static void sprite(VertexConsumer out, PoseStack pose, int x, int y, int u, int frame) {
        if (frame < 0) {
            return;
        }
        PoseStack.Pose last = pose.last();
        float v = 16.0F * frame;
        float u0 = u / 256.0F;
        float u1 = (u + 16) / 256.0F;
        float v0 = v / 256.0F;
        float v1 = (v + 16) / 256.0F;
        float z = -0.1F;
        corner(out, last, x, y + 16, z, u0, v1);
        corner(out, last, x + 16, y + 16, z, u1, v1);
        corner(out, last, x + 16, y, z, u1, v0);
        corner(out, last, x, y, z, u0, v0);
    }

    private static void corner(VertexConsumer out, PoseStack.Pose last, float x, float y, float z, float u, float v) {
        out.addVertex(last, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT).setNormal(last, 0.0F, 0.0F, 1.0F);
    }
}
