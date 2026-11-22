package dev.movingcastle.thaumicconcilium.network;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.world.PontifexToggle;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: the toggle key was pressed (original: PacketTogglePontifexRobe). The server checks the full set itself. */
public record PontifexTogglePayload() implements CustomPacketPayload {
    public static final Type<PontifexTogglePayload> TYPE = new Type<>(ThaumicConcilium.id("pontifex_toggle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PontifexTogglePayload> STREAM_CODEC = StreamCodec.unit(new PontifexTogglePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PontifexTogglePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                PontifexToggle.toggle(player);
            }
        });
    }
}
