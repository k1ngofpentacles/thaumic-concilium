package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToIntFunction;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

/**
 * A camera-facing animated sprite, used where the original drew a billboard from Thaumcraft 4's particle
 * atlas. We use Thaumaturge's own 32-frame {@code sparkle} strip (Thaumaturge is a hard dependency, so the
 * texture is always present; nothing from TC4 is bundled).
 */
public class SparkleBillboardRenderer<T extends Entity> extends EntityRenderer<T> {
    public static final ResourceLocation SPARKLE = ResourceLocation.fromNamespaceAndPath("thaumaturge", "textures/particle/sparkle.png");
    private static final int FRAMES = 32;

    private final ToIntFunction<T> argb;
    private final ToDoubleBiFunction<T, Float> scale;
    private final boolean additive;

    public SparkleBillboardRenderer(EntityRendererProvider.Context context, ToIntFunction<T> argb,
                                    ToDoubleBiFunction<T, Float> scale, boolean additive) {
        super(context);
        this.argb = argb;
        this.scale = scale;
        this.additive = additive;
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(T entity, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0.0, entity.getBbHeight() / 2.0, 0.0);
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        float s = (float) this.scale.applyAsDouble(entity, partialTicks);
        pose.scale(s, s, s);

        int frame = (int) ((entity.tickCount + entity.getId()) % FRAMES);
        float u0 = frame / (float) FRAMES;
        float u1 = (frame + 1) / (float) FRAMES;
        int c = this.argb.applyAsInt(entity);

        RenderType type = this.additive ? RenderType.eyes(SPARKLE) : RenderType.entityTranslucentEmissive(SPARKLE);
        VertexConsumer vc = buffers.getBuffer(type);
        Matrix4f m = pose.last().pose();
        int full = 0xF000F0;
        vertex(vc, m, pose, -0.5F, -0.5F, u0, 1.0F, c, full);
        vertex(vc, m, pose, 0.5F, -0.5F, u1, 1.0F, c, full);
        vertex(vc, m, pose, 0.5F, 0.5F, u1, 0.0F, c, full);
        vertex(vc, m, pose, -0.5F, 0.5F, u0, 0.0F, c, full);
        pose.popPose();
        super.render(entity, yaw, partialTicks, pose, buffers, light);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, PoseStack pose, float x, float y, float u, float v, int argb, int light) {
        vc.addVertex(m, x, y, 0.0F)
                .setColor(argb)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose.last(), 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return SPARKLE;
    }
}
