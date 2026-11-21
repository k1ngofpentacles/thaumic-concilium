package dev.movingcastle.thaumicconcilium.client;

import com.leclowndu93150.thaumaturge.client.effect.LateWorldRenderQueue;
import com.leclowndu93150.thaumaturge.client.render.TTRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.movingcastle.thaumicconcilium.entity.ChargedWisp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * A charged wisp looks like the Thaumaturge wisp it was (the original drew Thaumcraft 4's wisp): a white core in a halo
 * of its aspect's colour from Thaumaturge's aura node sheet, used at runtime from Thaumaturge's jar. The charge shows
 * as a larger, brighter halo, and the arcs to whatever it hurts.
 */
public class ChargedWispRenderer extends EntityRenderer<ChargedWisp> {
    private static final ResourceLocation NODES = ResourceLocation.fromNamespaceAndPath("thaumaturge", "textures/misc/auranodes.png");
    private static final int GRID = 32;
    private static final int FRAME_START = 800;
    private static final int LIGHT = LightTexture.pack(14, 0);

    public ChargedWispRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(ChargedWisp entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        int color = entity.color();
        int frame = FRAME_START + entity.tickCount % 16;
        Vec3 origin = entity.getPosition(partialTick).add(0.0, entity.getBbHeight() / 2.0, 0.0);
        RenderType type = TTRenderTypes.fxAdditiveAlphaTest(NODES);
        LateWorldRenderQueue.enqueue(origin, (latePose, lateBuffers) -> {
            latePose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
            quad(lateBuffers, latePose, type, frame, 1.0F, color, 0.55F);
            quad(lateBuffers, latePose, type, frame, 0.5F, 0xFFFFFF, 0.95F);
        });
    }

    private static void quad(MultiBufferSource buffers, PoseStack pose, RenderType type, int frame, float scale, int color, float alpha) {
        float step = 1.0F / GRID;
        float u0 = (frame % GRID) * step;
        float v0 = (frame / GRID) * step;
        float u1 = u0 + step;
        float v1 = v0 + step;
        float half = scale * 0.5F;
        int tint = FastColor.ARGB32.colorFromFloat(alpha, FastColor.ARGB32.red(color) / 255.0F, FastColor.ARGB32.green(color) / 255.0F,
                FastColor.ARGB32.blue(color) / 255.0F);
        VertexConsumer buffer = buffers.getBuffer(type);
        Matrix4f m = pose.last().pose();
        buffer.addVertex(m, -half, -half, 0.0F).setUv(u1, v1).setColor(tint).setLight(LIGHT);
        buffer.addVertex(m, -half, half, 0.0F).setUv(u1, v0).setColor(tint).setLight(LIGHT);
        buffer.addVertex(m, half, half, 0.0F).setUv(u0, v0).setColor(tint).setLight(LIGHT);
        buffer.addVertex(m, half, -half, 0.0F).setUv(u0, v1).setColor(tint).setLight(LIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(ChargedWisp entity) {
        return NODES;
    }
}
