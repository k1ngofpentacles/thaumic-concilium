package dev.movingcastle.thaumicconcilium.client;

import dev.movingcastle.thaumicconcilium.network.BindingPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Client copy of the bindings in view (D1 option C): who is chained to which matrix, what the chain demands, the last feeding. */
@EventBusSubscriber(modid = dev.movingcastle.thaumicconcilium.ThaumicConcilium.MODID, value = Dist.CLIENT)
public final class BindingClient {
    /** How long (ticks) a feeding flick or a lapsed bite stays on the chain. */
    static final int FLICK_TICKS = 10;

    /** One binding as the client sees it. */
    static final class State {
        UUID player;
        ItemStack demand = ItemStack.EMPTY;
        BlockPos flickTarget = BlockPos.ZERO;
        long flickUntil;
        long missUntil;
        /** Game time until which the chain glows (a Yank's telegraph). */
        long yankUntil;
    }

    static final Map<BlockPos, State> STATES = new HashMap<>();

    private BindingClient() {
    }

    public static void accept(BindingPayload p) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0L : mc.level.getGameTime();
        if (p.state() == BindingPayload.SNAP || p.state() == BindingPayload.CLOSE) {
            STATES.remove(p.matrix());
            return;
        }
        State s = STATES.computeIfAbsent(p.matrix(), k -> new State());
        s.player = p.player();
        switch (p.state()) {
            case BindingPayload.DEMAND -> s.demand = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(p.item())));
            case BindingPayload.FED -> {
                s.demand = ItemStack.EMPTY;
                s.flickTarget = p.target();
                s.flickUntil = now + FLICK_TICKS;
            }
            case BindingPayload.MISSED -> {
                s.demand = ItemStack.EMPTY;
                s.missUntil = now + FLICK_TICKS;
            }
            case BindingPayload.YANK -> s.yankUntil = now + 20;
            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        STATES.clear();
    }
}
