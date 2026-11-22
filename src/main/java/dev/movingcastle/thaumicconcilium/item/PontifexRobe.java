package dev.movingcastle.thaumicconcilium.item;

import com.leclowndu93150.thaumaturge.api.items.IVisDiscountGear;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * One piece of the Pontifex's robe (original: PontifexRobe). Runic charge 5 and warp 3 come from the item's default components,
 * the helm also reveals nodes (goggles component), 5% vis discount each. Dyeable (tag minecraft:dyeable, DYED_COLOR, default 0xA01818).
 * The full set is what the toggle key and the cultists' respect need ({@link #isFullSet}).
 */
public class PontifexRobe extends ArmorItem implements IVisDiscountGear {
    public static final int VIS_DISCOUNT = 5;
    public static final int DEFAULT_COLOR = 0xA01818;   // original 10491928

    public PontifexRobe(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public int getVisDiscount(ItemStack stack) {
        return VIS_DISCOUNT;
    }

    /** Original PontifexRobe.isFullSet: all four armour slots hold a robe piece. */
    public static boolean isFullSet(LivingEntity entity) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!(entity.getItemBySlot(slot).getItem() instanceof PontifexRobe)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.thaumicconcilium.pontifex_robe.desc").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
