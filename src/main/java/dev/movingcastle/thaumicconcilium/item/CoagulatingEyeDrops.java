package dev.movingcastle.thaumicconcilium.item;

import dev.movingcastle.thaumicconcilium.lithograph.Lithograph;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Coagulating Eye Drops (original: {@code CoagulatingEyeDrops}): a 60 tick drinking animation that, on a lithographed
 * player, toggles the monitor HUD off and on ("relieved"), costing one durability. Anyone else just wastes the time.
 */
public class CoagulatingEyeDrops extends Item {
    public CoagulatingEyeDrops(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 60;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player && Lithograph.get(player).lithographed()) {
            Lithograph.setRelieved(player, !Lithograph.get(player).relieved());
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS,
                    1.0F, 1.0F + level.random.nextFloat() * 0.1F);
        }
        return stack;
    }
}
