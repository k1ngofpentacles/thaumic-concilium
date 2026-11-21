package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.ConcentratedWarpCharge;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/**
 * The original's warp cube: two translucent 16-pixel cubes, a full-bright core inside a lit shell, breathing slowly.
 * Under self-flagellation the shell pulses dark.
 */
public class WarpChargeRenderer extends EntityRenderer<ConcentratedWarpCharge> {
    private static final ResourceLocation TEXTURE = ThaumicConcilium.id("textures/entity/warp_cube.png");
    private final ModelPart core;
    private final ModelPart shell;

    public WarpChargeRenderer(EntityRendererProvider.Context context) {
        super(context);
        ModelPart root = createLayer().bakeRoot();
        this.core = root.getChild("core");
        this.shell = root.getChild("shell");
    }

    private static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -8.0F, -8.0F, 16, 16, 16), PartPose.ZERO);
        root.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 32).addBox(-8.0F, -8.0F, -8.0F, 16, 16, 16), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void render(ConcentratedWarpCharge entity, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        float t = entity.tickCount + partialTicks;
        float pulse = 0.025F * Mth.sin(t * 0.075F);
        float dark = entity.mode() == ConcentratedWarpCharge.MODE_FLAGELLATION ? Math.max(0.0F, Mth.sin(t * 0.075F)) : 0.0F;
        float shade = 1.0F - dark;
        int alpha = (int) (255 * Mth.clamp(0.5F + pulse * 10.0F, 0.0F, 1.0F));
        int color = FastColor.ARGB32.color(alpha, (int) (255 * shade), (int) (255 * shade), (int) (255 * shade));

        pose.pushPose();
        pose.translate(0.0, 0.5, 0.0);
        pose.scale(1.0F - pulse, 1.0F - pulse, 1.0F - pulse);
        this.core.xRot = 0.02F * Mth.sin(t * 0.05F);
        this.core.zRot = 0.02F * Mth.sin(t * 0.1F);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        pose.pushPose();
        pose.scale(0.9F, 0.9F, 0.9F);
        this.core.render(pose, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
        pose.popPose();
        this.shell.render(pose, vc, light, OverlayTexture.NO_OVERLAY, color);
        pose.popPose();
        super.render(entity, yaw, partialTicks, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ConcentratedWarpCharge entity) {
        return TEXTURE;
    }
}
