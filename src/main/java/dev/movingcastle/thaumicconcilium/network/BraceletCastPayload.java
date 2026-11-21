package dev.movingcastle.thaumicconcilium.network;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.spell.BraceletCasting;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The bracelet cast key went down or up. */
public record BraceletCastPayload(boolean down) implements CustomPacketPayload {
    public static final Type<BraceletCastPayload> TYPE = new Type<>(ThaumicConcilium.id("bracelet_cast"));
    public static final StreamCodec<ByteBuf, BraceletCastPayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(BraceletCastPayload::new, BraceletCastPayload::down);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BraceletCastPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (payload.down()) {
                    BraceletCasting.press(player);
                } else {
                    BraceletCasting.release(player);
                }
            }
        });
    }
}
