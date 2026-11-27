package dev.movingcastle.thaumicconcilium.client.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.mob.BurnedWitch;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Original BurnedWitchRenderer: a bare biped model on her own skin, with the Witchery broom under her. The broom is the remake's own
 * model layer ({@code witchery:broom#main}) and texture, referenced by id at runtime: nothing of it is copied, and no
 * {@code dev.sterner} class is named. It is drawn the way the remake's broom renderer does (no living-entity flip), after the body.
 * Look memo: docs/design/phase10-look-prebrief.md 3.
 */
public final class BurnedWitchRenderer extends MobRenderer<BurnedWitch, BurnedWitchModel<BurnedWitch>> {
    private static final ResourceLocation TEXTURE = ThaumicConcilium.id("textures/entity/burned_witch.png");
    private static final ResourceLocation BROOM_TEXTURE = ResourceLocation.fromNamespaceAndPath("witchery", "textures/entity/broom.png");
    private static final ModelLayerLocation BROOM_LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("witchery", "broom"), "main");
    /**
     * Where the broom hangs under her feet, in blocks. The remake's own renderer uses -1.0; its handle then sits 0.28 above the
     * entity origin, the original's sat 0.6125 above her feet (24 - 14.2 pixels), hence -0.67. Tune only within 0.06.
     */
    public static final double BROOM_Y = -0.64;

    private final ModelPart broom;

    public BurnedWitchRenderer(EntityRendererProvider.Context context) {
        super(context, new BurnedWitchModel<>(context.bakeLayer(TCModelLayers.BURNED_WITCH)), 0.5F);
        this.broom = context.bakeLayer(BROOM_LAYER);
    }

    @Override
    public void render(BurnedWitch entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partialTick, pose, buffers, light);
        if (entity.isInvisible()) {
            return;
        }
        pose.pushPose();
        pose.translate(0.0, BROOM_Y, 0.0);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot)));
        this.broom.render(pose, buffers.getBuffer(RenderType.entityTranslucent(BROOM_TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(BurnedWitch entity) {
        return TEXTURE;
    }
}
