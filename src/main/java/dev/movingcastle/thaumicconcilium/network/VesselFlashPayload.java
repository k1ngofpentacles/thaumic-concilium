package dev.movingcastle.thaumicconcilium.network;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server to each player near a dying Vessel (phase 5d cycle 4e), once, at the collapse: the flash. {@code time} is the game time of the
 * collapse, {@code (x, y, z)} the hole, {@code radius} the swell's, and {@code inside} whether this player stood inside it (the white-out
 * does not touch them: they see again first; everyone else gets the flashbang). The animation agent draws the flash from this.
 */
public record VesselFlashPayload(long time, double x, double y, double z, float radius, boolean inside) implements CustomPacketPayload {
    public static final Type<VesselFlashPayload> TYPE = new Type<>(ThaumicConcilium.id("vessel_flash"));
    public static final StreamCodec<ByteBuf, VesselFlashPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, VesselFlashPayload::time,
            ByteBufCodecs.DOUBLE, VesselFlashPayload::x,
            ByteBufCodecs.DOUBLE, VesselFlashPayload::y,
            ByteBufCodecs.DOUBLE, VesselFlashPayload::z,
            ByteBufCodecs.FLOAT, VesselFlashPayload::radius,
            ByteBufCodecs.BOOL, VesselFlashPayload::inside,
            VesselFlashPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(VesselFlashPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> dev.movingcastle.thaumicconcilium.client.VesselFlashClient.receive(payload));
    }
}
