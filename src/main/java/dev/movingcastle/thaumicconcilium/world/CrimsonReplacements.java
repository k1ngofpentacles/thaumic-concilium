package dev.movingcastle.thaumicconcilium.world;

import com.leclowndu93150.thaumaturge.content.entity.EntityCultistKnight;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.mob.CrimsonPaladin;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Some of Thaumaturge's Crimson Knights join the world as Crimson Paladins instead (original: EntityJoinWorldEvent replacing
 * EntityCultistKnight at crimsonPaladinReplacesCultistWarriorChance, 20). Only fresh spawns are replaced, never loaded ones.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class CrimsonReplacements {
    private CrimsonReplacements() {
    }

    /** The replacement draw: {@code percent} in 0..100. */
    public static boolean shouldReplace(int percent, RandomSource random) {
        return percent > 0 && random.nextInt(100) < percent;
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.loadedFromDisk()
                || !(event.getEntity() instanceof EntityCultistKnight knight) || knight.getType() != TTEntities.CULTIST_KNIGHT.get()
                || knight.getPersistentData().getBoolean(dev.movingcastle.thaumicconcilium.entity.mob.LesserCrimsonPortal.NO_REPLACE)) {
            return;
        }
        if (!shouldReplace(TCConfig.PALADIN_REPLACES_KNIGHT.get(), level.random)) {
            return;
        }
        CrimsonPaladin paladin = TCEntities.CRIMSON_PALADIN.get().create(level);
        if (paladin == null) {
            return;
        }
        event.setCanceled(true);
        paladin.moveTo(knight.getX(), knight.getY(), knight.getZ(), knight.getYRot(), knight.getXRot());
        paladin.finalizeSpawn(level, level.getCurrentDifficultyAt(knight.blockPosition()), MobSpawnType.EVENT, null);
        if (knight.isPersistenceRequired()) {
            paladin.setPersistenceRequired();
        }
        level.addFreshEntity(paladin);
    }
}
