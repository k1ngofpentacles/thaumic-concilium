package dev.movingcastle.thaumicconcilium.item;

import com.leclowndu93150.thaumaturge.api.wands.IWandRodOnUpdate;
import com.leclowndu93150.thaumaturge.content.wands.ItemWand;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import dev.movingcastle.thaumicconcilium.spell.BraceletCasting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * A casting bracelet: a small wand worn in the Curios bracelet slot, so it casts without taking a hand or a hotbar
 * slot. It is a Thaumaturge wand underneath ({@link ItemWand}, with fixed {@code thaumaturge:wand_parts}), so vis,
 * recharge pedestals, tooltips and focus swapping all work as for a wand; casting goes through
 * {@link dev.movingcastle.thaumicconcilium.spell.BraceletCasting} from the cast key instead of right-click.
 * <p>
 * Worn (Curios bracelet slot) or in the shield slot it casts from the cast key; held in either hand it is a wand like
 * any other (right-click casting, node tapping, the Arcane Workbench's wand slot, infusion and other wand rituals). As
 * in the original, every cast costs two thirds of a wand's and cools down twice as fast; the terrasteel bracelet
 * instead casts instant spells in a volley of ten at triple cooldown.
 */
public class CastingBracelet extends ItemWand implements ICurioItem {
    private final BraceletKind kind;

    public CastingBracelet(Properties properties, BraceletKind kind) {
        super(properties);
        this.kind = kind;
    }

    public BraceletKind kind() {
        return this.kind;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId(stack));
    }

    /** In either hand it is a wand (Thaumaturge's use: casting, node tapping, wand-driven blocks), at bracelet prices. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // A drum in the off hand takes the click: pass it on so the game tries the off hand (the drum plays).
        if (hand == InteractionHand.MAIN_HAND && player.getOffhandItem().getItem() instanceof ThaumaturgeDrum) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        if (player instanceof ServerPlayer server) {
            return BraceletCasting.handUse(server, hand, player.getItemInHand(hand), () -> super.use(level, player, hand));
        }
        return super.use(level, player, hand);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        BraceletCasting.handTick(stack, () -> super.onUseTick(level, entity, stack, remaining));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (entity instanceof ServerPlayer server) {
            BraceletCasting.handRelease(server, stack, server.getUsedItemHand() == InteractionHand.MAIN_HAND,
                    () -> super.releaseUsing(stack, level, entity, remaining));
        } else {
            super.releaseUsing(stack, level, entity, remaining);
        }
    }

    /** The arm pose comes from the cast itself (BraceletArmPose), not the wand's bow draw. */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    /** Right-click is for casting; equip it through the Curios screen. */
    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return false;
    }

    /** The rod's self-recharge, which {@link ItemWand#inventoryTick} only runs for the main inventory. */
    @Override
    public void curioTick(SlotContext slot, ItemStack stack) {
        if (slot.entity() instanceof ServerPlayer player) {
            IWandRodOnUpdate onUpdate = this.getParts(stack).rod().onUpdate();
            if (onUpdate != null) {
                onUpdate.onUpdate(stack, player);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.thaumicconcilium.casting_bracelet.desc").withStyle(ChatFormatting.DARK_PURPLE));
        if (this.kind.volley()) {
            tooltip.add(Component.translatable("item.thaumicconcilium.casting_bracelet.volley").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
