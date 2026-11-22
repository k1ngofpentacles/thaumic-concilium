package dev.movingcastle.thaumicconcilium.network;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server to players tracking the matrix's chunk: the Crimson Dagger's binding changed (D1 option C). Sent on open, on a new
 * demand, when it is fed or lapses, on a snap and on close; never per tick. {@code item} is the demanded item's registry id
 * (DEMAND), {@code target} the pedestal that fed the chain (FED).
 */
public record BindingPayload(BlockPos matrix, UUID player, byte state, String item, BlockPos target) implements CustomPacketPayload {
    public static final byte OPEN = 0;
    public static final byte DEMAND = 1;
    public static final byte FED = 2;
    public static final byte MISSED = 3;
    public static final byte SNAP = 4;
    public static final byte CLOSE = 5;
    /** A Yank's telegraph began: the chain glows for 20 ticks. */
    public static final byte YANK = 6;

    public static final Type<BindingPayload> TYPE = new Type<>(ThaumicConcilium.id("binding"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BindingPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, BindingPayload::matrix,
            UUIDUtil.STREAM_CODEC, BindingPayload::player,
            ByteBufCodecs.BYTE, BindingPayload::state,
            ByteBufCodecs.STRING_UTF8, BindingPayload::item,
            BlockPos.STREAM_CODEC, BindingPayload::target,
            BindingPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BindingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> dev.movingcastle.thaumicconcilium.client.BindingClient.accept(payload));
    }
}
