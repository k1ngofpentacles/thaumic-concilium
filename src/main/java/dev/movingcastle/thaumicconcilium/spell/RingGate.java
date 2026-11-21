package dev.movingcastle.thaumicconcilium.spell;

import com.leclowndu93150.thaumaturge.api.spell.event.SpellCastEvent;
import com.leclowndu93150.thaumaturge.api.spell.part.SpellPart;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.item.RingOfBlusteringLight;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * "The life force of a mere mortal is insufficient to control such power." Spells containing the Spotlight,
 * Alchemist's Spree or Warp Extraction parts only cast while the caster wears a Ring of Blustering Light. The
 * check runs on {@link SpellCastEvent.Pre}, before any vis is paid, so a refused cast costs nothing; a channel
 * stops at once.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class RingGate {
    public static final Set<ResourceKey<SpellPart>> RING_PARTS = Set.of(
            SpellTrees.part("spotlight"), SpellTrees.part("alchemist_spree"), SpellTrees.part("warp_extraction"));

    private RingGate() {
    }

    @SubscribeEvent
    public static void onCast(SpellCastEvent.Pre event) {
        if (event.caster() instanceof ServerPlayer player
                && SpellTrees.containsAny(event.spell(), RING_PARTS)
                && !RingOfBlusteringLight.isWornBy(player)) {
            event.setCanceled(true);
            player.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("message.thaumicconcilium.no_ring").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC)));
        }
    }
}
