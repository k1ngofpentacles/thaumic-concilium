package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.block.QuicksilverReactorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** The mercury surface (original: {@code QuicksilverCrucibleTileRenderer}): a quad 0.3 + permutatio/100 up. */
public class QuicksilverReactorRenderer implements BlockEntityRenderer<QuicksilverReactorBlockEntity> {
    private static final ResourceLocation TEX = ThaumicConcilium.id("textures/block/quicksilver.png");

    public QuicksilverReactorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(QuicksilverReactorBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (be.permutatio() <= 0) {
            return;
        }
        float y = 0.3F + be.permutatio() / 100.0F;
        float lo = 0.125F;
        float hi = 0.875F;
        VertexConsumer vc = buffers.getBuffer(RenderType.entitySolid(TEX));
        PoseStack.Pose last = pose.last();
        Matrix4f m = last.pose();
        quad(vc, last, m, lo, y, lo, 0, 0, light);
        quad(vc, last, m, lo, y, hi, 0, 1, light);
        quad(vc, last, m, hi, y, hi, 1, 1, light);
        quad(vc, last, m, hi, y, lo, 1, 0, light);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, Matrix4f m, float x, float y, float z, float u, float v, int light) {
        vc.addVertex(m, x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
