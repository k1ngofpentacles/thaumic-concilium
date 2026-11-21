package dev.movingcastle.thaumicconcilium.entity.mob;

import com.leclowndu93150.thaumaturge.content.entity.EntityMindSpider;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.spell.Taint;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * The smart taint spider (original: {@code mobs.TaintSpiderSmart}): a taint spider that never picks its own target and
 * drops nothing. The Taint Animation focus turns commanded taint spiders and nearby mind spiders into these, so they
 * fight only what the caster points at. Thaumaturge's taint spiders are vanilla spiders with the tainted trait, so a
 * smart one is the same, tagged.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class SmartTaintSpiders {
    public static final String TAG = ThaumicConcilium.MODID + ".smart_taint_spider";

    private SmartTaintSpiders() {
    }

    public static boolean isSmart(Entity entity) {
        return entity.getTags().contains(TAG);
    }

    /** A taint spider that isn't smart yet (nor a mind spider, which is a spider subclass). */
    public static boolean isPlainTaintSpider(Entity entity) {
        return entity instanceof Spider spider && !(spider instanceof EntityMindSpider) && !isSmart(spider) && Taint.isTainted(spider);
    }

    public static Spider spawn(ServerLevel level, Vec3 at, float yaw) {
        Spider spider = Taint.spawnTaintSpider(level, at, yaw);
        if (spider != null) {
            spider.addTag(TAG);
            makeSmart(spider);
        }
        return spider;
    }

    private static void makeSmart(Spider spider) {
        // Keeps retaliation (HurtByTargetGoal), as the original did.
        spider.targetSelector.removeAllGoals(goal -> goal instanceof NearestAttackableTargetGoal<?>);
    }

    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Spider spider && isSmart(spider)) {
            makeSmart(spider);
        }
    }

    @SubscribeEvent
    public static void drops(LivingDropsEvent event) {
        if (isSmart(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
