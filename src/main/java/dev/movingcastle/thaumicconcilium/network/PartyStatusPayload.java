package dev.movingcastle.thaumicconcilium.network;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.lithograph.PartyStats;
import java.util.Collections;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to a lithographed client: the stats of the four watched players, in HUD order (original: PacketUpdatePartyStatus). */
public record PartyStatusPayload(List<PartyStats> party) implements CustomPacketPayload {
    public static final Type<PartyStatusPayload> TYPE = new Type<>(ThaumicConcilium.id("party_status"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PartyStatusPayload> STREAM_CODEC =
            PartyStats.STREAM_CODEC.apply(ByteBufCodecs.list(4)).map(PartyStatusPayload::new, PartyStatusPayload::party);
    /** What the HUD draws; written on the client thread, read by the render thread. */
    public static volatile List<PartyStats> current = Collections.emptyList();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PartyStatusPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> current = List.copyOf(payload.party));
    }
}
