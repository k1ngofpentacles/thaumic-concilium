package dev.movingcastle.thaumicconcilium.client;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.item.CrimsonDagger;
import dev.movingcastle.thaumicconcilium.network.DaggerFxPayload;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The Crimson Dagger's animation clock, shared by every option (the option's numbers are in a {@link DaggerPose}).
 * <p>
 * Who is raising, stabbing or cutting is reported by the server ({@link DaggerFxPayload}) and kept here per entity id, as
 * {@link BraceletCastState} does for bracelets. {@link #frame} turns that into a {@link Frame} for the entity at a partial
 * tick: the phase, ticks since it started, and how far the arm is raised. The development switch
 * {@code -Dthaumicconcilium.dagger_dev=<A|B|C>[:<phase>:<progress 0..1>]} freezes every dagger holder (the player and the
 * harness dummy) at a phase; empty means live. The letter must match the built option ({@link DaggerPoses#ACTIVE}), else live.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID, value = Dist.CLIENT)
public final class DaggerAnim {
    public static final String DEV_PROPERTY = "thaumicconcilium.dagger_dev";

    public enum Mode { REST, RAISE, LOWER, STAB, CUT }

    /**
     * One entity's dagger pose at one instant. {@code ticks}: ticks since the phase began (fractional). {@code r}: how far
     * the arm is raised, 0..1 (1 in a stab: it starts from the hold). {@code afterR}: in a stab, what the recovery returns
     * to (0 = rest, 1 = hold; depends on {@link CrimsonDagger#STAB_SPENDS_CHARGE}). {@code mainHand}: the dagger is in
     * the main hand.
     */
    public record Frame(Mode mode, float ticks, float r, float afterR, boolean mainHand) {
        public static final Frame REST = new Frame(Mode.REST, 0.0F, 0.0F, 0.0F, true);
    }

    /**
     * One arm's third-person target: {@code w} how far from vanilla (0..1), then P/Y/R in degrees (the memo's convention:
     * xRot, yaw towards the midline, roll away from the body). Weight 0 means untouched. {@code forward} moves
     * the shoulder forward, in model pixels (scaled by the weight), so a raised fist clears the head. There is no lift:
     * {@code HumanoidModel.setupAnim} sets the arms' y again after the pose transformer has run, so a 12 px arm on a
     * shoulder 10 px under the head top brings a fist to the top of the head and no higher.
     */
    public record Arm(float w, float p, float y, float r, float forward) {
        public static final Arm REST = new Arm(0.0F, 0.0F, 0.0F, 0.0F);

        public Arm(float w, float p, float y, float r) {
            this(w, p, y, r, 0.0F);
        }

        public static Arm of(float p, float y, float r) {
            return new Arm(1.0F, p, y, r);
        }

        public static Arm of(float p, float y, float r, float forward) {
            return new Arm(1.0F, p, y, r, forward);
        }

        /** Weight scaled by {@code k}. */
        public Arm weighted(float k) {
            return new Arm(w * k, p, y, r, forward);
        }

        /** Blend between two targets; an arm with weight 0 contributes only its weight. */
        public static Arm lerp(float e, Arm a, Arm b) {
            float w = Mth.lerp(e, a.w, b.w);
            if (a.w <= 0.0F) {
                return new Arm(w, b.p, b.y, b.r, b.forward);
            }
            if (b.w <= 0.0F) {
                return new Arm(w, a.p, a.y, a.r, a.forward);
            }
            return new Arm(w, Mth.lerp(e, a.p, b.p), Mth.lerp(e, a.y, b.y), Mth.lerp(e, a.r, b.r), Mth.lerp(e, a.forward, b.forward));
        }
    }

    private static final class State {
        Mode mode = Mode.REST;
        float start;
        float fromR;
        boolean using;
        boolean mainHand = true;
    }

    private static final Map<Integer, State> STATES = new HashMap<>();
    private static String devRaw = "";
    private static Mode devMode = Mode.REST;
    private static float devProgress;
    private static char devLetter;

    private DaggerAnim() {
    }

    // ------------------------------------------------------------------ easing
    public static float clamp01(float t) {
        return Mth.clamp(t, 0.0F, 1.0F);
    }

    public static float easeOutCubic(float t) {
        float u = 1.0F - clamp01(t);
        return 1.0F - u * u * u;
    }

    public static float easeInQuad(float t) {
        t = clamp01(t);
        return t * t;
    }

    public static float easeOutQuad(float t) {
        float u = 1.0F - clamp01(t);
        return 1.0F - u * u;
    }

    /** {@code t} within [from, to], clamped 0..1. */
    public static float span(float ticks, float from, float to) {
        return clamp01((ticks - from) / (to - from));
    }

    // ------------------------------------------------------------------ state
    public static void receive(DaggerFxPayload fx) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        float now = level.getGameTime();
        State s = STATES.computeIfAbsent(fx.entityId(), id -> new State());
        s.mainHand = fx.mainHand();
        switch (fx.mode()) {
            case DaggerFxPayload.RAISE -> {
                s.mode = Mode.RAISE;
                s.start = now;
                s.using = true;
            }
            case DaggerFxPayload.END -> {
                s.using = false;
                if (s.mode == Mode.RAISE) {
                    s.fromR = clamp01((now - s.start) / CrimsonDagger.RAISE_TICKS);
                    s.mode = Mode.LOWER;
                    s.start = now;
                } else if (s.mode == Mode.STAB && now - s.start >= CrimsonDagger.STAB_TICKS) {
                    // A finished stab with use still held is shown as a raise (see frame): lower from where that raise is.
                    s.fromR = CrimsonDagger.STAB_SPENDS_CHARGE
                            ? clamp01((now - s.start - CrimsonDagger.STAB_TICKS) / CrimsonDagger.RAISE_TICKS) : 1.0F;
                    s.mode = Mode.LOWER;
                    s.start = now;
                } else if (s.mode != Mode.STAB && s.mode != Mode.CUT) {
                    s.mode = Mode.REST;
                }
            }
            case DaggerFxPayload.STAB -> {
                s.mode = Mode.STAB;
                s.start = now;
            }
            case DaggerFxPayload.CUT -> {
                s.mode = Mode.CUT;
                s.start = now;
                s.using = false;
            }
            default -> {
            }
        }
    }

    /** The dagger's pose for {@code entity} at {@code partialTick}: {@link Frame#REST} when it is not doing anything. */
    public static Frame frame(LivingEntity entity, float partialTick) {
        Frame dev = dev(entity);
        if (dev != null) {
            return dev;
        }
        State s = STATES.get(entity.getId());
        if (s == null || s.mode == Mode.REST) {
            if (entity.isUsingItem() && entity.getUseItem().getItem() instanceof CrimsonDagger) {
                // The server's RAISE has not arrived yet (a tick): follow the client's own use clock, not vanilla's spear pose.
                float t = entity.getTicksUsingItem() + partialTick;
                return new Frame(Mode.RAISE, t, clamp01(t / CrimsonDagger.RAISE_TICKS), 0.0F,
                        entity.getUsedItemHand() == InteractionHand.MAIN_HAND);
            }
            return Frame.REST;
        }
        float ticks = entity.level().getGameTime() + partialTick - s.start;
        boolean spends = CrimsonDagger.STAB_SPENDS_CHARGE;
        switch (s.mode) {
            case RAISE:
                return new Frame(Mode.RAISE, ticks, clamp01(ticks / CrimsonDagger.RAISE_TICKS), 0.0F, s.mainHand);
            case LOWER:
                return ticks >= CrimsonDagger.LOWER_TICKS ? Frame.REST
                        : new Frame(Mode.LOWER, ticks, s.fromR * (1.0F - ticks / CrimsonDagger.LOWER_TICKS), 0.0F, s.mainHand);
            case STAB:
                if (ticks < CrimsonDagger.STAB_TICKS) {
                    return new Frame(Mode.STAB, ticks, 1.0F, spends ? 0.0F : 1.0F, s.mainHand);
                }
                if (!s.using) {
                    return Frame.REST;
                }
                float after = ticks - CrimsonDagger.STAB_TICKS;
                return spends ? new Frame(Mode.RAISE, after, clamp01(after / CrimsonDagger.RAISE_TICKS), 0.0F, s.mainHand)
                        : new Frame(Mode.RAISE, CrimsonDagger.RAISE_TICKS + after, 1.0F, 0.0F, s.mainHand);
            case CUT:
                return ticks >= CrimsonDagger.CUT_TICKS ? Frame.REST : new Frame(Mode.CUT, ticks, 0.0F, 0.0F, s.mainHand);
            default:
                return Frame.REST;
        }
    }

    /** The harness marker due on this client tick for {@code entity} ({@code dagger_stab}, {@code dagger_cut}), else null. */
    public static String marker(LivingEntity entity) {
        State s = STATES.get(entity.getId());
        if (s == null) {
            return null;
        }
        int ticks = (int) (entity.level().getGameTime() - s.start);
        if (s.mode == Mode.STAB && ticks == DaggerPoses.ACTIVE.stabContactTick()) {
            return "dagger_stab";
        }
        if (s.mode == Mode.CUT && ticks == CrimsonDagger.CUT_MARK_TICK) {
            return "dagger_cut";
        }
        return null;
    }

    /** The arm holding the dagger. */
    public static HumanoidArm holdingArm(LivingEntity entity, Frame frame) {
        return frame.mainHand() ? entity.getMainArm() : entity.getMainArm().getOpposite();
    }

    /** The hand that holds the dagger. */
    public static InteractionHand holdingHand(Frame frame) {
        return frame.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    private static Frame dev(LivingEntity entity) {
        String raw = System.getProperty(DEV_PROPERTY, "");
        if (!raw.equals(devRaw)) {
            devRaw = raw;
            devMode = Mode.REST;
            devLetter = 0;
            String[] parts = raw.split(":");
            if (parts.length == 3 && !parts[0].isEmpty()) {
                devLetter = Character.toUpperCase(parts[0].charAt(0));
                try {
                    devProgress = clamp01(Float.parseFloat(parts[2]));
                    devMode = switch (parts[1].toLowerCase()) {
                        case "raise" -> Mode.RAISE;
                        case "stab" -> Mode.STAB;
                        case "cut" -> Mode.CUT;
                        default -> Mode.REST;
                    };
                } catch (NumberFormatException e) {
                    devMode = Mode.REST;
                }
            }
        }
        if (devMode == Mode.REST || devLetter != DaggerPoses.ACTIVE.letter()
                || !(entity.getMainHandItem().getItem() instanceof CrimsonDagger)) {
            return null;
        }
        return switch (devMode) {
            case RAISE -> new Frame(Mode.RAISE, devProgress * CrimsonDagger.RAISE_TICKS, devProgress, 0.0F, true);
            case STAB -> new Frame(Mode.STAB, devProgress * CrimsonDagger.STAB_TICKS, 1.0F,
                    CrimsonDagger.STAB_SPENDS_CHARGE ? 0.0F : 1.0F, true);
            default -> new Frame(Mode.CUT, devProgress * CrimsonDagger.CUT_TICKS, 0.0F, 0.0F, true);
        };
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            STATES.clear();
            return;
        }
        Iterator<Map.Entry<Integer, State>> it = STATES.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            if (!(level.getEntity(e.getKey()) instanceof LivingEntity living)
                    || !(living.getMainHandItem().getItem() instanceof CrimsonDagger
                    || living.getOffhandItem().getItem() instanceof CrimsonDagger)
                    || frame(living, 0.0F).mode() == Mode.REST) {
                it.remove();
            }
        }
    }
}
