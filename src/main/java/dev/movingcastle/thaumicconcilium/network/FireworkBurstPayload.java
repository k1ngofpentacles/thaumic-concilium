package dev.movingcastle.thaumicconcilium.network;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Tells clients to play a firework burst at a point, without spawning a rocket entity.
 * Replaces the original's entity status 17 + {@code World.makeFireworks}.
 */
public record FireworkBurstPayload(Vec3 position, Vec3 motion, List<FireworkExplosion> explosions) implements CustomPacketPayload {
    public static final Type<FireworkBurstPayload> TYPE = new Type<>(ThaumicConcilium.id("firework_burst"));

    private static final StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Vec3::x, ByteBufCodecs.DOUBLE, Vec3::y, ByteBufCodecs.DOUBLE, Vec3::z, Vec3::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, FireworkBurstPayload> STREAM_CODEC = StreamCodec.composite(
            VEC3, FireworkBurstPayload::position,
            VEC3, FireworkBurstPayload::motion,
            FireworkExplosion.STREAM_CODEC.apply(ByteBufCodecs.list(16)), FireworkBurstPayload::explosions,
            FireworkBurstPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FireworkBurstPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientSide.burst(payload));
    }

    /** Kept in a nested class so the dedicated server never loads client classes. */
    private static final class ClientSide {
        static void burst(FireworkBurstPayload p) {
            var level = Minecraft.getInstance().level;
            if (level != null) {
                level.createFireworks(p.position.x, p.position.y, p.position.z, p.motion.x, p.motion.y, p.motion.z, p.explosions);
            }
        }
    }
}
