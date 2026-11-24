package dev.movingcastle.thaumicconcilium.world;

import com.leclowndu93150.thaumaturge.content.research.ResearchManager;
import com.leclowndu93150.thaumaturge.content.wands.ItemWand;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.registry.TCItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The Protobody chain's rules (original TCEntityEventHandler:272-275, Protolimb:playerTouch, EntityItemProtobody): where the void
 * starts, what absorbing a Protolimb does, and the wand trick that gathers the void into a Solid Void block.
 * <p>Original depths are measured from a floor at y 0 (item below -16, wand below -1); here from the dimension's own floor
 * ({@code minBuildHeight}), so the Nether, the End and a datapack dimension each have their own. Event driven, no tick work.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class Protolimbs {
    /** Original: {@code capabilities.protolimbs == 6}; also the flesh gate {@link Ethereal#PROTOLIMB_LIMIT}. */
    public static final int GOAL = Ethereal.PROTOLIMB_LIMIT;
    /** Original: item entity {@code posY < -16}. */
    public static final int ITEM_DEPTH = 16;
    /** Original: wand trick {@code posY < -1.0}. */
    public static final int WAND_DEPTH = 1;
    /** The research the sixth limb grants (code-granted, no way to learn it otherwise). */
    public static final ResourceLocation SOLID_VOID = ThaumicConcilium.id("solid_void");

    private Protolimbs() {
    }

    /** Y below which a Protobody item entity breaks into limbs in this level. */
    public static double itemThreshold(Level level) {
        return level.getMinBuildHeight() - ITEM_DEPTH;
    }

    /** Y below which a wand right-click gathers a Solid Void in this level. */
    public static double wandThreshold(Level level) {
        return level.getMinBuildHeight() - WAND_DEPTH;
    }

    public static boolean belowItemThreshold(Entity entity) {
        return entity.getY() < itemThreshold(entity.level());
    }

    /**
     * A limb touched the player (server): one more protolimb on the {@link CrimsonState}, the craft-fail sound, and at exactly six
     * the Solid Void research (once, with a heartbeat). Original: Protolimb.func_70100_b_. Returns the new count.
     */
    public static int absorb(ServerPlayer player) {
        CrimsonState.State s = CrimsonState.get(player);
        int n = s.protolimbs() + 1;
        CrimsonState.set(player, s.withProtolimbs(n));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.CRAFTFAIL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        if (n == GOAL && ResearchManager.complete(player, SOLID_VOID)) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.HEARTBEAT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return n;
    }

    /**
     * The wand trick: a wand right-clicked in the air low enough, with six limbs absorbed, puts a Solid Void in the inventory
     * (dropped when it is full). Returns whether it fired.
     */
    public static boolean gather(ServerPlayer player) {
        if (player.getY() >= wandThreshold(player.level()) || CrimsonState.get(player).protolimbs() < GOAL) {
            return false;
        }
        ItemStack block = new ItemStack(TCItems.SOLID_VOID.get());
        if (!player.getInventory().add(block)) {
            player.drop(block, false);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5F, 1.0F);
        return true;
    }

    @SubscribeEvent
    public static void wand(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() == InteractionHand.MAIN_HAND && !event.getLevel().isClientSide() && event.getEntity() instanceof ServerPlayer player
                && event.getItemStack().getItem() instanceof ItemWand) {
            gather(player);
        }
    }

    /** Test and tool support: the count a player holds. */
    public static int count(Player player) {
        return CrimsonState.get(player).protolimbs();
    }
}
