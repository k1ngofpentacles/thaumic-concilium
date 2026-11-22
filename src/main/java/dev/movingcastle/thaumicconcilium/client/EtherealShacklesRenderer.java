package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.EtherealShackles;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * The thrown Ethereal Shackles (original EtherealShacklesEntityRenderer:11-24): the shackles and chain meshes at half a block
 * up, spinning 10 degrees a tick about Y (interpolated here), plain textures, entity light, no entity yaw.
 */
public class EtherealShacklesRenderer extends EntityRenderer<EtherealShackles> {
    public static final ResourceLocation SHACKLES_OBJ = ThaumicConcilium.id("models/item/shackles.obj");
    public static final ResourceLocation CHAIN_OBJ = ThaumicConcilium.id("models/item/chain.obj");
    public static final ResourceLocation SHACKLES_TEX = ThaumicConcilium.id("textures/entity/shackles/shackles.png");
    public static final ResourceLocation CHAIN_TEX = ThaumicConcilium.id("textures/entity/shackles/chain.png");

    public EtherealShacklesRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(EtherealShackles entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0.0F, 0.5F, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * 10.0F));
        ObjMesh.getFlat(SHACKLES_OBJ).render(pose, buffers.getBuffer(RenderType.entityCutout(SHACKLES_TEX)), 0xFFFFFFFF, light);
        ObjMesh.getFlat(CHAIN_OBJ).render(pose, buffers.getBuffer(RenderType.entityCutout(CHAIN_TEX)), 0xFFFFFFFF, light);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EtherealShackles entity) {
        return SHACKLES_TEX;
    }
}
