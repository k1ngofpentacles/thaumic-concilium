package dev.movingcastle.thaumicconcilium.item;

import dev.movingcastle.thaumicconcilium.entity.PrimalArrow;
import dev.movingcastle.thaumicconcilium.entity.PrimalType;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * One of the six primal arrows. An {@link ArrowItem}, so every vanilla bow and crossbow fires it as {@link PrimalArrow}; it is in
 * {@code minecraft:arrows}. Like any non-vanilla arrow it is not made free by Infinity ({@link #isInfinite} stays false).
 */
public class PrimalArrowItem extends ArrowItem {
    private final PrimalType kind;

    public PrimalArrowItem(PrimalType kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public PrimalType kind() {
        return kind;
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, @Nullable ItemStack weapon) {
        return new PrimalArrow(level, shooter, ammo.copyWithCount(1), weapon);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(net.minecraft.network.chat.Component.translatable("thaumicconcilium.tooltip.primal_arrow." + kind.id())
                .withStyle(net.minecraft.ChatFormatting.BLUE));
    }

    @Override
    public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        PrimalArrow arrow = new PrimalArrow(level, pos.x(), pos.y(), pos.z(), stack.copyWithCount(1));
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        return arrow;
    }
}
