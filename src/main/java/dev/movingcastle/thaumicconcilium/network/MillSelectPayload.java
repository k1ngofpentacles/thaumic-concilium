package dev.movingcastle.thaumicconcilium.network;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.item.ShardMill;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Sneak + mouse wheel while holding a Shard Mill: step its selected shard kind (the original's radial menu). */
public record MillSelectPayload(int steps) implements CustomPacketPayload {
    public static final Type<MillSelectPayload> TYPE = new Type<>(ThaumicConcilium.id("mill_select"));
    public static final StreamCodec<ByteBuf, MillSelectPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(MillSelectPayload::new, MillSelectPayload::steps);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MillSelectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && !player.isUsingItem()) {
                ItemStack held = player.getMainHandItem();
                if (held.getItem() instanceof ShardMill mill) {
                    mill.cycle(player, held, Integer.signum(payload.steps()));
                }
            }
        });
    }
}
