package dev.movingcastle.thaumicconcilium.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

/**
 * The Pontifex's Hammer (original: PontifexHammer, a sword of tool material PHAMMER: harvest 4, 1000 uses, speed 8, damage +14,
 * enchantability 20; epic; warp 3). It mends itself one point a second. Holding the use button drains the life of every living thing
 * within 6 blocks (2 up and down) once a second: each takes the wielder's attack damage as magic damage, the wielder is healed 2 and the
 * hammer loses 2.
 */
public class PontifexHammer extends SwordItem {
    public static final Tier TIER = new Tier() {
        @Override public int getUses() { return 1000; }
        @Override public float getSpeed() { return 8.0F; }
        @Override public float getAttackDamageBonus() { return 14.0F; }
        @Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_NETHERITE_TOOL; }
        @Override public int getEnchantmentValue() { return 20; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
    };
    public static final int DRAIN_INTERVAL = 20;
    public static final double RANGE_XZ = 6.0;
    public static final double RANGE_Y = 2.0;
    public static final float HEAL = 2.0F;
    public static final int DRAIN_WEAR = 2;

    public PontifexHammer(Properties properties) {
        super(TIER, properties.attributes(SwordItem.createAttributes(TIER, 3, -2.4F)));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    /** Original inventory tick: a damaged hammer mends one point every second. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (stack.isDamaged() && !level.isClientSide() && entity.tickCount % 20 == 0) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        int used = getUseDuration(stack, user) - remaining;
        if (!(user instanceof Player player) || used % DRAIN_INTERVAL != 0 || used == 0) {
            return;
        }
        if (level instanceof ServerLevel server) {
            drain(server, player, stack);
        } else {
            for (LivingEntity e : victims(level, player)) {
                dev.movingcastle.thaumicconcilium.client.LifeDrainFx.spawn(level, e, player);
            }
        }
    }

    public static List<LivingEntity> victims(Level level, LivingEntity user) {
        AABB box = user.getBoundingBox().inflate(RANGE_XZ, RANGE_Y, RANGE_XZ);
        return level.getEntitiesOfClass(LivingEntity.class, box, e -> e != user && e.isAlive());
    }

    /** One drain pulse; returns how many things were hurt. */
    public static int drain(ServerLevel level, Player player, ItemStack stack) {
        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        int hurt = 0;
        for (LivingEntity e : victims(level, player)) {
            DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DamageTypes.MAGIC), player);
            if (e.hurt(source, damage)) {
                stack.hurtAndBreak(DRAIN_WEAR, player, EquipmentSlot.MAINHAND);
                player.heal(HEAL);
                hurt++;
            }
        }
        return hurt;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.thaumicconcilium.pontifex_hammer.desc").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
