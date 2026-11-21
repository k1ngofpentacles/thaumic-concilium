package dev.movingcastle.thaumicconcilium.item;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Ring of Blustering Light: a curio ring that carries a large runic shield (Thaumaturge reads the
 * {@code thaumaturge:runic_charge} component from worn curios) and trickles primal vis into a wand on the hotbar,
 * one vis of a random primal every three ticks, as in the original.
 * <p>
 * The Spotlight, Alchemist's Spree and Warp Extraction spell parts refuse to cast unless one is worn
 * (see {@link dev.movingcastle.thaumicconcilium.spell.RingGate}).
 */
public class RingOfBlusteringLight extends Item implements ICurioItem {
    public RingOfBlusteringLight(Properties properties) {
        super(properties);
    }

    public static boolean isWornBy(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .map(inv -> inv.isEquipped(stack -> stack.getItem() instanceof RingOfBlusteringLight))
                .orElse(false);
    }

    @Override
    public void curioTick(SlotContext slot, ItemStack stack) {
        if (slot.entity() instanceof Player player && !player.level().isClientSide() && player.tickCount % 3 == 0) {
            ResourceKey<IAspect> primal = TTAspects.PRIMALS.get(player.getRandom().nextInt(TTAspects.PRIMALS.size()));
            ItemStack wand = WandVisHelper.findWandInHotbarWithRoom(player, primal, 1);
            if (wand != null && !wand.isEmpty()) {
                WandVisHelper.addVis(wand, primal, 1, true);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.thaumicconcilium.ring_of_blustering_light.desc").withStyle(ChatFormatting.GOLD));
    }
}
