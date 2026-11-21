package dev.movingcastle.thaumicconcilium.item;

import com.leclowndu93150.thaumaturge.content.entity.EntityMindSpider;
import com.leclowndu93150.thaumaturge.content.entity.WispEntity;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Thaumaturge's Drum and the Warped Drum (original: {@code ThaumaturgeDrum}, metadata 0 and 1). Held down, it beats a
 * bass drum every 8 ticks. The plain drum calls a wisp of a random primal aspect every 30 ticks (up to 16 within
 * 64 blocks); the warped one a harmless mind spider every 10 (up to 16 within 32).
 */
public class ThaumaturgeDrum extends Item {
    private static final List<ResourceLocation> PRIMALS = List.of("aer", "terra", "ignis", "aqua", "ordo", "perditio").stream()
            .map(s -> ResourceLocation.fromNamespaceAndPath("thaumaturge", s)).toList();

    private final boolean warped;

    public ThaumaturgeDrum(Properties properties, boolean warped) {
        super(properties);
        this.warped = warped;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (level.isClientSide()) {
            return;
        }
        int used = this.getUseDuration(stack, user) - remaining;
        if (used % 8 == 0) {
            level.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.NOTE_BLOCK_BASEDRUM, SoundSource.PLAYERS,
                    1.0F, 0.5F + level.random.nextFloat() * 0.5F);
        }
        if (!this.warped) {
            if (used % 30 == 0) {
                AABB box = user.getBoundingBox().inflate(64.0, 32.0, 64.0);
                if (level.getEntitiesOfClass(WispEntity.class, box).size() < 16) {
                    WispEntity wisp = TTEntities.WISP.get().create(level);
                    if (wisp != null) {
                        wisp.moveTo(user.getX(), user.getY(), user.getZ(), level.random.nextFloat(), level.random.nextFloat());
                        wisp.setAspect(PRIMALS.get(level.random.nextInt(PRIMALS.size())));
                        level.addFreshEntity(wisp);
                    }
                }
            }
        } else if (used % 10 == 0) {
            AABB box = user.getBoundingBox().inflate(32.0);
            if (level.getEntitiesOfClass(EntityMindSpider.class, box).size() < 16) {
                EntityMindSpider spider = TTEntities.MIND_SPIDER.get().create(level);
                if (spider != null) {
                    spider.setHarmless(true);
                    spider.moveTo(user.getX(), user.getY(), user.getZ(), level.random.nextFloat(), level.random.nextFloat());
                    level.addFreshEntity(spider);
                }
            }
        }
    }

    public static ResourceLocation modelId(boolean warped) {
        return ThaumicConcilium.id(warped ? "warped_drum" : "thaumaturge_drum");
    }
}
