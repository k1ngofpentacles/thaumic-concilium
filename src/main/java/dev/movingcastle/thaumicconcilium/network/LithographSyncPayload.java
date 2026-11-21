package dev.movingcastle.thaumicconcilium.network;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.lithograph.Lithograph;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to the owning client: the player's {@link Lithograph.State}. */
public record LithographSyncPayload(Lithograph.State state) implements CustomPacketPayload {
    public static final Type<LithographSyncPayload> TYPE = new Type<>(ThaumicConcilium.id("lithograph_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LithographSyncPayload> STREAM_CODEC =
            Lithograph.State.STREAM_CODEC.map(LithographSyncPayload::new, LithographSyncPayload::state);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LithographSyncPayload payload, IPayloadContext context) {
        // IPayloadContext.player() is the local player on the client; setting the attachment there needs no further sync.
        context.enqueueWork(() -> context.player().setData(Lithograph.ATTACHMENT, payload.state()));
    }
}
