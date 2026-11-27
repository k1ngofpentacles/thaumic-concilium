package dev.movingcastle.thaumicconcilium.compat.witchery;

import com.mojang.serialization.Codec;
import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.sterner.witchery.core.api.event.ImpEvents;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The imp pact (original: {@code impDealTime}, 72000 ticks after a deal with a Witchery imp, during which Chorts and Sloppy
 * Alchemists do not pick the player as a target). Right-clicking an imp ({@code ImpEvents.Interact}) starts it; we only read
 * the event and never cancel it, so the soul trading add-on still opens. Copied on death, as the original's capability was.
 * Only loaded with {@code witchery}.
 */
public final class ImpPact {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ThaumicConcilium.MODID);
    /** Ticks left; the data is absent while no pact runs. */
    public static final Supplier<AttachmentType<Integer>> TICKS = ATTACHMENTS.register("imp_pact_ticks",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().build());

    private ImpPact() {
    }

    static void registerListeners() {
        // receiveCanceled: the soul trading add-on cancels the event to open its screen; the pact starts all the same.
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, true, ImpEvents.Interact.class,
                e -> start(e.getPlayer()));
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, e -> tick(e.getEntity()));
    }

    /** Starts (or restarts) the pact for the config's time. Server side only. */
    public static void start(Player player) {
        if (player instanceof ServerPlayer) {
            int ticks = TCConfig.impPactTime();
            if (ticks > 0) {
                player.setData(TICKS.get(), ticks);
            }
        }
    }

    public static boolean has(Player player) {
        return player.hasData(TICKS.get()) && player.getData(TICKS.get()) > 0;
    }

    private static void tick(Player player) {
        if (player.level().isClientSide || !player.hasData(TICKS.get())) {
            return;
        }
        int left = player.getData(TICKS.get()) - 1;
        if (left > 0) {
            player.setData(TICKS.get(), left);
        } else {
            player.removeData(TICKS.get());
        }
    }
}
