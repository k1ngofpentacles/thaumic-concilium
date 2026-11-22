package dev.movingcastle.thaumicconcilium.client;

import com.leclowndu93150.thaumaturge.client.effect.LateWorldRenderQueue;
import com.leclowndu93150.thaumaturge.client.render.TTRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.movingcastle.thaumicconcilium.entity.mob.BrightestOne;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The Brightest One (original {@code BrightestOneRenderer}): two camera-facing additive billboards at its centre. A big wisp sprite
 * (half-size 3.5) tinted by its aspect, red-tinted when it was just short-circuited, and a small white flickering core (half-size
 * 0.4 + 0.1 sin((age + partial) / 10)). Both full bright. The sprites are Thaumaturge's own wisp frames (as its
 * wisps use), since Thaumcraft 4's sheets are not ours to ship.
 */
public class BrightestOneRenderer extends EntityRenderer<BrightestOne> {
    private static final ResourceLocation NODES = ResourceLocation.fromNamespaceAndPath("thaumaturge", "textures/misc/auranodes.png");
    private static final ResourceLocation HALO = ResourceLocation.fromNamespaceAndPath("thaumicconcilium", "textures/misc/brightest_halo.png");
    private static final int GRID = 32;
    private static final int FRAME_START = 800;
    private static final int LIGHT = LightTexture.pack(15, 15);

    public BrightestOneRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(BrightestOne entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(BrightestOne entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (entity.getHealth() <= 0.0F) {
            return;
        }
        int color = entity.color();
        if (entity.hurtTime > 0) {
            int g = (int) ((entity.color() >> 8 & 255) / 300.0F * 255.0F);
            int b = (int) ((entity.color() & 255) / 300.0F * 255.0F);
            color = 0xFF0000 | g << 8 | b;
        }
        final int tint = color;
        int frame = FRAME_START + entity.tickCount % 16;
        float core = 2.0F * (0.4F + 0.1F * Mth.sin((entity.tickCount + partialTick) / 10.0F));
        Vec3 origin = entity.getPosition(partialTick).add(0.0, 1.5, 0.0);
        RenderType type = TTRenderTypes.fxAdditiveAlphaTest(NODES);
        RenderType haloType = TTRenderTypes.fxAdditiveAlphaTest(HALO);
        float pulse = 1.0F + 0.04F * Mth.sin((entity.tickCount + partialTick) / 8.0F);
        LateWorldRenderQueue.enqueue(origin, (latePose, lateBuffers) -> {
            latePose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
            quad(lateBuffers, latePose, haloType, 0, 1, 7.0F * pulse, tint, 1.0F);
            quad(lateBuffers, latePose, type, frame, GRID, core, 0xFFFFFF, 1.0F);
        });
    }

    private static void quad(MultiBufferSource buffers, PoseStack pose, RenderType type, int frame, int grid, float scale, int color, float intensity) {
        float step = 1.0F / grid;
        float u0 = (frame % grid) * step;
        float v0 = (frame / grid) * step;
        float u1 = u0 + step;
        float v1 = v0 + step;
        float half = scale * 0.5F;
        int tint = FastColor.ARGB32.colorFromFloat(1.0F, FastColor.ARGB32.red(color) / 255.0F * intensity, FastColor.ARGB32.green(color) / 255.0F * intensity,
                FastColor.ARGB32.blue(color) / 255.0F * intensity);
        VertexConsumer buffer = buffers.getBuffer(type);
        Matrix4f m = pose.last().pose();
        buffer.addVertex(m, -half, -half, 0.0F).setUv(u1, v1).setColor(tint).setLight(LIGHT);
        buffer.addVertex(m, -half, half, 0.0F).setUv(u1, v0).setColor(tint).setLight(LIGHT);
        buffer.addVertex(m, half, half, 0.0F).setUv(u0, v0).setColor(tint).setLight(LIGHT);
        buffer.addVertex(m, half, -half, 0.0F).setUv(u0, v1).setColor(tint).setLight(LIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(BrightestOne entity) {
        return NODES;
    }
}
