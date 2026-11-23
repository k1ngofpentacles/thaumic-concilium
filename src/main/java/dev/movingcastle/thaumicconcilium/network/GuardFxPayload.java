package dev.movingcastle.thaumicconcilium.network;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server to every client tracking an entity (and the entity): the shield foci's state, for the look only
 * (docs/design/shield-foci-look.md section 2). Sent on events, never per tick.
 * <ul>
 *   <li>{@link #ARMED}: a Riposte waits in the shield, {@code charges} of them, until game time {@code until}.</li>
 *   <li>{@link #RELEASED}: one was released toward the unit direction {@code (dx, dy, dz)}; {@code charges} are left until {@code until}.</li>
 *   <li>{@link #DISARMED}: the arming is gone (death, or run out and noticed).</li>
 *   <li>{@link #AEGIS_ON}: the Aegis effect is on until game time {@code until}; {@link #AEGIS_OFF}: it was removed early.</li>
 *   <li>{@link #AEGIS_BLOCK}: the Aegis forced a block of a hit from the unit direction {@code (dx, dy, dz)}.</li>
 * </ul>
 */
public record GuardFxPayload(int entityId, byte kind, int charges, long until, float dx, float dy, float dz) implements CustomPacketPayload {
    public static final byte ARMED = 0;
    public static final byte RELEASED = 1;
    public static final byte DISARMED = 2;
    public static final byte AEGIS_ON = 3;
    public static final byte AEGIS_OFF = 4;
    public static final byte AEGIS_BLOCK = 5;
    public static final Type<GuardFxPayload> TYPE = new Type<>(ThaumicConcilium.id("guard_fx"));
    public static final StreamCodec<ByteBuf, GuardFxPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.VAR_INT.encode(buf, p.entityId);
                buf.writeByte(p.kind);
                ByteBufCodecs.VAR_INT.encode(buf, p.charges);
                buf.writeLong(p.until);
                buf.writeFloat(p.dx);
                buf.writeFloat(p.dy);
                buf.writeFloat(p.dz);
            },
            buf -> new GuardFxPayload(ByteBufCodecs.VAR_INT.decode(buf), buf.readByte(), ByteBufCodecs.VAR_INT.decode(buf),
                    buf.readLong(), buf.readFloat(), buf.readFloat(), buf.readFloat()));

    public static GuardFxPayload of(int entityId, byte kind, int charges, long until) {
        return new GuardFxPayload(entityId, kind, charges, until, 0.0F, 0.0F, 0.0F);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GuardFxPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> dev.movingcastle.thaumicconcilium.client.GuardFx.receive(payload));
    }
}
