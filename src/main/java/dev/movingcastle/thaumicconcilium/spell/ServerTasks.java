package dev.movingcastle.thaumicconcilium.spell;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Short-lived server-side tasks ticked once per server tick, used for cosmetic sequences that the original
 * implemented as throwaway entities (the Wrath Attraction sigil, for instance). Tasks are not saved.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class ServerTasks {
    /** A ticking task; return {@code false} once finished. */
    @FunctionalInterface
    public interface Task {
        boolean tick();
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Task> PENDING = new ArrayList<>();

    private ServerTasks() {
    }

    public static void add(Task task) {
        PENDING.add(task);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        TASKS.addAll(PENDING);
        PENDING.clear();
        for (Iterator<Task> it = TASKS.iterator(); it.hasNext(); ) {
            boolean keep;
            try {
                keep = it.next().tick();
            } catch (RuntimeException e) {
                ThaumicConcilium.LOG.warn("Dropping a failed cosmetic task", e);
                keep = false;
            }
            if (!keep) {
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onStop(ServerStoppingEvent event) {
        TASKS.clear();
        PENDING.clear();
    }
}
