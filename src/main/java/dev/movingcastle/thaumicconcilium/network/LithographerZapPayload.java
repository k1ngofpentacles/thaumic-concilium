package dev.movingcastle.thaumicconcilium.network;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.joml.Vector3f;

/** Server to nearby clients: the Lithographer bolt (original: PacketFXLithographerZap, a cyan arc one block long). */
public record LithographerZapPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<LithographerZapPayload> TYPE = new Type<>(ThaumicConcilium.id("lithographer_zap"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LithographerZapPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, LithographerZapPayload::pos, LithographerZapPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LithographerZapPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientSide.zap(payload.pos));
    }

    /** Kept in a nested class so the dedicated server never loads client classes. */
    private static final class ClientSide {
        static void zap(BlockPos pos) {
            var level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            DustParticleOptions cyan = new DustParticleOptions(new Vector3f(0.0F, 0.85F, 1.0F), 1.0F);
            Vec3 from = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            Vec3 to = from.add(0.0, -1.0, 0.0);
            for (int i = 0; i <= 12; i++) {
                Vec3 p = from.lerp(to, i / 12.0);
                level.addParticle(cyan, p.x + (level.random.nextDouble() - 0.5) * 0.25, p.y, p.z + (level.random.nextDouble() - 0.5) * 0.25, 0.0, 0.0, 0.0);
            }
        }
    }
}
