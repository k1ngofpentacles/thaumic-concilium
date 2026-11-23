package dev.movingcastle.thaumicconcilium.golem;

import com.leclowndu93150.thaumaturge.content.golem.ItemGolemBell;
import com.leclowndu93150.thaumaturge.content.golem.seals.SealEntity;
import com.leclowndu93150.thaumaturge.content.golem.seals.SealHandler;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * What arms a Valet seal (phase 7): the owner's chat message, or a golem bell renamed in an anvil and rung (as if its name were
 * said, silently). Observe only: neither event is cancelled, the message is still sent and the bell still rings. Both
 * handlers leave at once unless there is something to do, and the seal lookup only runs after that.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class ValetTriggers {
    /** The original's hearing range, in blocks. */
    public static final int RANGE = 32;

    private ValetTriggers() {
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        trigger(event.getPlayer(), event.getRawText());
    }

    @SubscribeEvent
    public static void onBell(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!stack.is(TTItems.GOLEM_BELL.get())) {
            return;
        }
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        // sneaking and aiming at a seal are the bell's own gestures (open the seal, remove it, logistics): not a ring
        if (name == null || player.isShiftKeyDown() || ItemGolemBell.getAimedSeal(player) != null) {
            return;
        }
        trigger(player, name.getString());
    }

    /**
     * Arms every Valet seal within {@link #RANGE} blocks of the speaker that they own and whose phrase item is named exactly
     * {@code phrase} (case and spaces count). Returns how many armed.
     */
    public static int trigger(ServerPlayer player, String phrase) {
        if (phrase == null || phrase.isEmpty()) {
            return 0;
        }
        ServerLevel level = player.serverLevel();
        int armed = 0;
        for (SealEntity seal : SealHandler.getSealsInRange(level, player.blockPosition(), RANGE)) {
            if (seal.behavior() instanceof ValetBehavior valet && player.getUUID().equals(seal.owner()) && phrase.equals(ValetBehavior.phrase(seal))) {
                valet.arm(level);
                armed++;
            }
        }
        return armed;
    }
}
