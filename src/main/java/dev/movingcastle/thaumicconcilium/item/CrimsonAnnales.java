package dev.movingcastle.thaumicconcilium.item;

import com.leclowndu93150.thaumaturge.api.capability.PlayerResearchAccess;
import com.leclowndu93150.thaumaturge.content.research.ResearchManager;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Crimson Annales (original: ItemResource meta 3): using it completes the Crimson Annales research for a player who has done the
 * Thaumaturge's Crimson Rites research (original: CRIMSON and TCTAINTEDMAGIC; Tainted Magic is not part of this port); otherwise "the
 * meaning of this book remains unclear". Not consumed.
 */
public class CrimsonAnnales extends Item {
    public static final ResourceLocation RESEARCH = ThaumicConcilium.id("crimson_annales");
    /** Original: research CRIMSON. */
    public static final ResourceLocation PREREQUISITE = ResourceLocation.fromNamespaceAndPath("thaumaturge", "crimson_rites");

    public CrimsonAnnales(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server && !PlayerResearchAccess.of(server).hasCompleted(RESEARCH)) {
            if (PlayerResearchAccess.of(server).hasCompleted(PREREQUISITE) && ResearchManager.complete(server, RESEARCH)) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.LEARN.get(), SoundSource.PLAYERS, 0.75F, 1.0F);
            } else {
                player.sendSystemMessage(Component.translatable("message.thaumicconcilium.annales_unclear").withStyle(ChatFormatting.DARK_PURPLE));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
