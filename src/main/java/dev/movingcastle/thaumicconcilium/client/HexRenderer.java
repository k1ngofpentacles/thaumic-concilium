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
    /** Phase 5c: the rings' stone by crack state (0 whole, 1 to 3 cracked; plain retextures for now). */
    private static final ResourceLocation[] RUNES_BY_CRACK = {RUNES_TEX,
            ThaumicConcilium.id("textures/block/hex/runes_crack1.png"), ThaumicConcilium.id("textures/block/hex/runes_crack2.png"),
            ThaumicConcilium.id("textures/block/hex/runes_crack3.png")};
    private static final ResourceLocation INK_TEX = ThaumicConcilium.id("textures/block/hex/runes_ink.png");

    public HexRenderer(BlockEntityRendererProvider.Context context) {
    }

    /**
     * The hex as the break sequence's stand-in (phase 5c cycle 3c; drawn by the Vessel's renderer when no Sable sub-level plays it):
     * origin at the middle of the hex's base, fully cracked, the ink going from rift purple to white light as {@code crack} goes 0 to 1.
     */
    /** Set each frame by the Vessel's renderer while a break cracks: how far (0..1) and the game time it was set. */
    static volatile float breakCrack;
    static volatile long breakCrackAt = Long.MIN_VALUE / 2;

    /** Rift purple going to white light as the hex cracks. */
    private static int breakingInk(float crack) {
        return 0xFF000000 | ((int) (0x6A + (0xFF - 0x6A) * crack) << 16) | ((int) (0xE6 * crack) << 8) | (int) (0x6A + (0xFF - 0x6A) * crack);
    }

    static void drawBreaking(PoseStack pose, MultiBufferSource buffers, int light, float crack, float ticks) {
        // the ink as a hex holding a rift shows it (purple, full bright, the rings turning against each other), whitening as it cracks
        int ink = breakingInk(crack);
        int inkLight = 0xF000F0;
        mesh("hex_caps").render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(CAPS_TEX)), 0xFFFFFFFF, light);
        for (int i = 1; i <= 3; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees((i == 2 ? -1 : 1) * (ticks % 360.0F)));
            ObjMesh ring = mesh("hex" + i);
            ring.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(RUNES_BY_CRACK[3])), 0xFFFFFFFF, light);
            ring.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(INK_TEX)), ink, inkLight);
            pose.popPose();
        }
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
        int crack = hex.getBlockState().hasProperty(dev.movingcastle.thaumicconcilium.block.HexBlock.CRACK)
                ? hex.getBlockState().getValue(dev.movingcastle.thaumicconcilium.block.HexBlock.CRACK) : 0;
        // Original shader: ink = mix(black, purple, progress) / 1.2.
        int ink = rift ? 0xFF6A006A : 0xFF151515;
        int inkLight = rift ? 0xF000F0 : light;
        if (hex.inert() && hex.getLevel() != null && Math.abs(hex.getLevel().getGameTime() - breakCrackAt) <= 2L && breakCrack > 0.0F) {
            ink = breakingInk(breakCrack); // riding a Sable sub-level through the break: the runes whiten as it cracks
            inkLight = 0xF000F0;
        }
        pose.pushPose();
        pose.translate(0.5, 0.0, 0.5);
        mesh("hex_caps").render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(CAPS_TEX)), 0xFFFFFFFF, light);
        for (int i = 1; i <= 3; i++) {
            pose.pushPose();
            if (rift) {
                pose.mulPose(Axis.YP.rotationDegrees((i == 2 ? -1 : 1) * (ticks % 360.0F)));
            }
            ObjMesh ring = mesh("hex" + i);
            ring.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(RUNES_BY_CRACK[crack])), 0xFFFFFFFF, light);
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
