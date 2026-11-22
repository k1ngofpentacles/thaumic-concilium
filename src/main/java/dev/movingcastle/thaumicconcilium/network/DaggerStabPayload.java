package dev.movingcastle.thaumicconcilium.network;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.item.CrimsonDagger;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server (decision D9-b): the attack key went down while the Crimson Dagger is raised. Vanilla throws away
 * attack clicks while an item is in use, so the client polls the key itself. The server re-checks that the player is
 * raised ({@link CrimsonDagger#stabFromClient}).
 */
public record DaggerStabPayload() implements CustomPacketPayload {
    public static final Type<DaggerStabPayload> TYPE = new Type<>(ThaumicConcilium.id("dagger_stab"));
    public static final StreamCodec<ByteBuf, DaggerStabPayload> STREAM_CODEC = StreamCodec.unit(new DaggerStabPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DaggerStabPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                CrimsonDagger.stabFromClient(player);
            }
        });
    }
}
