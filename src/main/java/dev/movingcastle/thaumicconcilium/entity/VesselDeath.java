package dev.movingcastle.thaumicconcilium.entity;

import net.minecraft.util.Mth;

/**
 * The finale's timeline (phase 5d cycle 4e, memo {@code docs/design/5d-finale-growth.md} section 1): pure functions of the dying Vessel's
 * age {@code t} in ticks, shared by the server (blindness, collapse) and the client (the swell). The Vessel swells to {@code rs} in
 * {@link #SWELL} ticks, holds while the grove paints the crater, and collapses over {@link #FADE} ticks from {@code c}, the tick the
 * server decided the crater was done (-1 while undecided).
 */
public final class VesselDeath {
    /** Ticks the core takes to swell to the crater rim. */
    public static final int SWELL = 30;
    /** The core's radius when the swell starts (its resting radius when subdued). */
    public static final float START_RADIUS = 0.6F;
    /** Ticks between the server's checks (blindness and the crater-done test). */
    public static final int CHECK = 10;
    /** The ring's flash and the black's collapse after the decision. */
    public static final int FADE = 20;

    private VesselDeath() {
    }

    /** The swell's radius {@code rs}: the farthest rim plus one block. */
    public static float swellRadius(double maxRim) {
        return (float) (maxRim + 1.0);
    }

    /** The black's radius at age {@code t}: ease-out up to {@code rs}, held, then shrinking to nothing after {@code c} (when decided). */
    public static float radius(float t, float rs, int c) {
        if (t <= 0.0F) {
            return START_RADIUS;
        }
        if (c >= 0 && t >= c) {
            float k = Mth.clamp((t - c) / FADE, 0.0F, 1.0F);
            return rs * (1.0F - k * k * (3.0F - 2.0F * k));
        }
        float s = Mth.clamp(t / SWELL, 0.0F, 1.0F);
        float ease = 1.0F - (1.0F - s) * (1.0F - s);
        return START_RADIUS + (rs - START_RADIUS) * ease;
    }

    /**
     * Whether the collapse is decided at age {@code t}: only on a check tick; the crater must be done (a Vessel without a painting job
     * has none to wait for) and the minimum hold passed, or the timeout forces it.
     */
    public static boolean due(int t, boolean craterDone, int minHold, int timeout) {
        if (t % CHECK != 0) {
            return false;
        }
        return t >= timeout || (craterDone && t >= minHold);
    }

    /** Whether an eye at squared distance {@code d2} from the hole is inside a black of radius {@code r}. */
    public static boolean inside(double d2, double r) {
        return d2 <= r * r;
    }
}
