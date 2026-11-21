package dev.movingcastle.thaumicconcilium.spell;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.spell.SpellNode;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellContinuation;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellState;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellStats;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.leclowndu93150.thaumaturge.api.spell.cast.TouchBudget;
import com.leclowndu93150.thaumaturge.api.spell.event.SpellCastEvent;
import com.leclowndu93150.thaumaturge.api.spell.event.SpellNodeEvent;
import com.leclowndu93150.thaumaturge.api.spell.fx.SpellFx;
import com.leclowndu93150.thaumaturge.api.spell.part.SpellPart;
import dev.movingcastle.thaumicconcilium.TCConfig;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.item.ThaumaturgeDrum;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * The drum stance's per-cast state and its bonus while the drum is being played (rule B, decided 2026-11-21): the
 * stance's cast has its area radius and vis cost multiplied ({@link TCConfig#DRUM_RADIUS_MULTIPLIER},
 * {@link TCConfig#DRUM_VIS_COST_MULTIPLIER}). Both live on the casting thread for the length of one cast
 * ({@link #run}) and are gone afterwards: nothing is stored on the focus, the bracelet or the player.
 * <ul>
 *   <li><b>Vis cost:</b> {@link SpellCastEvent.Pre#setVisCost}, so the multiplied price is what Thaumaturge charges.</li>
 *   <li><b>Radius:</b> Thaumaturge sizes an area as the part's {@code radius} setting plus the {@code radius} spell
 *   stat (Widen), read from the node's {@link CastContext}. For each node with a {@code radius} setting (aura, burst,
 *   cloud, harvest) the {@link SpellNodeEvent} is cancelled and the part's own behaviour run on {@link Scaled}, a
 *   context that reports the stat raised so the total comes out multiplied. Everything else, children included, is
 *   delegated to the real context, so the bonus never leaks downstream.</li>
 * </ul>
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class DrumStance {
    private static final String RADIUS = "radius";
    /** Set for the length of a drum-stance cast: effects must not resolve an aimed target. */
    private static final ThreadLocal<Boolean> STANCE = new ThreadLocal<>();
    /** Set for a stance cast made while the drum is being played. */
    private static final ThreadLocal<Boolean> PLAYED = new ThreadLocal<>();

    private DrumStance() {
    }

    /** Whether the entity is using (holding right-click on) a drum: the drum is being played. */
    public static boolean playing(LivingEntity entity) {
        return entity != null && entity.isUsingItem() && entity.getUseItem().getItem() instanceof ThaumaturgeDrum;
    }

    /** Whether a drum-stance cast is running on this thread: effects use their unaimed behaviour. */
    public static boolean unaimed() {
        return STANCE.get() != null;
    }

    /** Whether the running stance cast has the played-drum bonus. */
    public static boolean played() {
        return PLAYED.get() != null;
    }

    /** Runs one stance cast: flags the thread, with the bonus if the drum is being played right now. */
    public static void run(LivingEntity caster, Runnable cast) {
        STANCE.set(Boolean.TRUE);
        if (playing(caster)) {
            PLAYED.set(Boolean.TRUE);
        }
        try {
            cast.run();
        } finally {
            STANCE.remove();
            PLAYED.remove();
        }
    }

    @SubscribeEvent
    public static void cost(SpellCastEvent.Pre event) {
        if (played()) {
            event.setVisCost(event.visCost() * TCConfig.DRUM_VIS_COST_MULTIPLIER.get().floatValue());
        }
    }

    /** Last, so any other listener that cancels or reroutes the node has done so first. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void radius(SpellNodeEvent event) {
        if (!played()) {
            return;
        }
        float factor = TCConfig.DRUM_RADIUS_MULTIPLIER.get().floatValue();
        CastContext ctx = event.context();
        if (factor == 1.0F || ctx.part().setting(RADIUS).isEmpty() || ctx instanceof Scaled) {
            return;
        }
        event.setCanceled(true);
        ctx.part().behavior().execute(new Scaled(ctx, factor), event.targets());
    }

    /** A node's context whose area comes out {@code factor} times as large; the rest is the real one. */
    private record Scaled(CastContext real, float factor) implements CastContext {
        @Override
        public SpellState state() {
            SpellState state = this.real.state();
            float stat = state.get(SpellStats.RADIUS);
            float total = Math.max(1, this.real.setting(RADIUS)) + stat;
            return state.with(SpellStats.RADIUS, stat + total * (this.factor - 1.0F));
        }

        @Override
        public ServerLevel level() {
            return this.real.level();
        }

        @Override
        public LivingEntity caster() {
            return this.real.caster();
        }

        @Override
        public Optional<UUID> casterId() {
            return this.real.casterId();
        }

        @Override
        public UUID castId() {
            return this.real.castId();
        }

        @Override
        public SpellNode node() {
            return this.real.node();
        }

        @Override
        public ResourceKey<SpellPart> partKey() {
            return this.real.partKey();
        }

        @Override
        public SpellPart part() {
            return this.real.part();
        }

        @Override
        public int setting(String key) {
            return this.real.setting(key);
        }

        @Override
        public Optional<Holder<IAspect>> aspect() {
            return this.real.aspect();
        }

        @Override
        public int color() {
            return this.real.color();
        }

        @Override
        public RandomSource random() {
            return this.real.random();
        }

        @Override
        public TouchBudget budget() {
            return this.real.budget();
        }

        @Override
        public SpellFx fx() {
            return this.real.fx();
        }

        @Override
        public boolean isAlly(Entity entity) {
            return this.real.isAlly(entity);
        }

        @Override
        public RegistryAccess registries() {
            return this.real.registries();
        }

        @Override
        public boolean hasNext() {
            return this.real.hasNext();
        }

        @Override
        public void proceed(List<SpellTarget> targets) {
            this.real.proceed(targets);
        }

        @Override
        public void proceed(List<SpellTarget> targets, SpellState next) {
            this.real.proceed(targets, next);
        }

        @Override
        public void proceedLater(int ticks, List<SpellTarget> targets, SpellState next) {
            this.real.proceedLater(ticks, targets, next);
        }

        @Override
        public SpellContinuation continuation() {
            return this.real.continuation();
        }

        @Override
        public SpellContinuation continuation(SpellState next) {
            return this.real.continuation(next);
        }
    }
}
