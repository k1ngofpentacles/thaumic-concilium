package dev.movingcastle.thaumicconcilium.network;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server to every client tracking a Crimson Dagger user (and the user): the dagger's animation state changed.
 * {@link #RAISE} use started, {@link #END} use released, {@link #STAB}, {@link #CUT} (the self-cut). {@code mainHand}: the
 * dagger is in the main hand (so that arm is the holding arm). The clock lives in {@code client/DaggerAnim}.
 */
public record DaggerFxPayload(int entityId, byte mode, boolean mainHand) implements CustomPacketPayload {
    public static final byte RAISE = 0;
    public static final byte END = 1;
    public static final byte STAB = 2;
    public static final byte CUT = 3;
    public static final Type<DaggerFxPayload> TYPE = new Type<>(ThaumicConcilium.id("dagger_fx"));
    public static final StreamCodec<ByteBuf, DaggerFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DaggerFxPayload::entityId,
            ByteBufCodecs.BYTE, DaggerFxPayload::mode,
            ByteBufCodecs.BOOL, DaggerFxPayload::mainHand,
            DaggerFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DaggerFxPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> dev.movingcastle.thaumicconcilium.client.DaggerAnim.receive(payload));
    }
}
