package dev.movingcastle.thaumicconcilium.network;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to nearby clients: a Quicksilver Reactor just merged aspects in the crucible at {@code to}; draw the flow. */
public record QuicksilverFlowPayload(BlockPos crucible, BlockPos reactor) implements CustomPacketPayload {
    public static final Type<QuicksilverFlowPayload> TYPE = new Type<>(ThaumicConcilium.id("quicksilver_flow"));
    public static final StreamCodec<FriendlyByteBuf, QuicksilverFlowPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, QuicksilverFlowPayload::crucible,
            BlockPos.STREAM_CODEC, QuicksilverFlowPayload::reactor,
            QuicksilverFlowPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuicksilverFlowPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> dev.movingcastle.thaumicconcilium.client.QuicksilverFlowFx.spawn(payload));
    }
}
