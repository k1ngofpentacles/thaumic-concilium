package dev.movingcastle.thaumicconcilium.client;

import dev.movingcastle.thaumicconcilium.block.VerdantCapitalBlockEntity;
import dev.movingcastle.thaumicconcilium.block.VerdantCapitalBlockEntity.Drive;
import net.minecraft.util.Mth;

/**
 * How one Verdant Capital moves (phase 5d, round 5; docs/design/5d-look.md addendum A4 and A5, docs/design/5d-capital-breath.md
 * with the user's decisions in 5d-design.md, "Breath decisions"). One of these hangs on each capital the client has drawn. The
 * renderer steps it frame by frame and reads the pose of the eight pieces from it; the block entity never ticks, the server knows
 * nothing of it, and every irregularity is a hash of the block position and a counter, so nothing here uses a random source.
 *
 * <p>What it holds beyond the renderer's own eased values (angle, speed, base spread, darkness):
 * <ul>
 * <li><b>arousal</b> (fed 0, idle 0.1, starved 0.5, unwinding 1) and <b>anger</b> (0 to 1: the latter half of a starved capital's
 * grace), which pick the breath;
 * <li>the <b>breath</b>: a phase and the shape of the breath now being taken. It only ever expands the sphere from its base, and
 * carries the tremble envelope that scales the wobble and the jiggle;
 * <li>the <b>crossing</b> (0 to 1): how far the six waist pieces have left their triforces for the stored loop of
 * {@link CapitalLoop}, and the pole pieces' turn against the stopped rings.
 * </ul>
 * Every bound that keeps the pieces from touching is proved by tools/capital_clearance.py (part 7) for the formulas as written here.
 */
public final class CapitalMotion {
    public static final float PX = 1.0F / 16.0F;

    // ---- the pose per drive: the upper triforce's speed (degrees a tick, clockwise from above; the lower turns the other way)
    // ---- and the easing times (ticks)
    static final float OMEGA_FED = 3.0F;
    static final float OMEGA_STARVED = 0.3F;
    static final float OMEGA_UNWINDING = -4.5F;
    static final float TAU_IDLE = 30.0F;
    static final float TAU_FED = 12.0F;
    static final float TAU_STARVED = 40.0F;
    static final float TAU_UNWINDING = 10.0F;
    /** The sphere: how dark per drive, and the easing time. */
    static final float DARK_STARVED = 0.5F;
    static final float DARK_UNWINDING = 1.0F;
    static final float TAU_DARK = 10.0F;
    /** The base spread's easing (and the arousal's): opening when fed, closing, and snapping in when the capital unwinds. */
    static final float TAU_SEP_OPEN = 15.0F;
    static final float TAU_SEP_SHUT = 25.0F;
    static final float TAU_SEP_SNAP = 6.0F;
    /** At rest the star settles to the nearest closed alignment (every 60 degrees) with this easing time. */
    static final float TAU_SETTLE = 20.0F;
    /** At the ring's tightest spread, before the crossing, the rings stop and settle faster. */
    static final float TAU_SETTLE_TIGHT = 6.0F;
    static final float TAU_RAY = 10.0F;
    /** The closed star's corner distance from the centre (STAR_RADIUS). */
    static final float STAR_RADIUS = 7.75F / 16.0F * 0.6123724F;

    // ---- the spreads (blocks): how far every piece stands out from its closed place
    /** The tightest spread at which the two triforces can still circle with the full wobble. */
    public static final float SPREAD_RING = 2.035F * PX;
    /** Full compaction, on the crossing loop only. */
    public static final float SPREAD_TIGHT = 1.75F * PX;
    public static final float SPREAD_IDLE = 2.60F * PX;
    public static final float SPREAD_FED = 2.95F * PX;
    /** The widest the star ever is: fed at the top of an inhale (15.9 px tall). */
    public static final float SPREAD_TOP = 3.202F * PX;
    /** Degrees of twist given up per block of spread beyond the ring's tightest: 60 there, 45 at the top of fed's inhale. */
    static final float TWIST_PER_BLOCK = 15.0F / (SPREAD_TOP - SPREAD_RING);
    /** The breath's headroom above the base: in the open states, and at full compaction. */
    static final float HEADROOM_OPEN = 0.25F * PX;
    static final float HEADROOM_TIGHT = 0.12F * PX;
    /** Half the block, less the closed star's reach: the jiggle is held to this less the spread, so the star stays in its block. */
    public static final float JIGGLE_ROOM = 8.0F * PX - STAR_RADIUS;

    // ---- arousal per drive, and the anger period
    static final float AROUSAL_IDLE = 0.1F;
    static final float AROUSAL_STARVED = 0.5F;
    /** Anger: 16 breaths a minute, the exhale short and hard with a half-second pause after it, as deep as the headroom allows. */
    static final float ANGER_PERIOD = 3.75F;
    static final float ANGER_RATIO = 1.3F;
    static final float ANGER_PAUSE = 0.5F;
    static final float ANGER_JITTER = 0.05F;
    static final float ANGER_EXHALE_ATTACK = 0.5F;
    /** Easing times (ticks): anger coming on, giving way to panic while the capital unwinds, and leaving when it is fed again. */
    static final float TAU_ANGER_IN = 40.0F;
    static final float TAU_ANGER_TO_PANIC = 60.0F;
    static final float TAU_ANGER_OUT = 20.0F;
    /** A drop in the arousal a drive asks for of at least this brings one sigh of relief. */
    static final float RELIEF_DROP = 0.39F;

    // ---- the breath's curve, at arousal 0, 0.25, 0.5, 0.75, 1 (5d-capital-breath.md, section 2)
    private static final float[] DEPTH = {1.0F, 0.9F, 0.65F, 0.6F, 0.75F};
    private static final float[] JITTER_LENGTH = {0.0F, 0.02F, 0.06F, 0.14F, 0.22F};
    private static final float[] JITTER_DEPTH = {0.0F, 0.03F, 0.10F, 0.25F, 0.40F};
    private static final float[] STEEPNESS = {8.0F, 6.6F, 5.25F, 3.9F, 2.5F};
    private static final float[] FLOOR = {0.0F, 0.0F, 0.0F, 0.08F, 0.30F};
    private static final float[] WOBBLE = {0.75F, 1.0F, 1.5F, 2.75F, 4.0F};
    private static final float[] TREMOR = {0.0F, 0.0F, 0.10F, 0.20F, 0.30F};
    private static final float[] JIGGLE = {0.05F, 0.08F, 0.12F, 0.26F, 0.40F};
    private static final float[] ATTACK = {0.0F, 0.0F, 0.0F, 0.3F, 0.6F};

    // ---- the crossing
    /** Ticks for the waist pieces to take up (or leave) their loops. */
    static final float CROSS_TICKS = 40.0F;
    /** The star compacts from the ring's spread to full compaction over the last part of that. */
    public static final float CROSS_COMPACT_FROM = 0.6F;
    static final float LAT_WAIST = 19.4712F;

    // ---- state (public: the pack test reads it)
    public float arousal;
    public float anger;
    /** 0 circling, 1 on the loop; while above 0 the rings stand still at {@link #crossRest} degrees. */
    public float cross;
    float crossRest;
    /** The pole pieces' turn against their own triforces (degrees) and its speed; 0 whenever the star is not at the ring's tightest. */
    public float poleOffset;
    float poleSpin;
    /** What the beam's pulses follow: the turning of the star while it circles, of its poles while the rings stand still. */
    float rayAngle;
    private float lastTarget;
    private boolean reliefPending;
    private long seed;
    private float loopOffset;
    private double wobble1;
    private double wobble2;
    private double jiggleTime;

    // ---- the breath being taken
    private static final int RISE = 0;
    private static final int TOP = 1;
    private static final int FALL = 2;
    private static final int REST = 3;
    private static final int CATCH = 4;
    private static final int GASP = 5;
    /** Breaths begun since first sight, and how many of them were the sigh of relief. */
    public int breathNo;
    public int reliefs;
    private double breathPos;
    private final float[] segLength = new float[8];
    private final float[] segFrom = new float[8];
    private final float[] segTo = new float[8];
    private final int[] segKind = new int[8];
    private int segs;
    /** The breath's length and its landmarks, in units of the period: end of the inhale, start of the decay, start of the exhale. */
    private float length;
    private float inhaleEnd;
    private float decayStart;
    private float exhaleStart;
    private float fullHold;
    private float depthShare;
    private float attackIn;
    private float attackOut;
    private float snapPeriod;
    private float buildAtStart;
    private boolean event;
    private int calmLeft;
    private float period = 16.0F;

    // ---- the pose, for the renderer and the crackles (radians and blocks)
    public final float[] lon = new float[8];
    public final float[] lat = new float[8];
    public final float[] turn = new float[8];
    public float radius;
    public float spread;
    public float offsetX;
    public float offsetY;
    public float offsetZ;
    public float envelope;
    /** The twist (degrees) and the most any piece may be rocking about it now (degrees), as last posed. */
    public float twistNow;
    public float wobbleNow;

    static CapitalMotion of(VerdantCapitalBlockEntity be) {
        if (be.clientMotion instanceof CapitalMotion m) {
            return m;
        }
        CapitalMotion m = new CapitalMotion();
        long p = be.getBlockPos().asLong();
        m.seed = (p ^ (p >>> 29)) * 0x9E3779B97F4A7C15L;
        be.clientMotion = m;
        return m;
    }

    // ---------------------------------------------------------------- small helpers
    private static float hash(long seed, int a, int b) {
        long h = seed + a * 0x9E3779B97F4A7C15L + b * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 40) / (float) (1 << 24);
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    private static float ease(float dt, float tau) {
        return 1.0F - (float) Math.exp(-dt / tau);
    }

    /** A row of the breath's table at an arousal: straight lines between its five columns. */
    private static float curve(float[] row, float a) {
        float x = Mth.clamp(a, 0.0F, 1.0F) * 4.0F;
        int i = Math.min((int) x, 3);
        return Mth.lerp(x - i, row[i], row[i + 1]);
    }

    /** As {@link #curve}, but flat up to idle's arousal: idle keeps fed's faint peak (A4's table), then rises to the 0.25 column. */
    private static float peak(float[] row, float a) {
        if (a <= AROUSAL_IDLE) {
            return row[0];
        }
        return a < 0.25F ? Mth.lerp((a - AROUSAL_IDLE) / (0.25F - AROUSAL_IDLE), row[0], row[1]) : curve(row, a);
    }

    static float arousalOf(Drive drive) {
        return switch (drive) {
            case FED -> 0.0F;
            case STARVED -> AROUSAL_STARVED;
            case UNWINDING -> 1.0F;
            default -> AROUSAL_IDLE;
        };
    }

    static float spreadOf(Drive drive) {
        return drive == Drive.FED ? SPREAD_FED : drive == Drive.UNWINDING ? SPREAD_RING : SPREAD_IDLE;
    }

    /** 0 at the ring's tightest spread, 1 at idle's and wider. */
    public static float openness(float base) {
        return Mth.clamp((base - SPREAD_RING) / (SPREAD_IDLE - SPREAD_RING), 0.0F, 1.0F);
    }

    /** The twist (degrees) of every piece about its radial axis for a base spread and the breath on top of it. */
    public static float twist(float base, float breath) {
        return Mth.clamp(60.0F - TWIST_PER_BLOCK * (base + breath * openness(base) - SPREAD_RING), 0.0F, 60.0F);
    }

    /** Seconds per breath: 16 x 0.0875^arousal, drawn toward anger's 3.75 s. */
    private static float period(float a, float anger) {
        double calm = Math.log(16.0) + a * Math.log(0.0875);
        return (float) Math.exp(Mth.lerp(anger, calm, Math.log(ANGER_PERIOD)));
    }

    private boolean angry(VerdantCapitalBlockEntity be, Drive drive, double now) {
        int grace = be.starveGrace();
        return drive == Drive.STARVED && grace > 0 && now - be.starvedSince() >= grace / 2.0;
    }

    static float targetOmega(Drive drive) {
        return switch (drive) {
            case FED -> OMEGA_FED;
            case STARVED -> OMEGA_STARVED;
            case UNWINDING -> OMEGA_UNWINDING;
            default -> 0.0F;
        };
    }

    static float tau(Drive drive) {
        return switch (drive) {
            case FED -> TAU_FED;
            case STARVED -> TAU_STARVED;
            case UNWINDING -> TAU_UNWINDING;
            default -> TAU_IDLE;
        };
    }

    static float targetDark(Drive drive) {
        return drive == Drive.STARVED ? DARK_STARVED : drive == Drive.UNWINDING ? DARK_UNWINDING : 0.0F;
    }

    static float tauSep(Drive drive) {
        return drive == Drive.FED ? TAU_SEP_OPEN : drive == Drive.UNWINDING ? TAU_SEP_SNAP : TAU_SEP_SHUT;
    }

    /**
     * Moves the capital's client animation state on to {@code now} (game time with the partial tick) and writes its pose. A repeat
     * pass of one frame changes nothing.
     */
    public static CapitalMotion advance(VerdantCapitalBlockEntity be, double now) {
        CapitalMotion motion = of(be);
        Drive drive = be.drive();
        float rayTo = drive == Drive.FED ? 1.0F : drive == Drive.UNWINDING ? 0.8F : 0.0F;
        float unwindTo = drive == Drive.UNWINDING ? 1.0F : 0.0F;
        double last = be.clientTime;
        if (Double.isNaN(last) || now < last - 1.0 || now - last > 200.0) {
            // first sight (or back after a long while): it is already doing what it does
            be.clientOmega = targetOmega(drive);
            be.clientDark = targetDark(drive);
            be.clientRay = rayTo;
            be.clientUnwind = unwindTo;
            if (drive == Drive.IDLE) {
                be.clientAngle = 0.0F;
            }
            motion.reset(be, drive, now);
        } else {
            float dt = (float) Mth.clamp(now - last, 0.0, 5.0);
            if (dt > 0.0F) {
                motion.step(be, drive, dt, now); // the rings, the base spread, the poles, the crossing and the breath
                be.clientDark += (targetDark(drive) - be.clientDark) * ease(dt, TAU_DARK);
                float k = ease(dt, TAU_RAY);
                be.clientRay += (rayTo - be.clientRay) * k;
                if (rayTo > 0.0F) {
                    be.clientUnwind += (unwindTo - be.clientUnwind) * k; // the colour holds while the ray fades out
                }
            }
        }
        be.clientTime = now;
        motion.pose(be, now);
        return motion;
    }

    // ---------------------------------------------------------------- first sight
    /** The capital as it stands when first seen (or seen again after a long while): already doing what it does. */
    void reset(VerdantCapitalBlockEntity be, Drive drive, double now) {
        boolean unwinding = drive == Drive.UNWINDING;
        this.arousal = arousalOf(drive);
        this.lastTarget = this.arousal;
        this.reliefPending = false;
        this.anger = this.angry(be, drive, now) ? 1.0F : 0.0F;
        this.cross = unwinding ? 1.0F : 0.0F;
        this.crossRest = 0.0F;
        this.poleOffset = 0.0F;
        this.poleSpin = unwinding ? OMEGA_UNWINDING : 0.0F;
        be.clientSep = spreadOf(drive);
        if (unwinding) {
            be.clientAngle = 0.0F;
            be.clientOmega = 0.0F;
        }
        this.loopOffset = hash(this.seed, 0, 11) * CapitalLoop.LOOP_TICKS;
        this.wobble1 = hash(this.seed, 0, 12) * Mth.TWO_PI;
        this.wobble2 = hash(this.seed, 0, 13) * Mth.TWO_PI;
        this.jiggleTime = hash(this.seed, 0, 14) * 1000.0;
        this.breathNo = 0;
        this.breathPos = hash(this.seed, 0, 15);
        this.calmLeft = 0;
        this.event = false;
        this.buildAtStart = -1.0F;
        this.period = period(this.arousal, this.anger);
        this.startBreath(false);
    }

    // ---------------------------------------------------------------- a frame
    /** Moves everything on by {@code dt} ticks. The renderer has not yet touched the angle or the spread for this frame. */
    void step(VerdantCapitalBlockEntity be, Drive drive, float dt, double now) {
        boolean unwinding = drive == Drive.UNWINDING;
        // On the loop, and until the poles are back in their triforces, the star keeps the ring's tightest spread, whatever the drive says.
        boolean locked = this.cross > 0.0F;
        boolean pinned = locked || this.poleOffset != 0.0F;
        Drive eff = unwinding || pinned ? Drive.UNWINDING : drive;

        float k = ease(dt, tauSep(eff));
        if (!pinned) {
            be.clientSep += (spreadOf(eff) - be.clientSep) * k;
        }
        this.arousal += (arousalOf(eff) - this.arousal) * k;
        boolean compact = eff == Drive.UNWINDING && be.clientSep <= SPREAD_RING + 0.002F * PX;
        if (compact) {
            be.clientSep = SPREAD_RING;
        }

        // the rings
        float rest = Math.round(be.clientAngle / 60.0F) * 60.0F;
        if (locked) {
            be.clientOmega = 0.0F;
        } else {
            float omegaTo = eff == Drive.UNWINDING ? (compact ? 0.0F : OMEGA_UNWINDING) : targetOmega(drive);
            be.clientOmega += (omegaTo - be.clientOmega) * ease(dt, compact ? TAU_SETTLE_TIGHT : tau(eff));
            float turned = be.clientOmega * dt;
            be.clientAngle += turned;
            this.rayAngle += turned;
            if ((drive == Drive.IDLE || compact) && Math.abs(be.clientOmega) < 0.6F) {
                // coming to rest: drawn to the nearest closed alignment (every 60 degrees), so a resting capital is always a true star
                rest = Math.round(be.clientAngle / 60.0F) * 60.0F;
                be.clientAngle += (rest - be.clientAngle) * ease(dt, compact ? TAU_SETTLE_TIGHT : TAU_SETTLE);
            }
        }

        // the crossing: begins once the star is at the ring's tightest and the rings have come to rest, and runs backwards on leaving
        if (unwinding) {
            if (locked || (compact && Math.abs(be.clientOmega) < 0.05F && Math.abs(be.clientAngle - rest) < 0.5F)) {
                if (!locked) {
                    be.clientAngle = rest;
                    be.clientOmega = 0.0F;
                    this.crossRest = Mth.positiveModulo(rest, 360.0F);
                }
                this.cross = Math.min(1.0F, this.cross + dt / CROSS_TICKS);
            }
        } else if (locked) {
            this.cross = Math.max(0.0F, this.cross - dt / CROSS_TICKS);
        }
        be.clientAngle = Mth.positiveModulo(be.clientAngle, 360.0F);

        // the poles: free of their triforces only at the ring's tightest spread (where any pole angle is clear)
        if (unwinding && (compact || locked)) {
            this.poleSpin += (OMEGA_UNWINDING - be.clientOmega - this.poleSpin) * ease(dt, TAU_UNWINDING);
            float turned = this.poleSpin * dt;
            this.poleOffset = Mth.positiveModulo(this.poleOffset + turned, 360.0F);
            this.rayAngle += turned;
            if (this.poleOffset == 0.0F) {
                this.poleOffset = 360.0F - 1.0E-3F;
            }
        } else if (this.poleOffset != 0.0F) {
            // back into the triforce, carrying on the way they were turning to the next place they fit (every 120 degrees)
            float to = (float) Math.floor(this.poleOffset / 120.0F) * 120.0F;
            float moved = -Math.min(-OMEGA_UNWINDING * dt, (this.poleOffset - to) * ease(dt, 4.0F));
            this.poleOffset += moved;
            this.rayAngle += moved;
            this.poleSpin = 0.0F;
            if (this.poleOffset - to < 0.3F) {
                this.poleOffset = 0.0F;
            }
        } else {
            this.poleSpin = 0.0F;
        }
        this.rayAngle = Mth.positiveModulo(this.rayAngle, 360.0F);

        // anger: the latter half of a starved capital's grace; it gives way to panic while the capital unwinds
        boolean angry = this.angry(be, drive, now);
        this.anger += ((angry ? 1.0F : 0.0F) - this.anger) * ease(dt, angry ? TAU_ANGER_IN : unwinding ? TAU_ANGER_TO_PANIC : TAU_ANGER_OUT);

        // one sigh of relief when the drive asks for much less arousal than before
        float target = arousalOf(drive);
        if (target <= this.lastTarget - RELIEF_DROP) {
            this.reliefPending = true;
        } else if (target > this.lastTarget) {
            this.reliefPending = false;
        }
        this.lastTarget = target;

        // the breath
        float seconds = dt / 20.0F;
        this.period = period(this.arousal, this.anger);
        float pace = this.reliefPending ? Math.min(this.period, this.snapPeriod) : this.period; // the breath before the sigh ends at its own pace
        this.breathPos += seconds / (pace * this.length);
        if (this.breathPos >= 1.0) {
            this.breathPos = Math.min(this.breathPos - 1.0, 0.5);
            this.breathNo++;
            boolean relief = this.reliefPending && this.cross == 0.0F && this.arousal - target < 0.1F;
            this.startBreath(relief);
        }

        // sway and jiggle clocks run faster with arousal (A4)
        this.wobble1 += Mth.TWO_PI * (0.4F + 0.9F * this.arousal) * seconds;
        this.wobble2 += Mth.TWO_PI * (0.9F + 2.0F * this.arousal) * seconds;
        this.jiggleTime += dt * (0.25F + 0.75F * this.arousal);
    }

    // ---------------------------------------------------------------- the breath
    private void seg(int kind, float length, float from, float to) {
        this.segKind[this.segs] = kind;
        this.segLength[this.segs] = length;
        this.segFrom[this.segs] = from;
        this.segTo[this.segs] = to;
        this.segs++;
    }

    /**
     * Fixes the shape of the breath now starting from the arousal and anger of this moment: the four phases' shares of the period,
     * this breath's own length and depth (jitter: a hash of the position and the breath's number), and whether it is one of the
     * events (a sigh, a stacked inhale, a gasp, or the one sigh of relief). The rate itself keeps following the arousal.
     */
    private void startBreath(boolean relief) {
        float a = this.arousal;
        float g = this.anger;
        int n = this.breathNo;
        float t = period(a, g);
        float full = 0.25F * (float) Math.pow(1.0F - a, 2.5) * (1.0F - g);
        float empty = Mth.lerp(g, 0.25F * (float) Math.pow(1.0F - a, 1.5), ANGER_PAUSE / ANGER_PERIOD);
        float ratio = Mth.lerp(g, 1.0F - 1.35F * a + 1.5F * a * a, ANGER_RATIO);
        float rest = 1.0F - full - empty;
        float in = rest * ratio / (1.0F + ratio);
        float out = rest / (1.0F + ratio);
        float calm = this.calmLeft > 0 ? 0.5F : 1.0F;
        if (this.calmLeft > 0) {
            this.calmLeft--;
        }
        float jitterLength = Mth.lerp(g, curve(JITTER_LENGTH, a), ANGER_JITTER) * calm;
        float jitterDepth = Mth.lerp(g, curve(JITTER_DEPTH, a), ANGER_JITTER) * calm;
        float stretch = 1.0F + jitterLength * (2.0F * hash(this.seed, n, 1) - 1.0F);
        float mean = Mth.lerp(g, curve(DEPTH, a), 1.0F);
        this.depthShare = Mth.clamp(mean * (1.0F + jitterDepth * (2.0F * hash(this.seed, n, 2) - 1.0F)), 0.3F * mean, 1.0F);
        this.attackIn = curve(ATTACK, a);
        this.attackOut = Mth.lerp(g, this.attackIn / 2.0F, ANGER_EXHALE_ATTACK);
        this.snapPeriod = t;

        // which event, if any: never two in a row
        float roll = hash(this.seed, n, 3);
        float gasp = a >= 0.85F ? 1.0F / (4.0F + 3.0F * hash(this.seed, n, 4)) : 0.0F;
        float stacked = Math.max(0.0F, 1.0F - Math.abs(a - 0.75F) / 0.25F) / (6.0F + 4.0F * hash(this.seed, n, 5));
        float sigh = Math.max(0.0F, 1.0F - Math.abs(a - 0.5F) / 0.25F) * (1.0F - g) / (12.0F + 6.0F * hash(this.seed, n, 6));
        boolean was = this.event;
        this.event = true;
        this.segs = 0;
        if (relief) {
            // the sigh of relief: a double inhale to the whole headroom, then a long exhale
            this.reliefPending = false;
            this.reliefs++;
            this.depthShare = 1.0F;
            this.seg(RISE, 0.7F * in, 0.0F, 0.7F);
            this.seg(CATCH, 0.15F / t, 0.7F, 0.7F);
            this.seg(RISE, 0.3F * in, 0.7F, 1.0F);
            this.seg(FALL, 2.5F * out, 1.0F, 0.0F);
            this.seg(REST, empty, 0.0F, 0.0F);
        } else if (!was && roll < gasp) {
            // a gasp: a fast inhale to the whole headroom, a catch at the top with the tremble full, an exhale in two steps
            this.depthShare = 1.0F;
            this.seg(RISE, 0.6F * in, 0.0F, 1.0F);
            this.seg(GASP, 0.25F / t, 1.0F, 1.0F);
            this.seg(FALL, 0.5F * out, 1.0F, 0.5F);
            this.seg(CATCH, 0.08F / t, 0.5F, 0.5F);
            this.seg(FALL, 0.5F * out, 0.5F, 0.0F);
            this.seg(REST, empty, 0.0F, 0.0F);
        } else if (!was && roll < gasp + stacked) {
            // a stacked inhale: it catches part way, then goes on to the whole headroom
            this.depthShare = 1.0F;
            this.seg(RISE, 0.5F * in, 0.0F, 0.6F);
            this.seg(CATCH, 0.1F / t, 0.6F, 0.6F);
            this.seg(RISE, 0.5F * in, 0.6F, 1.0F);
            this.seg(TOP, full, 1.0F, 1.0F);
            this.seg(FALL, out, 1.0F, 0.0F);
            this.seg(REST, empty, 0.0F, 0.0F);
        } else if (!was && roll < gasp + stacked + sigh) {
            // a sigh: long, slow and to the whole headroom; the next three breaths are steadier for it
            this.depthShare = 1.0F;
            this.calmLeft = 3;
            this.seg(RISE, 1.3F * in, 0.0F, 1.0F);
            this.seg(TOP, full, 1.0F, 1.0F);
            this.seg(FALL, 1.5F * out, 1.0F, 0.0F);
            this.seg(REST, 2.0F * empty, 0.0F, 0.0F);
        } else {
            this.event = false;
            this.seg(RISE, in * stretch, 0.0F, 1.0F);
            this.seg(TOP, full * stretch, 1.0F, 1.0F);
            this.seg(FALL, out * stretch, 1.0F, 0.0F);
            this.seg(REST, empty * stretch, 0.0F, 0.0F);
        }

        // landmarks for the tremble envelope
        float previousEnd = this.buildAtStart < 0.0F ? -1.0F : this.buildEnd();
        float at = 0.0F;
        this.inhaleEnd = 0.0F;
        this.decayStart = 0.0F;
        this.exhaleStart = -1.0F;
        this.fullHold = 0.0F;
        for (int i = 0; i < this.segs; i++) {
            int kind = this.segKind[i];
            if (kind == FALL && this.exhaleStart < 0.0F) {
                this.exhaleStart = at;
            }
            at += this.segLength[i];
            if (this.exhaleStart < 0.0F) {
                if (kind == RISE || kind == CATCH) {
                    this.inhaleEnd = at;
                    this.decayStart = at;
                } else if (kind == GASP) {
                    this.decayStart = at;
                } else if (kind == TOP) {
                    this.fullHold = this.segLength[i];
                }
            }
        }
        this.length = at;
        this.buildAtStart = previousEnd < 0.0F ? this.buildEnd() : previousEnd;
    }

    /** How far the tremble's build has come by the end of this breath (it runs from the start of an exhale to the end of the next inhale). */
    private float buildEnd() {
        float exhale = this.length - this.exhaleStart;
        return exhale / (exhale + this.inhaleEnd);
    }

    private static final double DECAY_END = Math.exp(-4.0);

    /**
     * Reads the breath at its present phase: sets {@link #envelope} (the tremble envelope, 0 to 1) and returns how far the sphere
     * is expanded, 0 to 1 of this breath's depth.
     */
    private float breathe() {
        float pos = (float) (this.breathPos * this.length);
        float at = 0.0F;
        float level = 0.0F;
        int kind = REST;
        for (int i = 0; i < this.segs; i++) {
            float len = this.segLength[i];
            if (pos < at + len || i == this.segs - 1) {
                kind = this.segKind[i];
                float x = len > 1.0E-6F ? Mth.clamp((pos - at) / len, 0.0F, 1.0F) : 1.0F;
                if (kind == RISE) {
                    x = smooth((float) Math.pow(x, 1.0F - this.attackIn));
                } else if (kind == FALL) {
                    x = smooth((float) Math.pow(x, 1.0F - this.attackOut));
                }
                level = Mth.lerp(x, this.segFrom[i], this.segTo[i]);
                break;
            }
            at += len;
        }
        float floor = curve(FLOOR, this.arousal);
        float steep = curve(STEEPNESS, this.arousal);
        // the build: from the start of the exhale, through the pause, to the end of the next inhale
        float u;
        if (pos < this.inhaleEnd) {
            u = this.buildAtStart + (1.0F - this.buildAtStart) * pos / this.inhaleEnd;
        } else if (pos >= this.exhaleStart) {
            u = (pos - this.exhaleStart) / (this.length - this.exhaleStart + this.inhaleEnd);
        } else {
            u = 0.0F;
        }
        float build = (float) ((Math.exp(steep * u) - 1.0) / (Math.exp(steep) - 1.0));
        // the decay: from the end of the inhale over the full hold (at least half a second), running into the exhale if the hold is shorter
        float decay = 0.0F;
        if (pos >= this.decayStart) {
            float v = (pos - this.decayStart) * this.period / Math.max(this.fullHold * this.period, 0.5F);
            decay = v >= 1.0F ? 0.0F : (float) ((Math.exp(-4.0 * v) - DECAY_END) / (1.0 - DECAY_END));
        }
        float e = kind == GASP ? 1.0F : Math.max(build, decay);
        this.envelope = floor + (1.0F - floor) * Mth.clamp(e, 0.0F, 1.0F);
        return level;
    }

    // ---------------------------------------------------------------- the pose
    private static float sample(float[] keys, float ticks) {
        float x = Mth.positiveModulo(ticks / CapitalLoop.KEY_TICKS, (float) CapitalLoop.KEYS);
        int i = Math.min((int) x, CapitalLoop.KEYS - 1);
        float f = x - i;
        int n = CapitalLoop.KEYS;
        float p0 = keys[(i + n - 1) % n];
        float p1 = keys[i];
        float p2 = keys[(i + 1) % n];
        float p3 = keys[(i + 2) % n];
        return 0.5F * (2.0F * p1 + (p2 - p0) * f + (2.0F * p0 - 5.0F * p1 + 4.0F * p2 - p3) * f * f + (3.0F * p1 - p0 - 3.0F * p2 + p3) * f * f * f);
    }

    /** Value noise in -1..1: a hash at the whole steps, smoothed between them. */
    private float noise(float x, int channel) {
        int i = Mth.floor(x);
        float f = smooth(x - i);
        return Mth.lerp(f, hash(this.seed, i, channel), hash(this.seed, i + 1, channel)) * 2.0F - 1.0F;
    }

    /**
     * Writes the pose for this moment: the sphere's radius, where each of the eight pieces stands on it and how it is turned
     * (pieces 0 and 4 are the upper and lower poles, 1-3 and 5-7 the waist pieces of the upper and lower triforce), and the
     * jiggle's offset of the whole capital. A pure function of the state and the time, so drawing a frame twice changes nothing.
     */
    void pose(VerdantCapitalBlockEntity be, double now) {
        float level = this.breathe();
        float a = this.arousal;
        float base = be.clientSep;
        float open = openness(base);
        float headroom = Math.min(a <= 0.5F ? HEADROOM_OPEN : Mth.lerp((a - 0.5F) / 0.5F, HEADROOM_OPEN, HEADROOM_TIGHT), Mth.lerp(open, HEADROOM_TIGHT, HEADROOM_OPEN));
        float breath = this.depthShare * headroom * level;
        float h = this.cross;
        float compacted = base - (SPREAD_RING - SPREAD_TIGHT) * smooth((h - CROSS_COMPACT_FROM) / (1.0F - CROSS_COMPACT_FROM));
        this.spread = Math.min(compacted + breath, SPREAD_TOP);
        this.radius = STAR_RADIUS / 2.0F + this.spread;
        float psi = twist(base, breath);
        this.twistNow = psi;

        // the wobble: every piece rocks about its radial axis on its own phase; never more than the spread allows
        float peak = Math.min(peak(WOBBLE, a), 4.0F - 2.5F * open) * this.envelope;
        this.wobbleNow = peak;
        float tremor = curve(TREMOR, a);
        float seconds = (float) (now % 72000.0) / 20.0F;

        float angle = be.clientAngle;
        float t = (float) ((now + this.loopOffset) % CapitalLoop.LOOP_TICKS);
        int restStep = Math.round(this.crossRest / 60.0F);
        for (int i = 0; i < 8; i++) {
            boolean upper = i < 4;
            float sign = upper ? 1.0F : -1.0F;
            int c = i % 4;
            float p = i * 2.399F;
            float f3 = 7.0F + 2.0F * hash(0x5D1CE5L, i, 7);
            float rock = peak * ((1.0F - tremor) * (0.6F * Mth.sin((float) (this.wobble1 % Mth.TWO_PI) + p) + 0.4F * Mth.sin((float) (this.wobble2 % Mth.TWO_PI) + 1.7F * p))
                    + tremor * Mth.sin(Mth.TWO_PI * f3 * seconds + 2.9F * p));
            float lonDeg;
            float latDeg;
            float turnDeg;
            if (c == 0) {
                lonDeg = 0.0F;
                latDeg = 90.0F * sign;
                turnDeg = upper ? 30.0F - psi + angle + this.poleOffset : 90.0F + psi - (60.0F - angle - this.poleOffset);
            } else {
                float home = upper ? (c - 1) * 120.0F + angle : (c - 1) * 120.0F + 60.0F - angle;
                lonDeg = home;
                latDeg = -sign * LAT_WAIST;
                turnDeg = upper ? 90.0F - psi : 30.0F + psi;
                if (h > 0.0F) {
                    // its place round the waist, in sixths of a turn from the upper triforce's first piece, sets where it is in the wave
                    int k = upper ? 2 * (c - 1) : Math.floorMod(2 * (c - 1) + 1 - 2 * restStep, 6);
                    float at = t + k * CapitalLoop.PIECE_SHIFT_TICKS;
                    lonDeg += h * sample(CapitalLoop.LON, at);
                    latDeg = Mth.lerp(h, latDeg, sample(CapitalLoop.LAT, at));
                    turnDeg += sign * h * sample(CapitalLoop.PIROUETTE, at);
                }
            }
            this.lon[i] = lonDeg * Mth.DEG_TO_RAD;
            this.lat[i] = latDeg * Mth.DEG_TO_RAD;
            this.turn[i] = (turnDeg + rock) * Mth.DEG_TO_RAD;
        }

        // the jiggle: the whole capital, rough and never the same twice; held inside the block
        float jiggle = Math.min(peak(JIGGLE, a) * PX * this.envelope, Math.max(0.0F, JIGGLE_ROOM - this.spread));
        float jt = (float) (this.jiggleTime % 100000.0);
        this.offsetX = jiggle * this.rough(jt, 20);
        this.offsetY = jiggle * this.rough(jt, 30);
        this.offsetZ = jiggle * this.rough(jt, 40);
    }

    private float rough(float t, int channel) {
        return 0.5F * this.noise(0.23F * t, channel) + 0.3F * this.noise(0.61F * t, channel + 1) + 0.2F * this.noise(1.37F * t, channel + 2);
    }
}
