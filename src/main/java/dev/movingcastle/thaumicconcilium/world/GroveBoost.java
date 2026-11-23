package dev.movingcastle.thaumicconcilium.world;

import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;

/**
 * Phase 5d cycle 4e (memo section 4): within the Concilium Grove, crops and trees grow {@code growth.boost} (25%) faster. Event-driven, no ticking
 * of our own: a growth that happened gets a 25% chance of one extra growth step, which is x 1.25 whatever the block's own odds.
 * <ul>
 * <li>Crops, stems, cocoa, wart, berries, cactus, cane, bamboo, chorus and the mods that use the hook: {@link CropGrowEvent.Post} re-ticks the block;
 * under the re-entry flag {@link CropGrowEvent.Pre} answers GROW, so the extra step does grow (Pre alone cannot: it does not carry the block's own roll).
 * <li>Trees: Dynamic Trees and vanilla saplings post no event; two optional mixins call {@link #extra} at the tail of their random tick.
 * </ul>
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class GroveBoost {
    private static boolean reentry;

    /** Tests: extra steps given since the game started. */
    public static long extras;
    /** Tests: growth ticks the hooks saw (an extra step is a chance per one of these). */
    public static long calls;

    private GroveBoost() {
    }

    /** The chance of the one extra step: the configured boost (0 = off). */
    public static double chance() {
        return chanceOverride != null ? chanceOverride : TCConfig.GROVE_BOOST.get();
    }

    /** Tests: replaces the configured chance while set. */
    public static Double chanceOverride;

    /** Pure: whether a roll {@code u} in [0, 1) earns the extra step at boost {@code boost}. */
    public static boolean rolls(double u, double boost) {
        return boost > 0.0 && u < boost;
    }

    /** One extra random tick of the block at {@code pos} when the roll succeeds and the column is grove. Re-entry guarded; no-op when off. */
    public static void extra(ServerLevel level, BlockPos pos) {
        if (!reentry) {
            calls++;
        }
        if (reentry || !rolls(level.random.nextDouble(), chance()) || !GroveColumns.isGrove(level, pos)) {
            return;
        }
        reentry = true;
        try {
            BlockState state = level.getBlockState(pos);
            state.randomTick(level, pos, level.random);
            extras++;
        } finally {
            reentry = false;
        }
    }

    @SubscribeEvent
    public static void onPre(CropGrowEvent.Pre event) {
        if (reentry) {
            event.setResult(CropGrowEvent.Pre.Result.GROW); // the extra step is the boost: it grows
        }
    }

    @SubscribeEvent
    public static void onPost(CropGrowEvent.Post event) {
        if (reentry || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        extra(level, event.getPos());
    }
}
