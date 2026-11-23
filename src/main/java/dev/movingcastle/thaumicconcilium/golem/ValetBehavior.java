package dev.movingcastle.thaumicconcilium.golem;

import com.leclowndu93150.thaumaturge.api.golems.GolemDispatch;
import com.leclowndu93150.thaumaturge.api.golems.IGolemAPI;
import com.leclowndu93150.thaumaturge.api.golems.seals.ISealBehavior;
import com.leclowndu93150.thaumaturge.api.golems.seals.ISealEntity;
import com.leclowndu93150.thaumaturge.api.golems.seals.ISealFilter;
import com.leclowndu93150.thaumaturge.api.golems.tasks.Task;
import com.leclowndu93150.thaumaturge.api.items.StackFilter;
import com.leclowndu93150.thaumaturge.content.golem.GolemInteractionHelper;
import com.leclowndu93150.thaumaturge.content.golem.seals.behavior.ItemMatchSettings;
import com.leclowndu93150.thaumaturge.content.golem.seals.behavior.UseBehavior;
import com.leclowndu93150.thaumaturge.content.golem.tasks.TaskBoard;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * The Valet seal's behaviour (phase 7; original: Valet golem core, WTFPL). It is the Use seal's click, held back until the
 * owner says the seal's phrase (see {@link ValetTriggers}). Filter slot 0 is the tool (as on the Use seal), slot 1 the phrase
 * item, renamed in an anvil.
 * <p>
 * Armed is transient (not saved): a game-time expiry, 200 ticks as the original golem's walk budget. While unarmed {@code tick}
 * returns at once, so an idle Valet seal costs nothing. One arming gives one click.
 */
public final class ValetBehavior implements ISealBehavior {
    /** The original's walk budget, in ticks. */
    public static final int ARMED_TICKS = 200;
    /** Filter slot of the tool the golem clicks with. */
    public static final int TOOL_SLOT = 0;
    /** Filter slot of the phrase item. */
    public static final int PHRASE_SLOT = 1;

    /** Game time at which the arming lapses; 0 = unarmed. */
    private long armedUntil;
    private int pendingTask = Integer.MIN_VALUE;

    public boolean isArmed() {
        return this.armedUntil != 0L;
    }

    /** The custom name of the phrase item in the seal's filter, or null when it has none (then the seal never arms). */
    public static String phrase(ISealEntity seal) {
        ISealFilter filter = seal.filter().orElse(null);
        if (filter == null || filter.spec().slots() <= PHRASE_SLOT) {
            return null;
        }
        Component name = filter.stack(PHRASE_SLOT).get(DataComponents.CUSTOM_NAME);
        return name == null ? null : name.getString();
    }

    /** Arms (or keeps armed, restarting the expiry) for one click. */
    public void arm(ServerLevel level) {
        this.armedUntil = level.getGameTime() + ARMED_TICKS;
    }

    private void disarm(ServerLevel level) {
        if (this.pendingTask != Integer.MIN_VALUE) {
            Task pending = TaskBoard.of(level).find(this.pendingTask);
            if (pending != null) {
                pending.end();
            }
        }
        this.armedUntil = 0L;
        this.pendingTask = Integer.MIN_VALUE;
    }

    @Override
    public void tick(ServerLevel level, ISealEntity seal) {
        if (this.armedUntil == 0L) {
            return;
        }
        if (level.getGameTime() >= this.armedUntil) {
            this.disarm(level);
        } else if (this.pendingTask == Integer.MIN_VALUE) {
            if (targetReady(level, seal)) {
                Task task = Task.atBlock(seal.pos(), seal.pos().pos());
                task.setPriority(seal.priority());
                GolemDispatch.postTask(level, task);
                this.pendingTask = task.id();
            } else {
                this.disarm(level);   // nothing to click: the original disarmed when no target matched
            }
        } else {
            Task pending = TaskBoard.of(level).find(this.pendingTask);
            if (pending == null || pending.isEnded() || pending.isCompleted()) {
                this.disarm(level);
            }
        }
    }

    private static boolean targetReady(ServerLevel level, ISealEntity seal) {
        return seal.setting(UseBehavior.INTO_AIR) == level.getBlockState(seal.pos().pos()).isAir();
    }

    @Override
    public boolean completeTask(ServerLevel level, ISealEntity seal, IGolemAPI golem, Task task) {
        if (this.isArmed() && targetReady(level, seal)) {
            ItemStack tool = pickTool(seal, golem);
            boolean bareHanded = seal.setting(UseBehavior.BARE_HANDED);
            if (!tool.isEmpty() || bareHanded) {
                ItemStack used = tool.copy();
                if (!tool.isEmpty()) {
                    golem.hands().release(tool.copy());
                }
                GolemInteractionHelper.golemClick(level, golem, task.pos(), seal.pos().face(), bareHanded ? ItemStack.EMPTY : used,
                        seal.setting(UseBehavior.SNEAKING), !seal.setting(UseBehavior.LEFT_CLICK));
            }
        }
        task.end();
        this.armedUntil = 0L;
        this.pendingTask = Integer.MIN_VALUE;
        return true;
    }

    private static ItemStack pickTool(ISealEntity seal, IGolemAPI golem) {
        ISealFilter filter = seal.filter().orElseThrow();
        return filter.stack(TOOL_SLOT).isEmpty() ? golem.hands().contents().get(0) : matchCarried(seal, golem);
    }

    private static ItemStack matchCarried(ISealEntity seal, IGolemAPI golem) {
        ISealFilter filter = seal.filter().orElseThrow();
        return new StackFilter(List.of(filter.stack(TOOL_SLOT)), List.of(filter.limit(TOOL_SLOT)), filter.isBlacklist(), ItemMatchSettings.of(seal))
                .firstAcceptedWithLimit(golem.hands().contents())
                .stack();
    }

    @Override
    public boolean canPerform(ISealEntity seal, IGolemAPI golem, Task task) {
        if (seal.setting(UseBehavior.BARE_HANDED)) {
            return true;
        }
        boolean carrying = !matchCarried(seal, golem).isEmpty();
        ISealFilter filter = seal.filter().orElseThrow();
        if (!carrying && seal.setting(UseBehavior.REQUEST_ITEMS) && !filter.isBlacklist() && !filter.stack(TOOL_SLOT).isEmpty()) {
            GolemDispatch.orderSupply(golem.level(), seal, filter.stack(TOOL_SLOT).copy());
        }
        return carrying;
    }

    @Override
    public void onRemoved(ServerLevel level, ISealEntity seal) {
        this.disarm(level);
    }
}
