package dev.movingcastle.thaumicconcilium.network;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.spell.effect.EtherealManipulatorEffect;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Mouse-wheel steps while channelling the Ethereal Manipulator, moving the held object nearer or further. */
public record EtherealRangePayload(int steps) implements CustomPacketPayload {
    public static final Type<EtherealRangePayload> TYPE = new Type<>(ThaumicConcilium.id("ethereal_range"));
    public static final StreamCodec<ByteBuf, EtherealRangePayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(EtherealRangePayload::new, EtherealRangePayload::steps);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EtherealRangePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.isUsingItem()) {
                EtherealManipulatorEffect.adjustRange(player, Math.max(-8, Math.min(8, payload.steps())));
            }
        });
    }
}
