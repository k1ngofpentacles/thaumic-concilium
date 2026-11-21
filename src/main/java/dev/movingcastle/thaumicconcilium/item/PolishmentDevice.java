package dev.movingcastle.thaumicconcilium.item;

import com.leclowndu93150.thaumaturge.content.entity.EntityFluxRift;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Polishment Device (original: creative only, no recipe). Right-click a rift to remove it. Thaumaturge's rifts are
 * not pickable (the crosshair passes through them), so the look ray is tested against their bounding boxes directly.
 */
public class PolishmentDevice extends Item {
    private static final double REACH = 8.0;

    public PolishmentDevice(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(REACH));
        EntityFluxRift best = null;
        double bestDist = Double.MAX_VALUE;
        List<EntityFluxRift> rifts = level.getEntitiesOfClass(EntityFluxRift.class, new AABB(from, to).inflate(1.0), r -> !r.isRemoved());
        for (EntityFluxRift rift : rifts) {
            var hit = rift.getBoundingBox().inflate(0.3).clip(from, to);
            if (hit.isPresent() && from.distanceToSqr(hit.get()) < bestDist) {
                bestDist = from.distanceToSqr(hit.get());
                best = rift;
            }
        }
        if (best == null) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide()) {
            best.discard();
            level.playSound(null, best.getX(), best.getY(), best.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 0.6F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("thaumicconcilium.tooltip.polishment").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("thaumicconcilium.tooltip.creative").withStyle(ChatFormatting.GOLD));
    }
}
