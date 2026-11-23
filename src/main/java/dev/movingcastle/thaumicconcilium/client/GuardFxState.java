package dev.movingcastle.thaumicconcilium.client;

import dev.movingcastle.thaumicconcilium.network.GuardFxPayload;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * What a client knows of the shield foci, per entity id, and every curve of their look (docs/design/shield-foci-look.md). No client
 * class is named here, so the pack test steps it. Times are game ticks (the level's game time plus the partial tick); the server's
 * expiry is lazy and silent, so an entry ends itself when its {@code until} passes.
 */
public final class GuardFxState {
    /**
     * Runic amber, the one tint of both wards (round 4). Thaumaturge draws no runic ward in the world, so the colour is read from
     * its runic shielding research icon (textures/research/r_runicupg.png: its bright saturated pixels average CF7717, here at
     * full brightness). It also writes an item's "Runic charge" tooltip line in gold (TTTooltipEvents, FFAA00): the same family.
     */
    public static final int WARD_COLOR = 0xFF921C;
    /** The same amber, lighter: the burst shell, the flash and the forced-block rune. */
    public static final int WARD_HOT = 0xFFC070;
    // ---- Riposte
    public static final int RIPOSTE_COLOR = WARD_COLOR;
    public static final float HALO_BASE = 0.18F;
    public static final float HALO_SWING = 0.06F;
    public static final float HALO_WARN_BASE = 0.26F;
    public static final float HALO_WARN_SWING = 0.14F;
    /** The halo lift of an arming (the shell takes half of ARM_BOOST). */
    public static final float HALO_ARM_BOOST = 0.30F;
    public static final float HALO_PERIOD = 32.0F;
    public static final int WARN_TICKS = 40;
    public static final float WARN_PERIOD = 8.0F;
    public static final float ARM_BOOST = 0.45F;
    public static final float ARM_TICKS = 8.0F;
    public static final float RELEASE_TICKS = 8.0F;
    public static final float RELEASE_SWELL = 0.35F;
    /** The ward shell: its alpha with one charge, what each further charge adds, and how it forms and bursts. */
    public static final float SHELL_ALPHA = 0.32F;
    public static final float SHELL_PER_CHARGE = 0.02F;
    public static final float SHELL_FORM_TICKS = 8.0F;
    public static final float BURST_SWELL = 0.6F;
    /** Bolts and shards keep coming this many ticks after a release or a forced block. */
    public static final int CRACKLE_TICKS = 4;
    // ---- Aegis
    public static final int AEGIS_COLOR = WARD_COLOR;
    public static final float RING_MARGIN = 0.55F;
    public static final float RING_HEIGHT = 0.72F;
    public static final float RING_ALPHA = 0.50F;
    public static final float RING_SWING = 0.10F;
    public static final float RING_PERIOD = 48.0F;
    public static final float RING_SPIN = 0.6F;
    public static final float RING_FADE_TICKS = 4.0F;
    public static final float FLASH_TICKS = 8.0F;
    public static final float FLASH_BOOST = 0.5F;

    private static final long NEVER = Long.MIN_VALUE / 2;

    /** One entity's state. */
    public static final class State {
        public int charges;
        public long armedUntil = NEVER;
        public long armAt = NEVER;
        public long releaseAt = NEVER;
        public float rx, ry, rz;
        public long aegisUntil = NEVER;
        public long aegisFlashAt = NEVER;
        public float ax, ay, az;

        public boolean armed(double t) {
            return charges > 0 && t < armedUntil;
        }

        /** 1 at a release, 0 from {@link #RELEASE_TICKS} later. */
        public float releaseEase(double t) {
            return fade(t - releaseAt, RELEASE_TICKS);
        }

        public boolean aegis(double t) {
            return t < aegisUntil;
        }

        /** 1 at a forced block, 0 from {@link #FLASH_TICKS} later. */
        public float flashEase(double t) {
            return fade(t - aegisFlashAt, FLASH_TICKS);
        }

        /** The halo's alpha: the breath (quick in the last two seconds), lifted by an arming, driven to 1 by a release. */
        public float haloAlpha(double t) {
            float a = 0.0F;
            if (armed(t)) {
                boolean warn = armedUntil - t <= WARN_TICKS;
                a = warn ? HALO_WARN_BASE + HALO_WARN_SWING * (float) Math.sin(Math.PI * 2.0 * t / WARN_PERIOD)
                        : HALO_BASE + HALO_SWING * (float) Math.sin(Math.PI * 2.0 * t / HALO_PERIOD);
                a += HALO_ARM_BOOST * fade(t - armAt, ARM_TICKS);
            }
            float e = releaseEase(t);
            return Math.min(1.0F, a + (1.0F - a) * e);
        }

        /** The ward shell's alpha: brighter with every charge, breathing with the halo, flickering in the last two seconds. */
        public float shellAlpha(double t) {
            if (!armed(t)) {
                return 0.0F;
            }
            float a = SHELL_ALPHA + SHELL_PER_CHARGE * (Math.min(charges, 10) - 1);
            boolean warn = armedUntil - t <= WARN_TICKS;
            a *= warn ? 0.65F + 0.35F * (float) Math.sin(Math.PI * 2.0 * t / WARN_PERIOD) : 0.88F + 0.12F * (float) Math.sin(Math.PI * 2.0 * t / HALO_PERIOD);
            return Math.min(1.0F, a * (0.5F + 0.5F * shellForm(t)) + ARM_BOOST * 0.5F * fade(t - armAt, ARM_TICKS));
        }

        /** 0 to 1 over the ticks after an arming or a release: the shell (re)forms. */
        public float shellForm(double t) {
            return clamp01((float) ((t - Math.max(armAt, releaseAt)) / SHELL_FORM_TICKS));
        }

        /** The shell grows from 0.6 of its size as it forms. */
        public float shellScale(double t) {
            float f = shellForm(t);
            return 0.6F + 0.4F * (1.0F - (1.0F - f) * (1.0F - f));
        }

        /** The burst shell of a release swells as it fades ({@link #releaseEase} is its alpha). */
        public float burstScale(double t) {
            return 1.0F + BURST_SWELL * (1.0F - releaseEase(t));
        }

        public float haloScale(double t) {
            float e = releaseEase(t);
            return 1.0F + RELEASE_SWELL * e * e;
        }

        /** The ring's alpha for a shield raised {@code raisedTicks} ago (blocking starts at 5), plus the forced-block lift. */
        public float ringAlpha(double t, float raisedTicks) {
            float in = ringIn(raisedTicks);
            float base = in * (RING_ALPHA + RING_SWING * (float) Math.sin(Math.PI * 2.0 * t / RING_PERIOD));
            return Math.min(1.0F, base + FLASH_BOOST * flashEase(t));
        }

        boolean live(long now) {
            return now < armedUntil && charges > 0 || now < aegisUntil || now - releaseAt <= RELEASE_TICKS || now - aegisFlashAt <= FLASH_TICKS;
        }
    }

    /** Ward shells drawn for a number of charges: one, two from 2, three from 6 (each also brightens the shell). */
    public static int shellLayers(int charges) {
        return charges <= 1 ? 1 : charges <= 5 ? 2 : 3;
    }

    /** 0 to 1 over the four ticks after blocking starts. */
    public static float ringIn(float raisedTicks) {
        return clamp01((raisedTicks - 5.0F) / RING_FADE_TICKS);
    }

    private static float fade(double age, float over) {
        return age < 0.0 ? 0.0F : clamp01(1.0F - (float) (age / over));
    }

    private static float clamp01(float v) {
        return v < 0.0F ? 0.0F : Math.min(v, 1.0F);
    }

    private static final Map<Integer, State> STATES = new HashMap<>();
    private static long lastPrune;

    private GuardFxState() {
    }

    public static boolean isEmpty() {
        return STATES.isEmpty();
    }

    public static State get(int entityId) {
        return STATES.get(entityId);
    }

    public static Map<Integer, State> all() {
        return STATES;
    }

    public static void clear() {
        STATES.clear();
    }

    /** Takes the server's message at game time {@code now}; returns the entity's state (never null). */
    public static State apply(GuardFxPayload p, long now) {
        State s = STATES.computeIfAbsent(p.entityId(), k -> new State());
        switch (p.kind()) {
            case GuardFxPayload.ARMED -> {
                s.charges = p.charges();
                s.armedUntil = p.until();
                s.armAt = now;
            }
            case GuardFxPayload.RELEASED -> {
                s.charges = p.charges();
                s.armedUntil = p.charges() > 0 ? p.until() : NEVER;
                s.releaseAt = now;
                s.rx = p.dx();
                s.ry = p.dy();
                s.rz = p.dz();
            }
            case GuardFxPayload.DISARMED -> {
                s.charges = 0;
                s.armedUntil = NEVER;
            }
            case GuardFxPayload.AEGIS_ON -> s.aegisUntil = p.until();
            case GuardFxPayload.AEGIS_OFF -> s.aegisUntil = NEVER;
            case GuardFxPayload.AEGIS_BLOCK -> {
                s.aegisFlashAt = now;
                s.ax = p.dx();
                s.ay = p.dy();
                s.az = p.dz();
            }
            default -> {
            }
        }
        return s;
    }

    /** Drops what has run out. Called from the render hooks, at most once a game tick. */
    public static void prune(long now) {
        if (now == lastPrune || STATES.isEmpty()) {
            return;
        }
        lastPrune = now;
        for (Iterator<State> it = STATES.values().iterator(); it.hasNext(); ) {
            if (!it.next().live(now)) {
                it.remove();
            }
        }
    }
}
