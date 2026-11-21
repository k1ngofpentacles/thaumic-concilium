package dev.movingcastle.thaumicconcilium.network;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server to every client tracking the caster (and the caster): a bracelet cast happened, so raise that player's casting
 * arm and spray particles in the spell's colour. {@link #BURST}: one cast (the arm goes up, then down);
 * {@link #HOLD} / {@link #RELEASE}: a channelled or charged spell starts and stops. {@code mainHand}: the cast came from
 * a bracelet held in the main hand (that arm rises); otherwise the off arm (worn, or in the shield slot).
 */
public record BraceletFxPayload(int entityId, byte mode, int color, boolean mainHand) implements CustomPacketPayload {
    public static final byte BURST = 0;
    public static final byte HOLD = 1;
    public static final byte RELEASE = 2;
    public static final Type<BraceletFxPayload> TYPE = new Type<>(ThaumicConcilium.id("bracelet_fx"));
    public static final StreamCodec<ByteBuf, BraceletFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BraceletFxPayload::entityId,
            ByteBufCodecs.BYTE, BraceletFxPayload::mode,
            ByteBufCodecs.INT, BraceletFxPayload::color,
            ByteBufCodecs.BOOL, BraceletFxPayload::mainHand,
            BraceletFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BraceletFxPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> dev.movingcastle.thaumicconcilium.client.BraceletCastState.receive(payload));
    }
}
