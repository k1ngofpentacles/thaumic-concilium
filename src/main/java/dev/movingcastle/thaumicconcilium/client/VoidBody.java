package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * The void look of the Protobody chain: vanilla humanoid geometry (the player model's own parts, nothing of ours or the original's
 * art) and a plain cube, drawn with the vanilla end portal render type, which is the starfield the original's end shader showed.
 * Original: ProtobodyRenderer, ProtolimbRenderer (types 0 head, 1 left arm, 2 right arm, 3 body, 4 left leg, 5 right leg), SolidVoidTileRenderer.
 */
public final class VoidBody {
    /** Model part names by Protolimb type. */
    private static final String[] PARTS = {"head", "left_arm", "right_arm", "body", "left_leg", "right_leg"};
    /** Centre of each part's box in pixels from its pivot (wide player model), so a limb can turn about its middle. */
    private static final float[][] CENTRE = {{0, -4, 0}, {1, 4, 0}, {-1, 4, 0}, {0, 6, 0}, {0, 6, 0}, {0, 6, 0}};

    private final ModelPart root;
    private final boolean centred;

    /** {@code centred}: each part is moved so its box centre is the origin (for limbs); otherwise the figure stands in its own pose. */
    public VoidBody(EntityModelSet models, boolean centred) {
        this.root = models.bakeLayer(ModelLayers.PLAYER);
        this.centred = centred;
        if (centred) {
            for (int i = 0; i < PARTS.length; i++) {
                this.root.getChild(PARTS[i]).setPos(-CENTRE[i][0], -CENTRE[i][1], -CENTRE[i][2]);
            }
        }
    }

    /** The whole figure, upright, 32 pixels (2 model units) tall with its middle at the origin: scale it as the caller needs. */
    public void drawFigure(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(180.0F)); // model space is y-down
        pose.translate(0.0F, -0.5F, 0.0F);
        VertexConsumer out = buffers.getBuffer(RenderType.endPortal());
        for (String name : PARTS) {
            this.root.getChild(name).render(pose, out, light, overlay, 0xFFFFFFFF);
        }
        pose.popPose();
    }

    /** One limb turning about its middle, with the original's three additive halo shells (scale 1.1 to the q) drawn first. */
    public void drawLimb(int type, float degrees, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ModelPart part = this.root.getChild(PARTS[Math.max(0, Math.min(5, type))]);
        pose.pushPose();
        pose.mulPose(new Quaternionf().rotationAxis(degrees * ((float) Math.PI / 180.0F), 0.70710678F, 0.0F, 0.70710678F));
        pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
        VertexConsumer halo = buffers.getBuffer(TCRenderTypes.PEELER_HALO);
        for (int q = 0; q < 3; q++) {
            pose.pushPose();
            float s = (float) Math.pow(1.1, q);
            pose.scale(s, s, s);
            part.render(pose, halo, light, overlay, 0xFFFFFFFF);
            pose.popPose();
        }
        part.render(pose, buffers.getBuffer(RenderType.endPortal()), light, overlay, 0xFFFFFFFF);
        pose.popPose();
    }

    // ---------------------------------------------------------------- the cube (vanilla TheEndPortalRenderer's quads)

    /** Draws a unit cube at the origin, every face (the item, which has no neighbours). */
    public static void cube(PoseStack pose, MultiBufferSource buffers) {
        VertexConsumer out = buffers.getBuffer(RenderType.endPortal());
        Matrix4f m = pose.last().pose();
        for (Direction d : Direction.values()) {
            face(m, out, d);
        }
    }

    /** Draws a block's cube, leaving out faces another block covers. */
    public static void block(BlockState state, BlockGetter level, net.minecraft.core.BlockPos pos, PoseStack pose, MultiBufferSource buffers) {
        VertexConsumer out = buffers.getBuffer(RenderType.endPortal());
        Matrix4f m = pose.last().pose();
        for (Direction d : Direction.values()) {
            if (Block.shouldRenderFace(state, level, pos, d, pos.relative(d))) {
                face(m, out, d);
            }
        }
    }

    private static void face(Matrix4f m, VertexConsumer out, Direction d) {
        switch (d) {
            case SOUTH -> quad(m, out, 0, 1, 0, 1, 1, 1, 1, 1);
            case NORTH -> quad(m, out, 0, 1, 1, 0, 0, 0, 0, 0);
            case EAST -> quad(m, out, 1, 1, 1, 0, 0, 1, 1, 0);
            case WEST -> quad(m, out, 0, 0, 0, 1, 0, 1, 1, 0);
            case DOWN -> quad(m, out, 0, 1, 0, 0, 0, 0, 1, 1);
            case UP -> quad(m, out, 0, 1, 1, 1, 1, 1, 0, 0);
        }
    }

    /** Same argument order as vanilla's {@code renderFace}: x0 x1 y0 y1 z0 z1 z2 z3. */
    private static void quad(Matrix4f m, VertexConsumer out, float x0, float x1, float y0, float y1, float z0, float z1, float z2, float z3) {
        out.addVertex(m, x0, y0, z0);
        out.addVertex(m, x1, y0, z1);
        out.addVertex(m, x1, y1, z2);
        out.addVertex(m, x0, y1, z3);
    }
}
