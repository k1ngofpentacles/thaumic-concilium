package dev.movingcastle.thaumicconcilium.rift;

import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.leclowndu93150.thaumaturge.content.warp.WarpEvents;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fires one Thaumaturge warp event for a player at once (phase 5c: an absent contributor's debt comes due on login).
 * Thaumaturge's only public entry, {@code WarpEvents.checkWarpEvent}, rolls its own chance (about 1 in 100 for a fresh warp counter) and
 * would almost never fire, so this calls its private {@code dispatchEvent(player, eff, Roll)} by reflection with a roll scaled to
 * the player's warp. If Thaumaturge changes that, it falls back to {@code checkWarpEvent} and logs once.
 */
public final class WarpKick {
    private static final Logger LOG = LoggerFactory.getLogger("thaumicconcilium");
    private static Method dispatch;
    private static Constructor<?> rollCtor;
    private static boolean failed;

    private WarpKick() {
    }

    /** Fires an event; returns true if the direct dispatch ran, false if only the chance-based fallback was used. */
    public static boolean fire(ServerPlayer player) {
        int actual = Math.max(1, Math.min(100, WarpHelper.getActualWarp(player)));
        RandomSource random = player.getRandom();
        if (!failed) {
            try {
                if (dispatch == null) {
                    Class<?> roll = Class.forName("com.leclowndu93150.thaumaturge.content.warp.WarpEvents$Roll");
                    rollCtor = roll.getDeclaredConstructors()[0];
                    rollCtor.setAccessible(true);
                    dispatch = WarpEvents.class.getDeclaredMethod("dispatchEvent", ServerPlayer.class, int.class, roll);
                    dispatch.setAccessible(true);
                }
                Object roll = rollCtor.newInstance(actual, WarpHelper.getWarp(player).get(WarpType.NORMAL), random);
                dispatch.invoke(null, player, 1 + random.nextInt(actual), roll);
                return true;
            } catch (ReflectiveOperationException | RuntimeException e) {
                failed = true;
                LOG.warn("Thaumaturge WarpEvents.dispatchEvent(ServerPlayer,int,Roll) not usable ({}); using its chance-based checkWarpEvent", e.toString());
            }
        }
        WarpEvents.checkWarpEvent(player);
        return false;
    }
}
