package dev.movingcastle.thaumicconcilium.world;

import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.mob.MadThaumaturge;
import dev.movingcastle.thaumicconcilium.entity.mob.ThaumaturgeEntity;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import dev.movingcastle.thaumicconcilium.spell.ServerTasks;
import com.leclowndu93150.thaumaturge.registry.TTEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Server-side spawning that isn't a biome spawn: wandering Thaumaturges, Nether Explorers on the lava sea, and Mad
 * Thaumaturges taking the place of Brainy Zombies. Each runs on a slow timer and touches only loaded chunks.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class ThaumaturgeWanderers {
    private static final int WANDERER_PERIOD = 1200;
    private static final int EXPLORER_PERIOD = 600;

    private ThaumaturgeWanderers() {
    }

    @SubscribeEvent
    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            return;
        }
        long time = level.getGameTime();
        if (level.dimension() == Level.OVERWORLD && time % WANDERER_PERIOD == 0) {
            wanderers(level);
        } else if (level.dimension() == Level.NETHER && time % EXPLORER_PERIOD == 0) {
            explorers(level);
        }
    }

    // ---------------------------------------------------------------- wandering Thaumaturges
    private static void wanderers(ServerLevel level) {
        RandomSource random = level.random;
        if (random.nextDouble() >= TCConfig.WANDERER_CHANCE.get()) {
            return;
        }
        ServerPlayer player = level.getRandomPlayer();
        if (player == null || player.isSpectator()) {
            return;
        }
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = 16.0 + random.nextDouble() * 32.0;
            int x = (int) (player.getX() + Math.cos(angle) * distance);
            int z = (int) (player.getZ() + Math.sin(angle) * distance);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
                continue;
            }
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (!SpawnPlacements.isSpawnPositionOk(TCEntities.THAUMATURGE.get(), level, pos)) {
                continue;
            }
            int count = random.nextInt(4) == 0 ? 2 : 1;
            for (int i = 0; i < count; i++) {
                ThaumaturgeEntity thaumaturge = TCEntities.THAUMATURGE.get().create(level);
                if (thaumaturge == null) {
                    return;
                }
                thaumaturge.moveTo(pos.getX() + 0.5 + i, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
                if (!level.noCollision(thaumaturge)) {
                    continue;
                }
                thaumaturge.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
                // A day or two, like the wandering trader.
                thaumaturge.setWanderer(24000 + random.nextInt(24000));
                level.addFreshEntity(thaumaturge);
            }
            return;
        }
    }

    // ---------------------------------------------------------------- Nether Explorers
    /**
     * Original: while at most three exist, one appears on a lava block (not within 24 blocks of a player, nor of the world
     * spawn). Here: near a random player in the Nether, on the surface of the lava sea.
     */
    private static void explorers(ServerLevel level) {
        int cap = TCConfig.NETHER_EXPLORERS.get();
        if (cap <= 0) {
            return;
        }
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }
        if (level.getEntities(TCEntities.NETHER_EXPLORER.get(), e -> true).size() >= cap) {
            return;
        }
        RandomSource random = level.random;
        ServerPlayer player = players.get(random.nextInt(players.size()));
        if (player.isSpectator()) {
            return;
        }
        BlockPos spawn = level.getSharedSpawnPos();
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = 24.0 + random.nextDouble() * 56.0;
            int x = (int) (player.getX() + Math.cos(angle) * distance);
            int z = (int) (player.getZ() + Math.sin(angle) * distance);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
                continue;
            }
            int top = Math.min(level.getMaxBuildHeight() - 2, player.getBlockY() + 24);
            int bottom = Math.max(level.getMinBuildHeight(), player.getBlockY() - 48);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top, z);
            for (int y = top; y > bottom; y--) {
                pos.setY(y);
                if (level.getFluidState(pos).is(FluidTags.LAVA) && level.getBlockState(pos.above()).isAir()) {
                    if (level.getNearestPlayer(x + 0.5, y, z + 0.5, 24.0, false) != null
                            || spawn.distToCenterSqr(x + 0.5, y, z + 0.5) < 576.0) {
                        break;
                    }
                    var explorer = TCEntities.NETHER_EXPLORER.get().create(level);
                    if (explorer != null) {
                        explorer.moveTo(x + 0.5, y + 0.9, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
                        level.addFreshEntity(explorer);
                    }
                    return;
                }
            }
        }
    }

    // ---------------------------------------------------------------- Mad Thaumaturges among Brainy Zombies
    @SubscribeEvent
    public static void replaceBrainyZombie(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getLevel() instanceof ServerLevel level)
                || event.getEntity().getType() != TTEntities.BRAINY_ZOMBIE.get()
                || level.random.nextInt(100) >= TCConfig.MAD_REPLACES_BRAINY.get()) {
            return;
        }
        var zombie = event.getEntity();
        double x = zombie.getX();
        double y = zombie.getY();
        double z = zombie.getZ();
        event.setCanceled(true);
        // Added on the next tick rather than from inside the join event.
        ServerTasks.add(() -> {
            MadThaumaturge mad = TCEntities.MAD_THAUMATURGE.get().create(level);
            if (mad != null) {
                mad.moveTo(x, y, z, level.random.nextFloat() * 360.0F, 0.0F);
                mad.addEquipment();
                level.addFreshEntity(mad);
            }
            return false;
        });
    }
}
