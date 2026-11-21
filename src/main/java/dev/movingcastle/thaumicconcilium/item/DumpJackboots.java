package dev.movingcastle.thaumicconcilium.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Dump Jackboots: while sneaking with a block in the main hand, the boots lift you straight up and lay that block
 * beneath your feet each time they clear a block space: a solid tower of six blocks a second, faster than jumping and
 * placing by hand.
 * <p>
 * The original bounced you up and placed a block every other tick, which in play read as a derpy hop. Here the lift is
 * a steady climb: the client drives it (players own their movement), the server places the blocks (one per block risen,
 * spent from the stack outside creative). The lift stops when there is no headroom, no block left, or the block
 * couldn't stand there.
 * <p>
 * {@link #RISE} is kept low enough that letting go never leaves a gap: released at full speed you coast about 0.7 of a
 * block higher, so stopping and restarting the lift (to save blocks on momentum) can't skip a block. At 0.5 a tick the
 * coast was 1.7 blocks, which made broken pillars of floating segments the efficient way to climb.
 */
public class DumpJackboots extends ArmorItem {
    /** Blocks risen per tick: 0.3 is six blocks a second, and a coast of 0.7 blocks after release. */
    private static final double RISE = 0.3;

    public DumpJackboots(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(entity instanceof Player player) || player.getItemBySlot(EquipmentSlot.FEET) != stack || !player.isShiftKeyDown()) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof BlockItem blockItem) || held.isEmpty()) {
            return;
        }
        BlockState state = blockItem.getBlock().defaultBlockState();
        BlockPos feet = player.blockPosition();

        // Lay the block in the space the feet have just cleared.
        BlockPos below = feet.below();
        if (!level.isClientSide() && level.getBlockState(below).canBeReplaced() && state.canSurvive(level, below)
                && level.isUnobstructed(state, below, CollisionContext.empty()) && level.setBlock(below, state, 3)) {
            SoundType sound = state.getSoundType(level, below, player);
            level.playSound(null, below, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F,
                    sound.getPitch() * 0.8F);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
        }

        // Climb while there is headroom and something to stand on next.
        BlockPos overhead = feet.above(2);
        boolean headroom = level.getBlockState(overhead).getCollisionShape(level, overhead).isEmpty();
        boolean canBuild = level.getBlockState(feet).canBeReplaced() && state.canSurvive(level, feet);
        if (headroom && canBuild && !held.isEmpty()) {
            player.setDeltaMovement(new Vec3(0.0, RISE, 0.0));
            player.resetFallDistance();
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.thaumicconcilium.dump_jackboots.desc").withStyle(ChatFormatting.GOLD));
    }
}
