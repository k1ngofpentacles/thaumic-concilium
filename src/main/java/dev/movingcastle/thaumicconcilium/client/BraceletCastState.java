package dev.movingcastle.thaumicconcilium.client;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.network.BraceletFxPayload;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;

/**
 * Who is casting from a bracelet, as the server reports it ({@link BraceletFxPayload}), and how far their casting arm is
 * raised: up over a few ticks, held while a channelled or charged spell runs, down after a single cast. Also spawns the
 * cast's particles at the raised wrist: a bright spray in the spell's colour on each cast, and a slow swirl of motes
 * while channelling.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID, value = Dist.CLIENT)
public final class BraceletCastState {
    private static final float RAISE_TICKS = 3.0F;
    private static final float LOWER_TICKS = 5.0F;
    /** A single cast holds the arm up this long. */
    private static final int BURST_HOLD = 9;

    private static final class State {
        float raisedAt;
        float lowerAt = Float.MAX_VALUE;
        boolean holding;
        int color;
        boolean mainHand;
    }

    private static final Map<Integer, State> STATES = new HashMap<>();

    private BraceletCastState() {
    }

    public static void receive(BraceletFxPayload fx) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        float now = level.getGameTime();
        State state = STATES.get(fx.entityId());
        if (state == null || raise(state, now) <= 0.0F) {
            state = new State();
            state.raisedAt = now;
            STATES.put(fx.entityId(), state);
        }
        if (fx.mode() != BraceletFxPayload.RELEASE) {
            state.color = fx.color();
            state.mainHand = fx.mainHand();
        }
        switch (fx.mode()) {
            case BraceletFxPayload.HOLD -> {
                state.holding = true;
                state.lowerAt = Float.MAX_VALUE;
            }
            case BraceletFxPayload.RELEASE -> {
                state.holding = false;
                state.lowerAt = now + 2;
            }
            default -> {
                if (!state.holding) {
                    state.lowerAt = now + BURST_HOLD;
                }
                Entity entity = level.getEntity(fx.entityId());
                if (entity instanceof LivingEntity living) {
                    burst(level, living, state.color);
                }
            }
        }
    }

    /** 0 (down) to 1 (fully raised). */
    public static float raise(LivingEntity entity, float partialTick) {
        State state = STATES.get(entity.getId());
        return state == null ? 0.0F : raise(state, entity.level().getGameTime() + partialTick);
    }

    private static float raise(State state, float time) {
        float up = Mth.clamp((time - state.raisedAt) / RAISE_TICKS, 0.0F, 1.0F);
        if (state.holding || time < state.lowerAt) {
            return up;
        }
        return Math.min(up, 1.0F - Mth.clamp((time - state.lowerAt) / LOWER_TICKS, 0.0F, 1.0F));
    }

    /** The casting arm: the main arm for a bracelet held in the main hand, else the off arm (worn or shield slot). */
    public static HumanoidArm castingArm(LivingEntity entity) {
        State state = STATES.get(entity.getId());
        return state != null && state.mainHand ? entity.getMainArm() : entity.getMainArm().getOpposite();
    }

    /** Roughly where the raised casting hand is, in the world. */
    public static Vec3 hand(LivingEntity entity, float partialTick) {
        Vec3 look = entity.getViewVector(partialTick);
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        Vec3 right = new Vec3(-flat.z, 0, flat.x);
        double side = castingArm(entity) == HumanoidArm.RIGHT ? 1 : -1;
        Vec3 eye = entity.getEyePosition(partialTick);
        return eye.add(0, -0.3, 0).add(right.scale(0.33 * side)).add(look.scale(0.7));
    }

    private static DustParticleOptions dust(int color, float size) {
        return new DustParticleOptions(new Vector3f((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F), size);
    }

    private static void burst(ClientLevel level, LivingEntity entity, int color) {
        RandomSource random = level.getRandom();
        Vec3 hand = hand(entity, 1.0F);
        Vec3 look = entity.getViewVector(1.0F);
        for (int i = 0; i < 14; i++) {
            Vec3 v = look.scale(0.15 + random.nextDouble() * 0.2).add((random.nextDouble() - 0.5) * 0.12,
                    (random.nextDouble() - 0.5) * 0.12, (random.nextDouble() - 0.5) * 0.12);
            level.addParticle(dust(color, 0.6F + random.nextFloat() * 0.5F), hand.x, hand.y, hand.z, v.x, v.y, v.z);
        }
        for (int i = 0; i < 4; i++) {
            level.addParticle(ParticleTypes.END_ROD, hand.x, hand.y, hand.z, look.x * 0.06 + (random.nextDouble() - 0.5) * 0.04,
                    look.y * 0.06 + (random.nextDouble() - 0.5) * 0.04, look.z * 0.06 + (random.nextDouble() - 0.5) * 0.04);
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            STATES.clear();
            return;
        }
        float now = level.getGameTime();
        Iterator<Map.Entry<Integer, State>> it = STATES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, State> e = it.next();
            State state = e.getValue();
            Entity entity = level.getEntity(e.getKey());
            if (!(entity instanceof LivingEntity living) || (!state.holding && raise(state, now) <= 0.0F && now > state.lowerAt)) {
                it.remove();
                continue;
            }
            if (state.holding && raise(state, now) >= 1.0F) {
                // Channelling: motes circling the wrist, drifting in.
                RandomSource random = level.getRandom();
                Vec3 hand = hand(living, 1.0F);
                double a = now * 0.5 + random.nextDouble();
                double r = 0.35;
                level.addParticle(dust(state.color, 0.5F), hand.x + Math.cos(a) * r, hand.y + (random.nextDouble() - 0.5) * 0.3,
                        hand.z + Math.sin(a) * r, -Math.cos(a) * 0.04, 0.0, -Math.sin(a) * 0.04);
            }
        }
    }
}
