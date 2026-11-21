package dev.movingcastle.thaumicconcilium.network;

import com.leclowndu93150.thaumaturge.content.casters.CasterManager;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.spell.BraceletCasting;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Socket a focus into the worn bracelet, chosen in the bracelet radial. The key is Thaumaturge's focus sort key, or
 * {@link CasterManager#REMOVE_FOCUS}; Thaumaturge's own {@code changeFocus} does the swap with the inventory and pouches.
 */
public record BraceletFocusPayload(String key) implements CustomPacketPayload {
    public static final Type<BraceletFocusPayload> TYPE = new Type<>(ThaumicConcilium.id("bracelet_focus"));
    public static final StreamCodec<ByteBuf, BraceletFocusPayload> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(BraceletFocusPayload::new, BraceletFocusPayload::key);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BraceletFocusPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ItemStack bracelet = BraceletCasting.worn(player);
                if (!bracelet.isEmpty()) {
                    BraceletCasting.cancel(player);
                    CasterManager.changeFocus(bracelet, player.level(), player, payload.key());
                }
            }
        });
    }
}
