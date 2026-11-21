package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.movingcastle.thaumicconcilium.item.ShardMill;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Third-person Shard Mill pose while it is in use (an {@link HumanoidModel.ArmPose} added through
 * {@code META-INF/enumextensions.json}).
 * <ul>
 *   <li><b>Grinding</b> (lid closed): the holding arm is lowered to chest height; the mill sits on the inside of the
 *       hand, its bottom pointing away from the body and a little upward with the crank inward; the free hand is on the
 *       crank's grip and goes round with it.</li>
 *   <li><b>Loading</b> (lid open): the mill is held upright and tips its open top towards the chest as the shard goes
 *       in; the free hand comes over the top and lowers into it.</li>
 * </ul>
 * The free hand's target is found by following the same chain the game uses to draw a held item (the arm's pivot and
 * rotation, ItemInHandLayer's turn and offset into the hand, the item's display scale), then this class's own move and
 * turn of the mill ({@link #transformMill}, which {@link MillRenderer} applies) and, when grinding, the crank angle. So
 * the hand lands on the mill itself.
 * <p>
 * Everything here is in model space, in pixels: Y points down and the body faces -Z. ModelPart rotates X, then Y, then Z.
 */
public final class MillArmPose {
    public static final EnumProxy<HumanoidModel.ArmPose> CRANK = new EnumProxy<>(HumanoidModel.ArmPose.class,
            true, (IArmPoseTransformer) MillArmPose::apply);

    /** Holding arm: about 45 degrees forward of hanging, turned slightly in. */
    private static final float HOLD_PITCH = -0.8F;
    private static final float HOLD_YAW_IN = 0.15F;
    /** Grinding: where the mill's bottom points, forward and slightly up (Y is down). */
    private static final Vector3f AIM = new Vector3f(0.0F, -0.35F, -1.0F).normalize();
    /** Loading: how far (degrees) the top tips towards the chest by the time the shard is in. */
    private static final float LOAD_TILT = 35.0F;
    /** The mill sits this far (pixels) in from the holding hand, towards the chest. */
    private static final float INSET = 6.0F;
    /** Must match the third-person "scale" in the mill's item model (gen_phase3_data.py, MILL_DISPLAY). */
    private static final float DISPLAY_SCALE = 0.8F;
    /** Shoulder to the middle of the fist, in pixels. */
    private static final float REACH = 10.5F;
    /** How far (pixels) the shoulder may lean towards or away from the target so the fist lands on it. */
    private static final float MAX_SLIDE = 3.0F;
    /** The mill's mouth: the middle of the cap's top, in the mill model (scaled from the original's cap, top y 3.3). */
    private static final Vector3f MOUTH = new Vector3f(0.7115F, 0.95F, 0.5F);
    /** Loading: the hand starts this far (pixels) above the mouth and settles 1.5 px above it. */
    private static final float DROP = 4.0F;

    private static final Quaternionf RIGHT_GRIND = orientation(true, new Vector3f(AIM), inward(true));
    private static final Quaternionf LEFT_GRIND = orientation(false, new Vector3f(AIM), inward(false));
    private static final Vector3f RIGHT_INSET = inset(true);
    private static final Vector3f LEFT_INSET = inset(false);

    private MillArmPose() {
    }

    private static Vector3f inward(boolean right) {
        // The right arm sits at -X, so the chest is towards +X; mirrored for the left.
        return new Vector3f(right ? 1.0F : -1.0F, 0.0F, 0.0F);
    }

    /** The holding arm's yaw. Positive yaw swings the arm's end towards -X. */
    private static float holdYaw(boolean right) {
        return right ? -HOLD_YAW_IN : HOLD_YAW_IN;
    }

    /** Rotation from the item's frame to model space for a mill held in the given hand (display rotation is zero). */
    private static Matrix3f itemToModelRotation(boolean right) {
        return new Matrix3f().rotateZYX(0.0F, holdYaw(right), HOLD_PITCH).rotateX(-Mth.HALF_PI).rotateY(Mth.PI);
    }

    /**
     * The turn that points the mill's bottom (item -Y) along {@code bottom} and its crank side (item -X) as close to
     * {@code crankSide} as possible, both given in model space.
     */
    private static Quaternionf orientation(boolean right, Vector3f bottom, Vector3f crankSide) {
        Matrix3f toItem = itemToModelRotation(right).invert();
        Vector3f b = toItem.transform(bottom).normalize();
        Vector3f side = toItem.transform(crankSide);
        Vector3f y = new Vector3f(b).negate();
        Vector3f crank = new Vector3f(side).sub(new Vector3f(b).mul(side.dot(b))).normalize();
        Vector3f x = new Vector3f(crank).negate();
        Vector3f z = new Vector3f(x).cross(y).normalize();
        return new Quaternionf().setFromNormalized(new Matrix3f(x, y, z));
    }

    /** {@link #INSET} pixels towards the chest, expressed in the mill renderer's frame (block units). */
    private static Vector3f inset(boolean right) {
        return itemToModelRotation(right).invert().transform(inward(right).mul(INSET / 16.0F / DISPLAY_SCALE));
    }

    /** Loading turn: upright, top tipped towards the chest by {@code progress} of {@link #LOAD_TILT}, crank outward. */
    private static Quaternionf loading(boolean right, float progress) {
        float tilt = LOAD_TILT * progress * Mth.DEG_TO_RAD;
        Vector3f top = new Vector3f(0.0F, -Mth.cos(tilt), Mth.sin(tilt));   // up is -Y, the chest is +Z
        return orientation(right, top.negate(), inward(right).negate());
    }

    /** Fraction of the one-second load done. */
    private static float loadProgress(LivingEntity entity, float partialTick) {
        return Mth.clamp((entity.getTicksUsingItem() + partialTick) / ShardMill.LOAD, 0.0F, 1.0F);
    }

    /**
     * Applied by {@link MillRenderer} in third person while the mill is in use: move it in towards the chest and turn
     * it for grinding or loading. The pose stack is at the renderer's origin (the model's corner).
     */
    public static void transformMill(PoseStack pose, LivingEntity holder, ItemStack stack, boolean rightHand,
                                     float partialTick) {
        Vector3f inset = rightHand ? RIGHT_INSET : LEFT_INSET;
        pose.translate(inset.x, inset.y, inset.z);
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(ShardMill.state(stack).open() ? loading(rightHand, loadProgress(holder, partialTick))
                : (rightHand ? RIGHT_GRIND : LEFT_GRIND));
        pose.translate(-0.5F, -0.5F, -0.5F);
    }

    /** The mill renderer's frame, in model pixels, as the game would place it in the holding hand. */
    private static Matrix4f millToModel(ModelPart holding, boolean right, Quaternionf turn) {
        Vector3f inset = right ? RIGHT_INSET : LEFT_INSET;
        return new Matrix4f()
                .translate(holding.x, holding.y, holding.z)                 // ModelPart.translateAndRotate
                .rotateZYX(holding.zRot, holding.yRot, holding.xRot)
                .rotateX(-Mth.HALF_PI).rotateY(Mth.PI)                      // ItemInHandLayer
                .translate(right ? 1.0F : -1.0F, 2.0F, -10.0F)
                .scale(DISPLAY_SCALE)                                       // the item's third-person display
                .translate(-8.0F, -8.0F, -8.0F)                             // ItemRenderer's custom-renderer offset
                .translate(inset.x * 16.0F, inset.y * 16.0F, inset.z * 16.0F)   // transformMill
                .translate(8.0F, 8.0F, 8.0F).rotate(turn).translate(-8.0F, -8.0F, -8.0F);
    }

    private static void apply(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        boolean right = arm == HumanoidArm.RIGHT;
        ModelPart holding = right ? model.rightArm : model.leftArm;
        ModelPart free = right ? model.leftArm : model.rightArm;
        float partialTick = MillRenderer.partialTick();

        holding.xRot = HOLD_PITCH;
        holding.yRot = holdYaw(right);
        holding.zRot = 0.0F;

        Vector3f target;
        if (ShardMill.state(entity.getUseItem()).open()) {
            float progress = loadProgress(entity, partialTick);
            target = millToModel(holding, right, loading(right, progress))
                    .transformPosition(new Vector3f(MOUTH).mul(16.0F))
                    .add(0.0F, -1.5F - DROP * (1.0F - progress), 0.0F);   // above the mouth (up is -Y), lowering in
        } else {
            float angle = MillRenderer.crankDegrees(entity, partialTick) * Mth.DEG_TO_RAD;
            target = millToModel(holding, right, right ? RIGHT_GRIND : LEFT_GRIND)
                    .translate(0.0F, MillRenderer.CRANK_PIVOT_Y * 16.0F, MillRenderer.CRANK_PIVOT_Z * 16.0F)
                    .rotateX(angle)
                    .transformPosition(new Vector3f(MillRenderer.GRIP_X * 16.0F,
                            (MillRenderer.GRIP_Y - MillRenderer.CRANK_PIVOT_Y) * 16.0F,
                            (MillRenderer.GRIP_Z - MillRenderer.CRANK_PIVOT_Z) * 16.0F));
        }

        // Aim the free arm from its shoulder at the target: its axis is (sin x sin y, cos x, sin x cos y).
        Vector3f d = target.sub(free.x, free.y, free.z);
        float distance = d.length();
        d.normalize();
        float xRot = -(float) Math.acos(Mth.clamp(d.y, -1.0F, 1.0F));
        float s = Mth.sin(xRot);
        free.xRot = xRot;
        free.yRot = Math.abs(s) < 1.0E-4F ? 0.0F : (float) Mth.atan2(d.x / s, d.z / s);
        free.zRot = 0.0F;
        // Lean the shoulder in or out a little so the fist, not the wrist or thin air, is on the target.
        float slack = Mth.clamp(REACH - distance, -MAX_SLIDE, MAX_SLIDE);
        free.x -= d.x * slack;
        free.y -= d.y * slack;
        free.z -= d.z * slack;
    }
}
