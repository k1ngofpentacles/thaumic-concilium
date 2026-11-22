package dev.movingcastle.thaumicconcilium.network;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.world.CrimsonState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to one player: their crimson state changed (robe toggle, ethereal, flesh, chained). Sent on change only. */
public record CrimsonStatePayload(CrimsonState.State state) implements CustomPacketPayload {
    public static final Type<CrimsonStatePayload> TYPE = new Type<>(ThaumicConcilium.id("crimson_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CrimsonStatePayload> STREAM_CODEC =
            CrimsonState.State.STREAM_CODEC.map(CrimsonStatePayload::new, CrimsonStatePayload::state);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CrimsonStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> CrimsonState.set(context.player(), payload.state()));
    }
}
