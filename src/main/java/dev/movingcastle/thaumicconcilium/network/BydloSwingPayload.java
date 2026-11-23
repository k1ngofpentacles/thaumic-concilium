package dev.movingcastle.thaumicconcilium.network;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.golem.BydloCommands;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** A rider of a Golem Bydlo clicked at nothing (the client-only {@code LeftClickEmpty}): the wrath command. */
public record BydloSwingPayload() implements CustomPacketPayload {
    public static final Type<BydloSwingPayload> TYPE = new Type<>(ThaumicConcilium.id("bydlo_swing"));
    public static final StreamCodec<ByteBuf, BydloSwingPayload> STREAM_CODEC = StreamCodec.unit(new BydloSwingPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BydloSwingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                BydloCommands.onSwingAtAir(player);
            }
        });
    }
}
