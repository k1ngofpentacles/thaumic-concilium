package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.block.HexBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

/**
 * The Hex of Predictability (original: {@code HexOfPredictabilityTileRenderer}, WTFPL models): a cap plate and three
 * rune rings over the 3x3, drawn by the centre block from the original OBJ meshes (their rune texture repeats around
 * the rings, so they're drawn with their own textures rather than baked into the block atlas). While the hex holds a
 * rift the rings turn, the middle one the other way, and the runes' ink glows purple (the original's shader recoloured
 * the ink from black to purple; here the ink is its own layer, tinted).
 */
public class HexRenderer implements BlockEntityRenderer<HexBlockEntity> {
    private static final ResourceLocation CAPS_TEX = ThaumicConcilium.id("textures/block/hex/caps.png");
    private static final ResourceLocation RUNES_TEX = ThaumicConcilium.id("textures/block/hex/runes.png");
    private static final ResourceLocation INK_TEX = ThaumicConcilium.id("textures/block/hex/runes_ink.png");

    public HexRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static ObjMesh mesh(String name) {
        return ObjMesh.get(ThaumicConcilium.id("models/block/hex/" + name + ".obj"));
    }

    @Override
    public void render(HexBlockEntity hex, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!hex.isMaster()) {
            return;
        }
        float ticks = hex.getLevel() == null ? 0.0F : hex.getLevel().getGameTime() + partialTick;
        boolean rift = hex.hasRift();
        // Original shader: ink = mix(black, purple, progress) / 1.2.
        int ink = rift ? 0xFF6A006A : 0xFF151515;
        int inkLight = rift ? 0xF000F0 : light;
        pose.pushPose();
        pose.translate(0.5, 0.0, 0.5);
        mesh("hex_caps").render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(CAPS_TEX)), 0xFFFFFFFF, light);
        for (int i = 1; i <= 3; i++) {
            pose.pushPose();
            if (rift) {
                pose.mulPose(Axis.YP.rotationDegrees((i == 2 ? -1 : 1) * (ticks % 360.0F)));
            }
            ObjMesh ring = mesh("hex" + i);
            ring.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(RUNES_TEX)), 0xFFFFFFFF, light);
            ring.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(INK_TEX)), ink, inkLight);
            pose.popPose();
        }
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(HexBlockEntity hex) {
        return new AABB(hex.getBlockPos()).inflate(2.0, 1.0, 2.0);
    }
}
