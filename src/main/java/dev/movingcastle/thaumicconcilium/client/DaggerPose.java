package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;

/**
 * One Crimson Dagger animation option: every number that differs between the options lives in an implementation of this
 * (C, Heartguard: {@link DaggerPoseC}). The shared code ({@link DaggerAnim}, {@link DaggerArmPose}, {@link DaggerClient}) calls it with a
 * {@link DaggerAnim.Frame}; the implementation owns the keyframes and easing of its phases.
 * <p>
 * Conventions (the memo's): {@code s} = +1 right arm, -1 left. Third person, degrees: P = xRot (-90 arm level forward,
 * -180 straight up), Y = yaw towards the midline, R = roll away from the body. First person, after the base translate:
 * {@code translate(s*dx, dy, dz)} then {@code XP(Rx)}, {@code YP(s*Ry)}, {@code ZP(s*Rz)}.
 */
public interface DaggerPose {
    /** The option's letter (A, B or C), matched against the {@code dagger_dev} switch. */
    char letter();

    /** The tick of a stab on which the blade lands (the harness marker {@code dagger_stab}). */
    int stabContactTick();

    /**
     * Item property {@code thaumicconcilium:grip}, which picks the item model (numbers in {@code gen_phase5b_trunk.py}):
     * 0 forward (the plain handheld model), 0.25 the self-cut (third person: the blade continues the forearm, edge down;
     * first person: the handheld display), 0.5 inline, 0.75 plunge (reverse grip with the wrist turned down, for the end
     * of a downward stab; first person as reverse), 0.875 heart (option C: the true reverse grip, straight out of the bottom
     * of the fist), 1 reverse (icepick, tilted 25 degrees up the arm).
     */
    float grip(DaggerAnim.Frame frame);

    /** The holding arm's third-person target. {@code model} is for options that follow the head; {@code s} the arm's side. */
    DaggerAnim.Arm holdingArm(DaggerAnim.Frame frame, HumanoidModel<?> model, float s);

    /** The free arm's target (posed only when that hand is empty). */
    DaggerAnim.Arm freeArm(DaggerAnim.Frame frame, HumanoidModel<?> model, float s);

    /** How far the holding shoulder slides forward along the look, in model pixels (option B's lunge); 0 for the others. */
    default float shoulderSlide(DaggerAnim.Frame frame) {
        return 0.0F;
    }

    /**
     * First person, the dagger hand: apply the option's offsets to {@code pose}, which already carries the base translate.
     * Return false to leave vanilla's swing and equip alone.
     */
    boolean firstPerson(PoseStack pose, float s, DaggerAnim.Frame frame);

    /**
     * First person, the free arm (drawn only with an empty off hand), or null when it is not in view:
     * {@code {x, y, z, pitch, yaw, roll, slide}}, already including any side sign. x, y, z: the fist's centre in view
     * space (blocks; the eye is the origin, -z is ahead). The angles are {@link DaggerFirstPerson}'s: all 0 = the forearm
     * hangs straight down from the fist; roll turns it in the screen plane (counter-clockwise; 180 = it comes down from
     * the top edge), negative pitch brings the shoulder end towards the eye. slide (blocks) pulls the arm back along its
     * own length, for sliding it in and out of view. A shorter array leaves the rest 0. {@code s} is the dagger arm's side.
     */
    float[] freeHandFirstPerson(DaggerAnim.Frame frame, float s);
}
