package dev.movingcastle.thaumicconcilium.network;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to one player: the enslave flag changed (original: PacketEnslave). The client walks itself while the flag is set. */
public record EnslavePayload(boolean enslaved) implements CustomPacketPayload {
    public static final Type<EnslavePayload> TYPE = new Type<>(ThaumicConcilium.id("enslave"));
    public static final StreamCodec<ByteBuf, EnslavePayload> STREAM_CODEC = ByteBufCodecs.BOOL.map(EnslavePayload::new, EnslavePayload::enslaved);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EnslavePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> dev.movingcastle.thaumicconcilium.client.EnslaveClient.set(payload.enslaved()));
    }
}
