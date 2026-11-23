package dev.movingcastle.thaumicconcilium.golem;

import com.leclowndu93150.thaumaturge.api.golems.GolemDispatch;
import com.leclowndu93150.thaumaturge.api.golems.IGolemAPI;
import com.leclowndu93150.thaumaturge.api.golems.seals.ISealBehavior;
import com.leclowndu93150.thaumaturge.api.golems.seals.ISealEntity;
import com.leclowndu93150.thaumaturge.api.golems.seals.ISealFilter;
import com.leclowndu93150.thaumaturge.api.golems.seals.SealArea;
import com.leclowndu93150.thaumaturge.api.golems.seals.SealSetting;
import com.leclowndu93150.thaumaturge.api.golems.tasks.Task;
import com.leclowndu93150.thaumaturge.content.golem.EntityThaumaturgeGolem;
import com.leclowndu93150.thaumaturge.content.golem.tasks.TaskBoard;
import com.leclowndu93150.thaumaturge.content.world.space.LevelSpaces;
import com.leclowndu93150.thaumaturge.registry.TTItems;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumichorizons.jar.Jars;
import dev.movingcastle.thaumichorizons.registry.THDataComponents;
import dev.movingcastle.thaumichorizons.registry.THItems;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.Tags;

/**
 * The Assistant seal (phase 8; original: the Assistant golem core, WTFPL). A golem that carries an empty warded jar hunts the
 * creatures the seal wants, hits them until the blow it could strike would finish them, then puts them in the jar instead and
 * drops the filled critter jar at its feet (as the original did).
 * <p>
 * <b>Filter</b>: blood samples (Thaumic Horizons) name the species wanted; with none, passive animals (not tamed, not babies).
 * <b>Entropy</b> ("Hostile too"): monsters count as well. <b>Order</b> ("Only weakened"): the golem only takes on a creature that its
 * blow already finishes, so it never fights.
 * <p>
 * The seal's own clock scans every {@link #SCAN_PERIOD} ticks and only while no hunt is pending; the jarring is {@code completeTask}
 * only. The jar rules are the Containment focus's (no players, golems of Thaumaturge, {@code thaumichorizons:uncontainable}, bosses),
 * so a blood sample for a boss does not jar it (the original could).
 */
public final class AssistantBehavior implements ISealBehavior {
    /** Seal setting: only creatures the golem's blow already finishes (the Order upgrade). */
    public static final SealSetting ORDER = new SealSetting("only_weakened", "gui.thaumicconcilium.seal.setting.only_weakened", false);
    /** Seal setting: hostile creatures too (the Entropy upgrade). */
    public static final SealSetting ENTROPY = new SealSetting("hostile_too", "gui.thaumicconcilium.seal.setting.hostile_too", false);
    public static final int SCAN_PERIOD = 40;
    public static final int FILTER_SLOTS = 9;
    /** Task life, ticks: long enough to walk to the creature and wear it down. */
    public static final int TASK_LIFE = 400;

    public static final TagKey<EntityType<?>> UNCONTAINABLE = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("thaumichorizons", "uncontainable"));

    private int clock = System.identityHashCode(this) % SCAN_PERIOD;
    private int pendingTask = Integer.MIN_VALUE;

    /** The jar rules of the Containment focus (the original's own were none: any species was jarred). */
    public static boolean jarrable(net.minecraft.world.entity.Entity target) {
        return target instanceof LivingEntity living && !(target instanceof Player) && living.isAlive()
                && !(target instanceof EntityThaumaturgeGolem)
                && !target.getType().is(UNCONTAINABLE) && !target.getType().is(Tags.EntityTypes.BOSSES);
    }

    /** Entity type ids named by the blood samples in the seal's filter (read from the sample's data: no entity is built). */
    public static Set<String> wanted(ISealEntity seal) {
        Set<String> ids = new HashSet<>();
        ISealFilter filter = seal.filter().orElse(null);
        if (filter == null) {
            return ids;
        }
        for (int i = 0; i < filter.spec().slots(); i++) {
            ItemStack stack = filter.stack(i);
            if (stack.is(THItems.BLOOD_SAMPLE.get())) {
                CustomData data = stack.get(THDataComponents.CRITTER.get());
                String id = data == null ? "" : data.copyTag().getString("id");
                if (!id.isEmpty()) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    /** Whether the seal wants this creature (everything but the Order weakness test, which needs the golem's damage). */
    public static boolean wants(ISealEntity seal, LivingEntity target) {
        if (!jarrable(target)) {
            return false;
        }
        if (target instanceof TamableAnimal pet && pet.isTame() && seal.owner() != null && seal.owner().equals(pet.getOwnerUUID())) {
            return false;
        }
        Set<String> wanted = wanted(seal);
        if (!wanted.isEmpty()) {
            return wanted.contains(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString());
        }
        if (target instanceof Enemy) {
            return seal.setting(ENTROPY);
        }
        return (target instanceof Animal || target instanceof WaterAnimal) && !(target instanceof AbstractGolem)
                && !(target instanceof TamableAnimal pet && pet.isTame()) && !target.isBaby();
    }

    /** The golem's blow: the jar goes on when this is at least the creature's health (original rule, {@code damage >= health}). */
    public static boolean finishes(LivingEntity hunter, LivingEntity target) {
        return target.getHealth() <= hunter.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    public int pendingTask() {
        return this.pendingTask;
    }

    @Override
    public void tick(ServerLevel level, ISealEntity seal) {
        if (++this.clock % SCAN_PERIOD != 0) {
            return;
        }
        if (this.pendingTask != Integer.MIN_VALUE) {
            Task pending = TaskBoard.of(level).find(this.pendingTask);
            if (pending != null && !pending.isEnded() && !pending.isCompleted()) {
                return;
            }
            this.pendingTask = Integer.MIN_VALUE;
        }
        for (LivingEntity candidate : LevelSpaces.entitiesIn(level, LivingEntity.class, seal.pos().pos(), SealArea.bounds(seal),
                EntitySelector.NO_SPECTATORS)) {
            if (wants(seal, candidate)) {
                Task task = Task.onEntity(seal.pos(), candidate);
                task.setPriority(seal.priority());
                task.setLife(TASK_LIFE);
                GolemDispatch.postTask(level, task);
                this.pendingTask = task.id();
                return;
            }
        }
    }

    private static ItemStack carriedJar(IGolemAPI golem) {
        for (ItemStack held : golem.hands().contents()) {
            if (Jars.isEmptyJar(held)) {
                return held;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canPerform(ISealEntity seal, IGolemAPI golem, Task task) {
        if (!(task.entity() instanceof LivingEntity target) || !wants(seal, target) || golem.asEntity().isAlliedTo(target)) {
            return false;
        }
        if (carriedJar(golem).isEmpty()) {
            GolemDispatch.orderSupply(golem.level(), seal, new ItemStack(TTItems.JAR_NORMAL.get()));
            return false;
        }
        return !seal.setting(ORDER) || finishes(golem.asEntity(), target);
    }

    @Override
    public boolean completeTask(ServerLevel level, ISealEntity seal, IGolemAPI golem, Task task) {
        if (!(task.entity() instanceof LivingEntity target) || !target.isAlive() || !wants(seal, target)) {
            this.finish(task);
            return true;
        }
        LivingEntity hunter = golem.asEntity();
        ItemStack jar = carriedJar(golem);
        if (jar.isEmpty()) {
            return false;
        }
        golem.animateSwing();
        if (finishes(hunter, target)) {
            ItemStack empty = jar.copyWithCount(1);
            golem.hands().release(empty);
            ItemStack filled = Jars.jarStack(Jars.write(target));
            level.playSound(null, target.blockPosition(), TTSounds.CRAFTFAIL.get(), SoundSource.NEUTRAL, 0.8F, 1.0F);
            target.discard();   // no death, no loot, no experience
            hunter.spawnAtLocation(filled, 1.0F);
            golem.grantRankExperience(1);
            this.finish(task);
            return true;
        }
        if (hunter instanceof Mob mob) {
            mob.doHurtTarget(target);
        }
        return false;   // the task stays open: the next attempt hits again or jars
    }

    private void finish(Task task) {
        task.end();
        this.pendingTask = Integer.MIN_VALUE;
    }

    @Override
    public void onRemoved(ServerLevel level, ISealEntity seal) {
        if (this.pendingTask != Integer.MIN_VALUE) {
            Task pending = TaskBoard.of(level).find(this.pendingTask);
            if (pending != null) {
                pending.end();
            }
            this.pendingTask = Integer.MIN_VALUE;
        }
    }
}
