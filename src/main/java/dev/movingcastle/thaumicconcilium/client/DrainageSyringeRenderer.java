package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.joml.Matrix4f;

/**
 * The Drainage Syringe: the original's OBJ (Concilium's own) in every view but first person's two-part pose
 * ({@link DrainageSyringeFirstPerson}, which calls {@link #drawHeld}). Original: DrainageSyringeRenderer (1.7.10); every
 * number below is one of its GL calls or of the 1.7.10 code that called it, none is tuned by eye. As the original did, the
 * cap and the liquid column are drawn in first person only: third person, the GUI, the ground and frames show the bare
 * syringe. The glass is translucent and drawn without culling; the meshes are lit by flat face normals.
 */
public final class DrainageSyringeRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation BODY_OBJ = ThaumicConcilium.id("models/item/syringe.obj");
    private static final ResourceLocation CAP_OBJ = ThaumicConcilium.id("models/item/syringe_cap.obj");
    private static final ResourceLocation BODY_TEX = ThaumicConcilium.id("textures/entity/syringe/syringe.png");
    private static final ResourceLocation[] CAP_TEX = {
            ThaumicConcilium.id("textures/entity/syringe/cap_vis.png"),
            ThaumicConcilium.id("textures/entity/syringe/cap_mana.png"),
            ThaumicConcilium.id("textures/entity/syringe/cap_blood.png")};
    /**
     * Liquid colours by type (vis, mana, blood) and the amount that fills the barrel (original renderLiquid :183-202). The
     * original's 0x9700C0, 0x0FD9FF and 0xFF0000 at its brightness of 200 of 240, drawn here as flat colour: each channel x 0.8.
     */
    private static final int[] LIQUID_COLOR = {0xFF79009A, 0xFF0CAECC, 0xFFCC0000};
    private static final int[] LIQUID_MAX = {100, 250000, 500000};

    private static DrainageSyringeRenderer instance;

    private DrainageSyringeRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static DrainageSyringeRenderer get() {
        if (instance == null) {
            instance = new DrainageSyringeRenderer();
        }
        return instance;
    }

    /** Client hook that gives the syringe this renderer. */
    public static final class Extensions implements IClientItemExtensions {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return DrainageSyringeRenderer.get();
        }
    }

    private static void body(PoseStack pose, MultiBufferSource buffers, int light) {
        ObjMesh.getFlat(BODY_OBJ).render(pose, buffers.getBuffer(RenderType.entityTranslucent(BODY_TEX)), 0xFFFFFFFF, light);
    }

    /**
     * First person, in the syringe's own frame (the caller has replayed the hand chain up to the original's scale 0.9).
     *
     * @param hasState false for a stack that was never used (the original's null tag): no cap, no liquid
     * @param type 0 vis, 1 mana, 2 blood
     */
    public static void drawHeld(PoseStack pose, MultiBufferSource buffers, int light, boolean hasState, int type, int amount, boolean open) {
        int t = Mth.clamp(type, 0, 2);
        if (hasState && !open) {
            ObjMesh.getFlat(CAP_OBJ).render(pose, buffers.getBuffer(RenderType.entityCutout(CAP_TEX[t])), 0xFFFFFFFF, light);
        }
        if (hasState && amount > 0) {
            pose.pushPose();
            pose.translate(-1.4F, 0.0F, 0.0F);
            pose.mulPose(Axis.ZP.rotationDegrees(60.0F));
            pose.scale(0.7F, 0.7F, 0.7F);
            float level = Math.min(1.0F, (float) amount / LIQUID_MAX[t]) * 0.625F;
            RenderType liquid = RenderType.debugQuads();
            box(pose.last().pose(), buffers.getBuffer(liquid), -0.25F, 0.0625F, -0.25F, 0.25F, 0.0625F + level, 0.25F, LIQUID_COLOR[t]);
            pose.popPose();
            // The glass writes depth: the liquid inside it has to reach the screen first.
            if (buffers instanceof MultiBufferSource.BufferSource bs) {
                bs.endBatch(liquid);
            }
        }
        body(pose, buffers, light);
    }

    /** A box of flat, unlit colour (the original drew the column full bright with lighting off). */
    private static void box(Matrix4f m, VertexConsumer out, float x0, float y0, float z0, float x1, float y1, float z1, int argb) {
        quad(m, out, argb, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        quad(m, out, argb, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        quad(m, out, argb, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        quad(m, out, argb, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        quad(m, out, argb, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        quad(m, out, argb, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
    }

    private static void quad(Matrix4f m, VertexConsumer out, int argb, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        out.addVertex(m, ax, ay, az).setColor(argb);
        out.addVertex(m, bx, by, bz).setColor(argb);
        out.addVertex(m, cx, cy, cz).setColor(argb);
        out.addVertex(m, dx, dy, dz).setColor(argb);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        // The item renderer has centred the origin on the model's corner: undo it.
        pose.translate(0.5F, 0.5F, 0.5F);
        switch (context) {
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                float m = context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND ? -1.0F : 1.0F;
                // Undo the 1.21 hand prefix, then replay the 1.7.10 chain (RenderPlayer.renderEquippedItems, the
                // EQUIPPED_BLOCK helper, renderer :158-162), as GemRenderer does.
                pose.translate(m * -0.0625F, -0.125F, 0.625F);
                pose.mulPose(Axis.YP.rotationDegrees(-180.0F));
                pose.mulPose(Axis.XP.rotationDegrees(90.0F));
                pose.translate(m * -0.0625F, 0.4375F, 0.0625F);
                pose.translate(m * 0.25F, 0.1875F, -0.1875F);
                pose.scale(0.375F, 0.375F, 0.375F);
                pose.mulPose(Axis.ZP.rotationDegrees(m * 60.0F));
                pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
                pose.mulPose(Axis.ZP.rotationDegrees(m * 20.0F));
                pose.translate(m * -0.5F, -0.5F, -0.5F);
                pose.scale(0.5F, 0.5F, 0.5F);
                pose.translate(0.0F, 2.5F, 1.4F);
                pose.scale(1.5F, 1.5F, 1.5F);
                pose.mulPose(Axis.YP.rotationDegrees(m * -30.0F));
                mirrorMesh(pose, m);
            }
            case GROUND -> {
                pose.scale(0.5F, 0.5F, 0.5F);
                pose.translate(0.0F, 0.5F, 0.0F);
            }
            case GUI -> {
                pose.scale(0.5F, 0.5F, 0.5F);
                // 1.7.10's inventory helper showed the needle pointing down and to the right; a quarter turn does the same here.
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
            default -> pose.scale(0.5F, 0.5F, 0.5F);
        }
        body(pose, buffers, light);
        pose.popPose();
    }

    /**
     * The left-hand picture is the mirror image of the right-hand one. The chain is mirrored by its sign {@code m}; the mesh
     * itself is symmetric front to back (z), so a half turn about y stands in for mirroring it.
     */
    static void mirrorMesh(PoseStack pose, float m) {
        if (m < 0.0F) {
            pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        }
    }
}
