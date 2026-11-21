package dev.movingcastle.thaumicconcilium.item;

import com.leclowndu93150.thaumaturge.api.items.IVisDiscountGear;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Runic Body and Legs Windings. Each carries a runic charge of 200 (the {@code thaumaturge:runic_charge} component,
 * which Thaumaturge sums over armour and curios), refills the shield by 5 every second on top of Thaumaturge's own
 * slow recharge, and gives a 3% vis discount, as in the original.
 */
public class RunicWindings extends ArmorItem implements IVisDiscountGear {
    public static final int VIS_DISCOUNT = 3;
    private static final int REFILL = 5;
    private static final int INTERVAL = 20;

    public RunicWindings(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public int getVisDiscount(ItemStack stack) {
        return VIS_DISCOUNT;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide() || !(entity instanceof Player player) || player.tickCount % INTERVAL != 0
                || player.getItemBySlot(this.getEquipmentSlot()) != stack) {
            return;
        }
        float max = player.getMaxAbsorption();
        float now = player.getAbsorptionAmount();
        if (now < max) {
            player.setAbsorptionAmount(Math.min(max, now + REFILL));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.thaumicconcilium.runic_windings.desc").withStyle(ChatFormatting.GOLD));
    }
}
