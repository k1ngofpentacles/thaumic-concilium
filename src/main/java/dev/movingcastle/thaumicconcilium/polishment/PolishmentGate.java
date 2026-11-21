package dev.movingcastle.thaumicconcilium.polishment;

import com.leclowndu93150.thaumaturge.api.infusion.InfusionCraftedEvent;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.block.DestabilizedCrystalBlockEntity;
import dev.movingcastle.thaumicconcilium.spell.effect.VisConductorEffect;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * The polished infusion, a mandatory gate as in the original: when an infusion whose result has a {@link Polishment}
 * entry completes, the crafter must be channelling a Vis Conductor bound to a Destabilized Crystal of the entry's aspect
 * that has given up at least the entry's amount. The crystal is then spent (a shock, a swing, a lightning arc to the
 * matrix, and the unreleased charge is dropped). Anything else, or no crafter, and the result is destroyed.
 * <p>
 * Thaumaturge's {@code BlockEntityInfusionMatrix.finishCraft} sets the pedestal to {@code event.getResult()}, so an empty
 * result clears the pedestal (the catalyst is already consumed into the job) and awards nothing: no duplicate, no error.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class PolishmentGate {
    private PolishmentGate() {
    }

    @SubscribeEvent
    public static void onCrafted(InfusionCraftedEvent event) {
        ServerLevel level = event.getLevel();
        Polishment entry = Polishment.find(level.registryAccess(), event.getResult());
        if (entry == null) {
            return;
        }
        ServerPlayer player = event.getPlayer();
        if (player != null && spend(level, player, entry, event.getMatrixPos().getCenter())) {
            return;
        }
        event.setResult(ItemStack.EMPTY);
        if (player != null) {
            player.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("message.thaumicconcilium.polishment_failed").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC)));
        }
    }

    /** True when the player's draw meets the entry; the crystal is then removed and the draw cleared. */
    static boolean spend(ServerLevel level, ServerPlayer player, Polishment entry, Vec3 matrix) {
        VisConductorEffect.Draw draw = VisConductorEffect.drawOf(level, player.getUUID());
        if (draw == null || !draw.aspect().equals(entry.aspect()) || draw.drawn() < entry.amount()
                || !(level.getBlockEntity(draw.pos()) instanceof DestabilizedCrystalBlockEntity crystal)) {
            return false;
        }
        int rgb = crystal.color();
        VisConductorEffect.clearDraw(player.getUUID());
        level.removeBlock(draw.pos(), false);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.SHOCK.get(), SoundSource.PLAYERS, 0.8F,
                level.random.nextFloat() * 0.1F + 0.9F);
        player.swing(InteractionHand.MAIN_HAND, true);
        player.stopUsingItem();
        VisConductorEffect.arc(level, player.getEyePosition().subtract(0.0, 0.4, 0.0), matrix, rgb);
        return true;
    }
}
