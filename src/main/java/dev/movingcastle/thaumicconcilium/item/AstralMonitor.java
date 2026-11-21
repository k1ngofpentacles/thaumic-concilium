package dev.movingcastle.thaumicconcilium.item;

import dev.movingcastle.thaumicconcilium.menu.AstralMonitorMenu;
import dev.movingcastle.thaumicconcilium.registry.TCDataComponents;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The Astral Monitor (original: {@code AstralMonitor}). Used while sneaking it opens a one-slot screen that takes a
 * xylographed wand; the signed player is the one it watches. Held in the main hand, the server reads their health,
 * mana and runic shielding every 10 ticks ({@link dev.movingcastle.thaumicconcilium.lithograph.PartyUpdates}) and the
 * first-person model shows them as constellation glyphs ({@link dev.movingcastle.thaumicconcilium.client.AstralMonitorRenderer}).
 * The wand lives in the {@code monitor_wand} data component, the latest reading in {@code monitor_stats}.
 */
public class AstralMonitor extends Item {
    public AstralMonitor(Properties properties) {
        super(properties);
    }

    /** The wand inside, or empty. */
    public static ItemStack wand(ItemStack monitor) {
        ItemStack wand = monitor.get(TCDataComponents.MONITOR_WAND.get());
        return wand == null ? ItemStack.EMPTY : wand;
    }

    /** Whether {@code wand} may go in a monitor: a wand signed by a player (original: a non-empty Xylography tag). */
    public static boolean fits(ItemStack wand) {
        return Xylography.signer(wand).isPresent();
    }

    /** The player a monitor watches (the signer of its wand). */
    public static Optional<UUID> target(ItemStack monitor) {
        return Xylography.signer(wand(monitor)).map(Xylography.Signer::id);
    }

    /** Whether a monitor is watching someone, which is what the Lithographer accepts. */
    public static boolean watching(ItemStack stack) {
        return stack.getItem() instanceof AstralMonitor && target(stack).isPresent();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Original: only sneaking, only from the hand that is selected on the hotbar (the screen locks that slot).
        if (!player.isShiftKeyDown() || hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide() && player instanceof ServerPlayer server) {
            server.openMenu(new SimpleMenuProvider((id, inventory, p) -> new AstralMonitorMenu(id, inventory),
                    Component.translatable("container.thaumicconcilium.astral_monitor")), buf -> {
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        Optional<Xylography.Signer> signer = Xylography.signer(wand(stack));
        signer.ifPresent(s -> tooltip.add(Component.translatable("thaumicconcilium.tooltip.monitor").withStyle(ChatFormatting.BLUE)
                .append(Component.literal(" " + s.name()).withStyle(ChatFormatting.RESET))));
    }
}
