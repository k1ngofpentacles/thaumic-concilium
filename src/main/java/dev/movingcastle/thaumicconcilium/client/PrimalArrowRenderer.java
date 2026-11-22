package dev.movingcastle.thaumicconcilium.client;

import com.leclowndu93150.thaumaturge.api.aspect.Aspects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.PrimalArrow;
import dev.movingcastle.thaumicconcilium.entity.PrimalType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * The vanilla arrow model in the vanilla arrow texture, tinted with the primal's aspect colour, plus a small camera-facing glow
 * (additive; plain alpha for perditio, whose dark colour would vanish when added). Two quads per arrow, nothing cached per frame.
 */
public class PrimalArrowRenderer extends ArrowRenderer<PrimalArrow> {
    private static final ResourceLocation ARROW = ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png");
    private static final ResourceLocation GLOW = ThaumicConcilium.id("textures/entity/primal_arrow_glow.png");

    /** Colour of the arrow being drawn (the renderer is single-threaded); read by {@link #vertex}. */
    private int tint = 0xFFFFFFFF;

    public PrimalArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    private static int colorOf(PrimalArrow arrow) {
        PrimalType kind = arrow.primalType();
        var holder = Aspects.resolve(arrow.level(), kind.primal());
        return holder == null ? 0xFFFFFF : holder.value().color() & 0xFFFFFF;
    }

    @Override
    public void render(PrimalArrow arrow, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        int rgb = colorOf(arrow);
        tint = 0xFF000000 | rgb;
        super.render(arrow, yaw, partialTicks, pose, buffers, light);

        pose.pushPose();
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        pose.scale(0.45F, 0.45F, 0.45F);
        boolean additive = arrow.primalType() != PrimalType.PERDITIO;
        VertexConsumer vc = buffers.getBuffer(additive ? RenderType.eyes(GLOW) : RenderType.entityTranslucentEmissive(GLOW));
        Matrix4f m = pose.last().pose();
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        int a = additive ? 190 : 150;
        glow(vc, m, pose, -0.5F, -0.5F, 0.0F, 1.0F, r, g, b, a);
        glow(vc, m, pose, 0.5F, -0.5F, 1.0F, 1.0F, r, g, b, a);
        glow(vc, m, pose, 0.5F, 0.5F, 1.0F, 0.0F, r, g, b, a);
        glow(vc, m, pose, -0.5F, 0.5F, 0.0F, 0.0F, r, g, b, a);
        pose.popPose();
    }

    private static void glow(VertexConsumer vc, Matrix4f m, PoseStack pose, float x, float y, float u, float v, int r, int g, int b, int a) {
        vc.addVertex(m, x, y, 0.0F).setColor(r, g, b, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0).setNormal(pose.last(), 0.0F, 1.0F, 0.0F);
    }

    @Override
    public void vertex(PoseStack.Pose pose, VertexConsumer vc, int x, int y, int z, float u, float v, int nx, int ny, int nz, int light) {
        vc.addVertex(pose, (float) x, (float) y, (float) z).setColor(tint).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose, (float) nx, (float) nz, (float) ny);
    }

    @Override
    public ResourceLocation getTextureLocation(PrimalArrow arrow) {
        return ARROW;
    }
}
