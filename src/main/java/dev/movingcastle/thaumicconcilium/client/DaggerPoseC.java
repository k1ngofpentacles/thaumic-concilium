package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.movingcastle.thaumicconcilium.item.CrimsonDagger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * Option C, "Heartguard" (reverse grip, fist to the sternum): the vow pose, blade turned on oneself. One arm; the smallest
 * silhouette. The stab is a backhand hook out from the chest; the self-cut draws the point down the free forearm. Every
 * number that belongs to this option is in this class; see {@link DaggerPose}.
 * <p>
 * Grip: the option's own reverse model ({@code crimson_dagger_heart}, property value {@link #GRIP}; the handle sits at the
 * model's origin, so the first-person numbers below place the fist and turn the blade about it). The grip changes while the
 * dagger is out of sight in first person: the forward-grip dagger dips below the view (as on a re-equip), the reversed one
 * comes up; the same in reverse on the way out. Nothing pops.
 */
public final class DaggerPoseC implements DaggerPose {
    /** Item property value of the heart grip model ({@code gen_phase5b_trunk.py}). */
    private static final float GRIP = 0.875F;
    // ---- third person (degrees)
    private static final DaggerAnim.Arm HOLD = DaggerAnim.Arm.of(-70.0F, 40.0F, 0.0F);
    /** Contact: arm level, swung out past the shoulder, twisted so the point leads outwards. */
    private static final DaggerAnim.Arm STAB = DaggerAnim.Arm.of(-95.0F, -25.0F, 50.0F);
    private static final DaggerAnim.Arm CUT_FREE = DaggerAnim.Arm.of(-45.0F, 40.0F, 0.0F);
    private static final DaggerAnim.Arm CUT_HOLD_FROM = DaggerAnim.Arm.of(-55.0F, 50.0F, 0.0F);
    private static final DaggerAnim.Arm CUT_HOLD_TO = DaggerAnim.Arm.of(-40.0F, 25.0F, 0.0F);
    /** The hold's breathing: degrees on P and blocks on dy, at 0.15 rad per tick. */
    private static final float SWAY_P = 1.5F;
    private static final float SWAY_DY = 0.008F;
    private static final float SWAY_RATE = 0.15F;
    // ---- timeline
    private static final int CONTACT = 2;
    /** Stab recovery: the reversed dagger is out of view by this tick, the forward grip comes back after it. */
    private static final int STAB_SWAP = 4;
    private static final int CUT_IN = 5;
    private static final int CUT_DRAW_END = 11;
    /** Cut: the grip changes at these ticks (first person: below the view). */
    private static final int CUT_SWAP_IN = 2;
    private static final int CUT_SWAP_OUT = 14;
    /** Raise: the part of it (0..1) spent dipping the forward grip out of view before the reversed one comes up. */
    private static final float RAISE_SWAP = 0.2F;
    // ---- first person: dx, dy, dz, Rx, Ry, Rz
    private static final float[] ZERO = new float[6];
    /** The forward-grip dagger dropped out of view (vanilla's full re-equip drop). */
    private static final float[] FP_DIP = {0.0F, -0.6F, 0.0F, 0.0F, 0.0F, 0.0F};
    /** How far below its pose the reversed dagger waits out of view. */
    private static final float FP_UNDER = -0.25F;
    /** A raise whose use began more than this many ticks before it is a recharge after a stab (first person only). */
    private static final int RECHARGE_SLACK = 4;
    private static final float[] FP_HOLD = {-0.26F, 0.13F, 0.0F, -45.0F, -15.0F, 0.0F};
    private static final float[] FP_STAB = {-0.11F, 0.34F, -0.23F, -10.0F, -55.0F, 0.0F};
    private static final float[] FP_CUT = {-0.31F, 0.47F, -0.25F, -30.0F, -15.0F, 35.0F};
    private static final float FP_CUT_DX_END = -0.23F;
    private static final float FP_CUT_DY_END = 0.31F;
    /** Free forearm for the cut: fist x (times the dagger side), y, z in view space, pitch, roll (times the side). */
    private static final float[] FREE_CUT = {-0.09F, -0.44F, -1.15F, -30.0F, -60.0F};
    /** How far the free arm slides back along itself, out of view. */
    private static final float FREE_SLIDE = 0.8F;

    @Override
    public char letter() {
        return 'C';
    }

    @Override
    public int stabContactTick() {
        return CONTACT;
    }

    @Override
    public float grip(DaggerAnim.Frame f) {
        float t = f.ticks();
        switch (f.mode()) {
            case RAISE:
            case LOWER:
                return f.r() >= RAISE_SWAP ? GRIP : 0.0F;
            case STAB:
                return f.afterR() > 0.0F || t < STAB_SWAP ? GRIP : 0.0F;
            case CUT:
                return t >= CUT_SWAP_IN && t < CUT_SWAP_OUT ? GRIP : 0.0F;
            default:
                return 0.0F;
        }
    }

    // ------------------------------------------------------------------ third person
    private static boolean held(DaggerAnim.Frame f) {
        return f.mode() == DaggerAnim.Mode.RAISE && f.ticks() >= CrimsonDagger.RAISE_TICKS;
    }

    @Override
    public DaggerAnim.Arm holdingArm(DaggerAnim.Frame f, HumanoidModel<?> model, float s) {
        float t = f.ticks();
        switch (f.mode()) {
            case RAISE:
            case LOWER: {
                DaggerAnim.Arm hold = held(f) ? DaggerAnim.Arm.of(HOLD.p() + SWAY_P * Mth.sin(t * SWAY_RATE), HOLD.y(), HOLD.r()) : HOLD;
                return hold.weighted(DaggerAnim.easeOutCubic(f.r()));
            }
            case STAB: {
                if (t < CONTACT) {
                    return DaggerAnim.Arm.lerp(DaggerAnim.easeInQuad(t / CONTACT), HOLD, STAB);
                }
                DaggerAnim.Arm end = f.afterR() > 0.0F ? HOLD : DaggerAnim.Arm.REST;
                return DaggerAnim.Arm.lerp(DaggerAnim.easeOutQuad(DaggerAnim.span(t, CONTACT, CrimsonDagger.STAB_TICKS)), STAB, end);
            }
            case CUT: {
                DaggerAnim.Arm pose = DaggerAnim.Arm.lerp(DaggerAnim.easeOutQuad(DaggerAnim.span(t, CUT_IN, CUT_DRAW_END)),
                        CUT_HOLD_FROM, CUT_HOLD_TO);
                return pose.weighted(cutWeight(t));
            }
            default:
                return DaggerAnim.Arm.REST;
        }
    }

    @Override
    public DaggerAnim.Arm freeArm(DaggerAnim.Frame f, HumanoidModel<?> model, float s) {
        return f.mode() == DaggerAnim.Mode.CUT ? CUT_FREE.weighted(cutWeight(f.ticks())) : DaggerAnim.Arm.REST;
    }

    /** In over ticks 0-5, held through the draw, out over 11-16. */
    private static float cutWeight(float t) {
        if (t < CUT_IN) {
            return DaggerAnim.easeOutCubic(t / CUT_IN);
        }
        return t < CUT_DRAW_END ? 1.0F : 1.0F - DaggerAnim.easeOutQuad(DaggerAnim.span(t, CUT_DRAW_END, CrimsonDagger.CUT_TICKS));
    }

    // ------------------------------------------------------------------ first person
    private static float[] mix(float e, float[] a, float[] b) {
        float[] out = new float[a.length];
        for (int i = 0; i < a.length; i++) {
            out[i] = Mth.lerp(e, a[i], b[i]);
        }
        return out;
    }

    /** {@code pose} waiting below the view. */
    private static float[] under(float[] pose) {
        float[] out = pose.clone();
        out[1] += FP_UNDER;
        return out;
    }

    /** First person is the local player: is it using the dagger, and for longer than {@code ticks}? */
    private static boolean stillUsing(float ticks) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.isUsingItem() && player.getUseItem().getItem() instanceof CrimsonDagger
                && player.getTicksUsingItem() > ticks;
    }

    private static float[] fpTarget(DaggerAnim.Frame f) {
        float t = f.ticks();
        switch (f.mode()) {
            case RAISE:
            case LOWER: {
                float r = f.r();
                if (r < RAISE_SWAP) {
                    if (f.mode() == DaggerAnim.Mode.RAISE && stillUsing(t + RECHARGE_SLACK)) {
                        return FP_DIP;      // recharging after a stab: the dagger is already down, it stays there
                    }
                    return mix(DaggerAnim.easeInQuad(r / RAISE_SWAP), ZERO, FP_DIP);
                }
                float[] v = mix(DaggerAnim.easeOutCubic((r - RAISE_SWAP) / (1.0F - RAISE_SWAP)), under(FP_HOLD), FP_HOLD);
                if (held(f)) {
                    v[1] += SWAY_DY * Mth.sin(t * SWAY_RATE);
                }
                return v;
            }
            case STAB: {
                if (t < CONTACT) {
                    return mix(DaggerAnim.easeInQuad(t / CONTACT), FP_HOLD, FP_STAB);
                }
                if (f.afterR() > 0.0F) {
                    return mix(DaggerAnim.easeOutQuad(DaggerAnim.span(t, CONTACT, CrimsonDagger.STAB_TICKS)), FP_STAB, FP_HOLD);
                }
                if (t < STAB_SWAP) {
                    return mix(DaggerAnim.easeOutQuad(DaggerAnim.span(t, CONTACT, STAB_SWAP)), FP_STAB, under(FP_HOLD));
                }
                if (stillUsing(0.0F)) {
                    return FP_DIP;          // use still held: the raise recharges from below, the forward grip never shows
                }
                return mix(DaggerAnim.easeOutQuad(DaggerAnim.span(t, STAB_SWAP, CrimsonDagger.STAB_TICKS)), FP_DIP, ZERO);
            }
            case CUT: {
                if (t < CUT_SWAP_IN) {
                    return mix(DaggerAnim.easeInQuad(t / CUT_SWAP_IN), ZERO, FP_DIP);
                }
                if (t >= CUT_SWAP_OUT) {
                    return mix(DaggerAnim.easeOutQuad(DaggerAnim.span(t, CUT_SWAP_OUT, CrimsonDagger.CUT_TICKS)), FP_DIP, ZERO);
                }
                float draw = DaggerAnim.easeOutQuad(DaggerAnim.span(t, CUT_IN, CUT_DRAW_END));
                float[] v = FP_CUT.clone();
                v[0] = Mth.lerp(draw, FP_CUT[0], FP_CUT_DX_END);
                v[1] = Mth.lerp(draw, FP_CUT[1], FP_CUT_DY_END);
                if (t < CUT_IN) {
                    return mix(DaggerAnim.easeOutCubic(DaggerAnim.span(t, CUT_SWAP_IN, CUT_IN)), under(FP_CUT), v);
                }
                return t < CUT_DRAW_END ? v
                        : mix(DaggerAnim.easeInQuad(DaggerAnim.span(t, CUT_DRAW_END, CUT_SWAP_OUT)), v, under(v));
            }
            default:
                return ZERO;
        }
    }

    @Override
    public boolean firstPerson(PoseStack pose, float s, DaggerAnim.Frame f) {
        float[] v = fpTarget(f);
        pose.translate(s * v[0], v[1], v[2]);
        pose.mulPose(Axis.XP.rotationDegrees(v[3]));
        pose.mulPose(Axis.YP.rotationDegrees(s * v[4]));
        pose.mulPose(Axis.ZP.rotationDegrees(s * v[5]));
        return true;
    }

    @Override
    public float[] freeHandFirstPerson(DaggerAnim.Frame f, float s) {
        if (f.mode() != DaggerAnim.Mode.CUT) {
            return null;
        }
        float w = cutWeight(f.ticks());
        if (w <= 0.001F) {
            return null;
        }
        return new float[] {s * FREE_CUT[0], FREE_CUT[1], FREE_CUT[2], FREE_CUT[3], 0.0F, s * FREE_CUT[4], (1.0F - w) * FREE_SLIDE};
    }
}
