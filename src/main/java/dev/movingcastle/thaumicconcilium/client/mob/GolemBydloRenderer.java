package dev.movingcastle.thaumicconcilium.client.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.GolemBydlo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Golem Bydlo (phase 8): our own humanoid model ({@link GolemBydloModel}) and texture (the original's were Thaumcraft 4's).
 */
public final class GolemBydloRenderer extends EntityRenderer<GolemBydlo> {
    public static final ResourceLocation TEXTURE = ThaumicConcilium.id("textures/entity/golem_bydlo.png");
    /** The model is 39 pixels from foot to crown and 26 across the shoulders: 1.4 makes 3.4 x 2.3 blocks. */
    public static final float SCALE_Y = 1.4F;
    public static final float SCALE_XZ = 1.4F;

    private final GolemBydloModel model;

    public GolemBydloRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new GolemBydloModel(context.bakeLayer(GolemBydloModel.LAYER));
        this.shadowRadius = 1.4F;
    }

    @Override
    public ResourceLocation getTextureLocation(GolemBydlo entity) {
        return TEXTURE;
    }

    @Override
    public void render(GolemBydlo entity, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float bodyRot = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot) - bodyRot;
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        float walkPos = entity.walkAnimation.position(partialTick);
        float walkSpeed = Math.min(1.0F, entity.walkAnimation.speed(partialTick));
        float raise = entity.actionAnim > 0 ? Mth.sin((entity.actionAnim - partialTick) / GolemBydlo.ACTION_TICKS * Mth.PI) : 0.0F;

        this.model.pose(entity.tickCount + partialTick, walkPos, walkSpeed, headYaw, pitch, raise, entity.isVehicle());

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - bodyRot));
        if (entity.deathTime > 0) {
            float fall = Mth.sqrt((entity.deathTime + partialTick - 1.0F) / 20.0F * 1.6F);
            pose.mulPose(Axis.ZP.rotationDegrees(Math.min(fall, 1.0F) * 90.0F));
        }
        pose.scale(-SCALE_XZ, -SCALE_Y, SCALE_XZ);
        pose.translate(0.0F, -1.501F, 0.0F);
        int overlay = OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(entity.hurtTime > 0 || entity.deathTime > 0));
        this.model.root().render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay);
        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffers, light);
    }
}
