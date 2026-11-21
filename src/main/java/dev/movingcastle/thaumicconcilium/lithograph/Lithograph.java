package dev.movingcastle.thaumicconcilium.lithograph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.network.LithographSyncPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
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
 * A player's lithograph state (original: {@code TCPlayerCapabilities}), a persistent data attachment that is copied on
 * death and synced to its owner (and only its owner) with {@link LithographSyncPayload}.
 * <ul>
 *   <li>{@code lithographed}: the Lithographer has operated on this player: they see the monitor HUD;</li>
 *   <li>{@code relieved}: Coagulating Eye Drops have switched the HUD off (drinking again switches it back);</li>
 *   <li>{@code monitored}: up to {@value #MAX_MONITORED} target descriptors, in HUD order. Each is a string
 *   {@code "<kind>:<id>"}; today the only kind is {@code player:<uuid>} (the owner of a xylographed wand). Later kinds
 *   (Gadomancy's crystal eye, Red-Powered Mind) just add other prefixes; readers must skip kinds they don't know.</li>
 * </ul>
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class Lithograph {
    public static final int MAX_MONITORED = 4;
    public static final String PLAYER_KIND = "player:";

    public record State(boolean lithographed, boolean relieved, List<String> monitored) {
        public static final State DEFAULT = new State(false, false, List.of());
        public static final Codec<State> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.BOOL.optionalFieldOf("lithographed", false).forGetter(State::lithographed),
                Codec.BOOL.optionalFieldOf("relieved", false).forGetter(State::relieved),
                Codec.STRING.listOf().optionalFieldOf("monitored", List.of()).forGetter(State::monitored)).apply(i, State::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, State::lithographed,
                ByteBufCodecs.BOOL, State::relieved,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(MAX_MONITORED)), State::monitored,
                State::new);

        public State {
            monitored = List.copyOf(monitored.size() > MAX_MONITORED ? monitored.subList(0, MAX_MONITORED) : monitored);
        }

        /** Whether the monitor HUD should draw: lithographed and not relieved. */
        public boolean hudVisible() {
            return this.lithographed && !this.relieved;
        }
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ThaumicConcilium.MODID);
    public static final Supplier<AttachmentType<State>> ATTACHMENT = ATTACHMENTS.register("lithograph",
            () -> AttachmentType.builder(() -> State.DEFAULT).serialize(State.CODEC).copyOnDeath().build());

    private Lithograph() {
    }

    public static State get(Player player) {
        return player.getData(ATTACHMENT);
    }

    /** Replaces the state; on the server this also syncs it to the owner. */
    public static void set(Player player, State state) {
        player.setData(ATTACHMENT, state);
        if (player instanceof ServerPlayer server) {
            sync(server);
        }
    }

    public static void setLithographed(Player player, boolean value) {
        State s = get(player);
        set(player, new State(value, s.relieved(), s.monitored()));
    }

    public static void setRelieved(Player player, boolean value) {
        State s = get(player);
        set(player, new State(s.lithographed(), value, s.monitored()));
    }

    /** Replaces the monitored list (extra entries beyond {@link #MAX_MONITORED} are dropped). */
    public static void setMonitored(Player player, List<String> targets) {
        State s = get(player);
        set(player, new State(s.lithographed(), s.relieved(), new ArrayList<>(targets)));
    }

    public static String playerTarget(UUID id) {
        return PLAYER_KIND + id;
    }

    /** Sends the owner's state to their client. */
    public static void sync(ServerPlayer player) {
        if (!(player instanceof FakePlayer)) {
            PacketDistributor.sendToPlayer(player, new LithographSyncPayload(get(player)));
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }
}
