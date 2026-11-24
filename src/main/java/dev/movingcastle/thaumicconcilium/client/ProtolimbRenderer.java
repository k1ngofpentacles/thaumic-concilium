package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.movingcastle.thaumicconcilium.entity.mob.Protolimb;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * A Protolimb: one humanoid part of the vanilla player model turning about its middle on the diagonal axis (a degree a tick, as
 * the original's {@code innerRotation}), drawn in the end portal's starfield inside three additive halo shells. Original:
 * ProtolimbRenderer. No per-tick work; the model is baked once per renderer.
 */
public class ProtolimbRenderer extends EntityRenderer<Protolimb> {
    private final VoidBody body;

    public ProtolimbRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.body = new VoidBody(context.getModelSet(), true);
    }

    @Override
    public void render(Protolimb limb, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0.0F, limb.getBbHeight() / 2.0F, 0.0F);
        body.drawLimb(limb.limbType(), limb.innerRotation + partialTick, pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(Protolimb limb) {
        return TCRenderTypes.PEELER_HALO_TEXTURE;
    }
}
