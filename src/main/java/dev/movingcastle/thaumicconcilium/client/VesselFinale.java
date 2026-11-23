package dev.movingcastle.thaumicconcilium.client;

import dev.movingcastle.thaumicconcilium.entity.VesselDeath;
import net.minecraft.util.Mth;

/**
 * The finale's look (phase 5d cycle 4, design "The finale" and "The light inside"; mockup {@code tools/preview/finale.html}, approved
 * 2026-11-23): the numbers and the pure curves the renderer, the flash and the white-out draw from. Ages are ticks since the dying
 * Vessel's start; the server's own times ({@code light}, {@code burst}, {@code c}) come from its synced state and
 * {@code TCConfig finale.*}. Nothing here touches the game, so the preview's exporter reads the constants and a test can step the curves.
 */
public final class VesselFinale {
    /** The horizon's violet turns to the mana beam's cyan between these ages (grove endings only). */
    public static final int CYAN_START = 30;
    public static final int CYAN_END = 110;
    public static final float RING_INT = 1.0F;
    /** The dark halo reaches this far beyond the black, at this peak alpha. */
    public static final float HALO_EXTRA = 8.0F;
    public static final float HALO_ALPHA = 0.80F;
    /** The light stage: the blindness eases to a shadowy blanket of this strength over these ticks. */
    public static final int SHADOW_EASE = 20;
    public static final float SHADOW_ALPHA = 0.55F;
    /** The wisp (a small, faint Brightest One): ticks into the light stage it appears, its rise, its pulses, its burst. */
    public static final int WISP_DELAY = 10;
    public static final int WISP_RISE = 30;
    public static final int WISP_PULSES = 3;
    public static final int WISP_BURST = 8;
    public static final float WISP_SCALE = 0.4F;
    public static final float WISP_FAINT = 0.5F;
    public static final float WISP_SWELL = 0.35F;
    /** The white light once it escapes (ticks after the collapse starts): growth to its reach (x the swell's radius), hold, fade. */
    public static final int LIGHT_GROW = 10;
    public static final float LIGHT_REACH = 1.5F;
    public static final int LIGHT_HOLD = 10;
    public static final int LIGHT_FADE = 30;
    /** How strongly those who stood inside see the light (they get no white-out). */
    public static final float LIGHT_INSIDE = 0.6F;
    /** The outside observer's white-out (ticks after the collapse starts): 1.9 s in all, within the picked 1-2 s. */
    public static final int BANG_DELAY = 4;
    public static final int BANG_RISE = 2;
    public static final int BANG_HOLD = 16;
    public static final int BANG_FADE = 20;
    public static final float BANG_PEAK = 1.0F;
    /** "Looking at it": the white-out is whole within this angle of the hole and nothing beyond the second (degrees). */
    public static final float BANG_LOOK_FULL = 45.0F;
    public static final float BANG_LOOK_NONE = 80.0F;
    public static final int LIGHT_COLOR = 0xFFFFFF;
    public static final int BANG_COLOR = 0xFFFFFF;

    /** The render harness pins the age here for stills (negative: the game's own time); {@link #pinnedC} is the collapse it pretends. */
    public static float pinnedAge = -1.0F;
    public static int pinnedC = -1;

    private VesselFinale() {
    }

    /** The collapse's age: the synced one, or the harness's while it pins the age. */
    public static int collapse(int synced) {
        return pinnedAge >= 0.0F && pinnedC >= 0 ? pinnedC : synced;
    }

    public static float smooth(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    /** How far the horizon has turned to cyan at age {@code t}. */
    public static float shift(float t) {
        return smooth((t - CYAN_START) / Math.max(1.0e-6F, CYAN_END - CYAN_START));
    }

    /** What is left of the dark for those inside once the light stage began at {@code light}: 1 easing to the shadow. */
    public static float shadow(float t, int light) {
        return light < 0 ? 1.0F : 1.0F - (1.0F - SHADOW_ALPHA) * smooth((t - light) / SHADOW_EASE);
    }

    /**
     * The wisp at age {@code t}: {@code out} = how far up from the pond to the hole (0-1), its size multiplier, its brightness. False
     * when there is none (before it appears, or once its burst has filled the black).
     */
    public static boolean wisp(float t, int light, int burst, float[] out) {
        if (light < 0) {
            return false;
        }
        float start = light + WISP_DELAY;
        float pulse = Math.min(start + WISP_RISE, burst);
        if (t < start || t >= burst + WISP_BURST) {
            return false;
        }
        if (t < pulse) {
            float q = (t - start) / Math.max(1.0F, pulse - start);
            out[0] = smooth(q);
            out[1] = 1.0F;
            out[2] = WISP_FAINT * Mth.clamp(q * 4.0F, 0.0F, 1.0F);
        } else if (t < burst) {
            float q = (t - pulse) / Math.max(1.0F, burst - pulse);
            float b = 0.5F - 0.5F * Mth.cos(Mth.TWO_PI * WISP_PULSES * q * q); // quickening
            out[0] = 1.0F;
            out[1] = 1.0F + WISP_SWELL * b;
            out[2] = WISP_FAINT + (1.0F - WISP_FAINT) * b * (0.4F + 0.6F * q);
        } else {
            float g = (t - burst) / WISP_BURST;
            out[0] = 1.0F;
            out[1] = 1.0F + WISP_SWELL + 3.0F * g * g;
            out[2] = 1.0F;
        }
        return true;
    }

    /** The burst's light inside the black before the collapse: its radius, filling the swell (contained) over the burst. */
    public static float contained(float t, int burst, float rs) {
        float g = Mth.clamp((t - burst) / WISP_BURST, 0.0F, 1.0F);
        return rs * g * g;
    }

    /** The escaped light {@code u} ticks after the collapse: its radius; those inside already stand in it at the swell's radius. */
    public static float lightRadius(float u, float rs, boolean inside) {
        float g = Mth.clamp(u / LIGHT_GROW, 0.0F, 1.0F);
        float from = inside ? rs : 0.0F;
        return from + (rs * LIGHT_REACH - from) * (1.0F - (1.0F - g) * (1.0F - g));
    }

    public static float lightAlpha(float u) {
        return u <= 0.0F ? 0.0F : 1.0F - smooth((u - LIGHT_GROW - LIGHT_HOLD) / LIGHT_FADE);
    }

    /** The white-out's strength {@code u} ticks after the collapse: rise, hold, eased fade. */
    public static float bang(float u) {
        u -= BANG_DELAY;
        if (u <= 0.0F) {
            return 0.0F;
        }
        if (u < BANG_RISE) {
            return BANG_PEAK * u / BANG_RISE;
        }
        float k = Mth.clamp((u - BANG_RISE - BANG_HOLD) / BANG_FADE, 0.0F, 1.0F);
        return BANG_PEAK * (1.0F - k) * (1.0F - k);
    }

    /** How much of the white-out an eye gets whose view is {@code degrees} off the hole. */
    public static float looking(float degrees) {
        return 1.0F - smooth((degrees - BANG_LOOK_FULL) / (BANG_LOOK_NONE - BANG_LOOK_FULL));
    }

    /** The last tick after the collapse at which anything of the flash is still drawn. */
    public static int flashTicks() {
        return Math.max(LIGHT_GROW + LIGHT_HOLD + LIGHT_FADE, BANG_DELAY + BANG_RISE + BANG_HOLD + BANG_FADE);
    }

    /**
     * Whether an eye {@code dist} from the hole is in the black: within its radius while it stands, and, once the collapse began, within
     * the swell's (the server's own measure of who stood inside). Such an eye is not drawn the black's disc.
     */
    public static boolean within(float dist, float t, float rs, int c) {
        if (t < 0.0F) {
            return false;
        }
        return c >= 0 && t >= c ? dist <= rs : dist <= VesselDeath.radius(t, rs, -1);
    }

    /** How far the black has gone for those inside, 0-1 over the collapse. */
    public static float escape(float t, int c) {
        return c < 0 ? 0.0F : smooth((t - c) / VesselDeath.FADE);
    }
}
