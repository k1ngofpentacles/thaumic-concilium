package dev.movingcastle.thaumicconcilium.item;

import java.util.function.BiFunction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A bottle thrown like a splash potion (the original's -20 degree lift and 0.5 speed). */
public class ThrowableBottle extends Item {
    private final BiFunction<Level, LivingEntity, ThrowableItemProjectile> projectile;

    public ThrowableBottle(Properties properties, BiFunction<Level, LivingEntity, ThrowableItemProjectile> projectile) {
        super(properties);
        this.projectile = projectile;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide()) {
            ThrowableItemProjectile thrown = this.projectile.apply(level, player);
            thrown.setItem(stack.copyWithCount(1));
            thrown.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.5F, 1.0F);
            level.addFreshEntity(thrown);
        }
        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
