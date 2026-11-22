package dev.movingcastle.thaumicconcilium.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.network.CrimsonStatePayload;
import java.util.function.Supplier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * A player's crimson state (original: {@code TCPlayerCapabilities} ethereal, pontifexRobeToggle, chainedTime, fleshAmount,
 * protolimbs): one saved data attachment, synced to its owner when it changes by {@link CrimsonStatePayload} (never per tick).
 * <p>{@code chainedTime} is the one field that counts down: server and client each decrement their own copy every tick and
 * the server syncs only when it is set. {@code fleshAmount} is health held back: {@link #applyFlesh} mirrors it into the
 * max-health modifier {@value #FLESH_ID}. {@code protolimbs} is kept for phase 8 and unused now.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class CrimsonState {
    public static final ResourceLocation FLESH_ID = ThaumicConcilium.id("flesh");

    public record State(int fleshAmount, boolean ethereal, boolean pontifexRobeToggle, int chainedTime, int protolimbs) {
        public static final State DEFAULT = new State(0, false, false, 0, 0);
        public static final Codec<State> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("fleshAmount", 0).forGetter(State::fleshAmount),
                Codec.BOOL.optionalFieldOf("ethereal", false).forGetter(State::ethereal),
                Codec.BOOL.optionalFieldOf("pontifexRobeToggle", false).forGetter(State::pontifexRobeToggle),
                Codec.INT.optionalFieldOf("chainedTime", 0).forGetter(State::chainedTime),
                Codec.INT.optionalFieldOf("protolimbs", 0).forGetter(State::protolimbs)).apply(i, State::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, State::fleshAmount,
                ByteBufCodecs.BOOL, State::ethereal,
                ByteBufCodecs.BOOL, State::pontifexRobeToggle,
                ByteBufCodecs.VAR_INT, State::chainedTime,
                ByteBufCodecs.VAR_INT, State::protolimbs,
                State::new);

        public State withFlesh(int v) {
            return new State(Math.max(0, v), ethereal, pontifexRobeToggle, chainedTime, protolimbs);
        }

        public State withEthereal(boolean v) {
            return new State(fleshAmount, v, pontifexRobeToggle, chainedTime, protolimbs);
        }

        public State withToggle(boolean v) {
            return new State(fleshAmount, ethereal, v, chainedTime, protolimbs);
        }

        public State withChained(int v) {
            return new State(fleshAmount, ethereal, pontifexRobeToggle, Math.max(0, v), protolimbs);
        }

        public State withProtolimbs(int v) {
            return new State(fleshAmount, ethereal, pontifexRobeToggle, chainedTime, Math.max(0, v));
        }
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ThaumicConcilium.MODID);
    public static final Supplier<AttachmentType<State>> ATTACHMENT = ATTACHMENTS.register("crimson_state",
            () -> AttachmentType.builder(() -> State.DEFAULT).serialize(State.CODEC).copyOnDeath().build());

    private CrimsonState() {
    }

    public static State get(Player player) {
        return player.hasData(ATTACHMENT) ? player.getData(ATTACHMENT) : State.DEFAULT;
    }

    /** True when the player has any crimson state worth ticking (the early-out for everyone else). */
    public static boolean active(Player player) {
        if (!player.hasData(ATTACHMENT)) {
            return false;
        }
        State s = player.getData(ATTACHMENT);
        return s.ethereal() || s.pontifexRobeToggle() || s.chainedTime() > 0;
    }

    public static boolean isEthereal(Player player) {
        return get(player).ethereal();
    }

    /** Replaces the state; on the server this syncs it to the owner and refreshes the flesh modifier and the no-clip flag. */
    public static void set(Player player, State state) {
        State old = get(player);
        if (old.equals(state)) {
            return;
        }
        player.setData(ATTACHMENT, state);
        if (old.ethereal() && !state.ethereal()) {
            player.noPhysics = false;
        }
        if (player instanceof ServerPlayer server) {
            if (old.fleshAmount() != state.fleshAmount()) {
                applyFlesh(server);
            }
            sync(server);
        }
    }

    public static void addFlesh(Player player, int n) {
        State s = get(player);
        set(player, s.withFlesh(s.fleshAmount() + n));
    }

    /** Sends the owner's state to their client. */
    public static void sync(ServerPlayer player) {
        if (!(player instanceof FakePlayer)) {
            PacketDistributor.sendToPlayer(player, new CrimsonStatePayload(get(player)));
        }
    }

    /** Mirrors {@code fleshAmount} into the max-health modifier (never taking max health below 2). */
    public static void applyFlesh(Player player) {
        AttributeInstance max = player.getAttribute(Attributes.MAX_HEALTH);
        if (max == null) {
            return;
        }
        max.removeModifier(FLESH_ID);
        int flesh = get(player).fleshAmount();
        if (flesh > 0) {
            double amount = Math.min(flesh, Math.max(0.0, max.getBaseValue() - 2.0));
            if (amount > 0) {
                max.addTransientModifier(new AttributeModifier(FLESH_ID, -amount, AttributeModifier.Operation.ADD_VALUE));
            }
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth()); // vanilla clamps on its next attribute sweep; do it now
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.hasData(ATTACHMENT)) {
            applyFlesh(player);
            sync(player);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.hasData(ATTACHMENT)) {
            sync(player);
        }
    }

    /** Original respawn rule (TCEntityEventHandler:380-393), applied to the copied state. */
    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        if (event.isWasDeath() && event.getEntity() instanceof ServerPlayer player && player.hasData(ATTACHMENT)) {
            player.setData(ATTACHMENT, respawnRule(get(player), player.getAttributeBaseValue(Attributes.MAX_HEALTH)));
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.hasData(ATTACHMENT)) {
            applyFlesh(player);
            sync(player);
        }
    }

    /** Ethereal with flesh at or over the health total comes back with 2 health (HP - 2 held back); toggles and protolimbs reset. */
    public static State respawnRule(State s, double baseHealth) {
        int flesh = s.fleshAmount();
        if (s.ethereal() && flesh >= baseHealth) {
            flesh = (int) baseHealth - 2;
        }
        return new State(flesh, false, false, 0, 0);
    }
}
