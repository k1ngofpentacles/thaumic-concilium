package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.movingcastle.thaumicconcilium.entity.MaterialPeeler;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

/**
 * The Peeler of Matter: a 1x1x1 cube that is a window onto the end-portal starfield, turning 10 degrees a second about the
 * diagonal (0.7071, 0, 0.7071), inside three additive halo shells (original MaterialPeelerRenderer:33-54). Spec:
 * docs/design/5b-peeler-shackles-render.md section 1 (D1: vanilla end-portal stars). No per-tick work, nothing allocated per frame.
 */
public class MaterialPeelerRenderer extends EntityRenderer<MaterialPeeler> {
    public MaterialPeelerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(MaterialPeeler entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float angle = (entity.tickCount + partialTick) * 0.5F + entity.innerRotation() * 0.5F;
        pose.pushPose();
        pose.translate(-0.5F, 0.0F, -0.5F);
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(new Quaternionf().rotationAxis(angle * Mth.DEG_TO_RAD, 0.70710678F, 0.0F, 0.70710678F));
        pose.translate(-0.5F, -0.5F, -0.5F);
        // Halo first (the shells must not brighten the faces the core covers), then the opaque core.
        VertexConsumer halo = buffers.getBuffer(TCRenderTypes.PEELER_HALO);
        for (int q = 0; q < 3; q++) {
            pose.pushPose();
            pose.translate(-0.1F, -0.1F, -0.1F);
            float s = (float) Math.pow(1.1, q);
            pose.scale(s, s, s);
            cube(pose, halo);
            pose.popPose();
        }
        cube(pose, buffers.getBuffer(RenderType.endPortal()));
        pose.popPose();
    }

    /** A unit box, six outward-facing (counter-clockwise) quads. */
    private static void cube(PoseStack pose, VertexConsumer out) {
        var m = pose.last().pose();
        quad(m, out, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1); // down
        quad(m, out, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0); // up
        quad(m, out, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0); // north
        quad(m, out, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1); // south
        quad(m, out, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0); // west
        quad(m, out, 1, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1); // east
    }

    private static void quad(org.joml.Matrix4f m, VertexConsumer out, float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        out.addVertex(m, x0, y0, z0);
        out.addVertex(m, x1, y1, z1);
        out.addVertex(m, x2, y2, z2);
        out.addVertex(m, x3, y3, z3);
    }

    @Override
    public ResourceLocation getTextureLocation(MaterialPeeler entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
