package dev.movingcastle.thaumicconcilium.network;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.world.PontifexToggle;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to one player: the robe toggle is now on or off (the client phases through blocks while on). */
public record PontifexStatePayload(boolean on) implements CustomPacketPayload {
    public static final Type<PontifexStatePayload> TYPE = new Type<>(ThaumicConcilium.id("pontifex_state"));
    public static final StreamCodec<ByteBuf, PontifexStatePayload> STREAM_CODEC = ByteBufCodecs.BOOL.map(PontifexStatePayload::new, PontifexStatePayload::on);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PontifexStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            PontifexToggle.clientOn = payload.on();
            if (!payload.on()) {
                context.player().noPhysics = false;
            }
        });
    }
}
