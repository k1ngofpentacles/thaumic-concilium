package dev.movingcastle.thaumicconcilium.compat.witchery;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.capability.PlayerResearchAccess;
import com.leclowndu93150.thaumaturge.api.research.ResearchEvent;
import com.leclowndu93150.thaumaturge.content.particle.BoltParticleOptions;
import com.leclowndu93150.thaumaturge.content.particle.SparkleParticleOptions;
import com.leclowndu93150.thaumaturge.content.research.pool.AspectPools;
import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.EtherealShackles;
import dev.movingcastle.thaumicconcilium.entity.ShardPowder;
import dev.movingcastle.thaumicconcilium.item.ShardKind;
import dev.movingcastle.thaumicconcilium.registry.TCSounds;
import dev.movingcastle.thaumicconcilium.spell.Looking;
import dev.sterner.witchery.core.api.SymbologySpell;
import dev.sterner.witchery.core.registry.WitcheryInfusions;
import dev.sterner.witchery.core.registry.WitcherySymbologies;
import dev.sterner.witchery.feature.infusion.InfusionPlayerAttachment;
import dev.sterner.witchery.feature.infusion.InfusionType;
import dev.sterner.witchery.feature.symbology.GridDirection;
import dev.sterner.witchery.feature.symbology.SymbologyPlayerAttachment;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Phase 10-3 (user decision R4 A): the five Crimson Cult spells (original WitcheryIntegration, symbols 90-94) as spells of the remake's
 * symbology, cast with an Ent Twig while infused. Effects are the original's by infusion type: infernal (inf 4), overworld (2), light (1).
 * Only loaded with {@code witchery} ({@link WitcheryGate}); this class is one of the few that name its classes.
 */
public final class CrimsonSpells {
    /** PROVISIONAL(R9): the same share of a full infusion as the original's 2 of 20 charges (the remake counts 6000). Option B would be 200. */
    public static final int COST = 600;
    /** The research that teaches all five (original CRIMSONSPELLS). */
    public static final ResourceLocation RESEARCH = ThaumicConcilium.id("crimson_spells");
    private static final int RANGE = 32;

    public static final DeferredRegister<SymbologySpell> SPELLS =
            DeferredRegister.create(WitcherySymbologies.INSTANCE.getSYMBOLOGY_REGISTRY_KEY(), ThaumicConcilium.MODID);

    // Strokes (1.7.10: 0 up, 1 down, 2 right, 3 left), converted 1:1 to the remake's grid directions.
    private static final GridDirection U = GridDirection.UP, D = GridDirection.DOWN, L = GridDirection.LEFT, R = GridDirection.RIGHT;

    public static final DeferredHolder<SymbologySpell, SymbologySpell> INCARCEROUS = SPELLS.register("crimson_incarcerous",
            () -> new Incarcerous(List.of(R, R, R, U, L, L, D)));
    public static final DeferredHolder<SymbologySpell, SymbologySpell> ARROW = SPELLS.register("crimson_arrow",
            () -> new ArrowSpell(List.of(D, D, D, L)));
    public static final DeferredHolder<SymbologySpell, SymbologySpell> SPIDER = SPELLS.register("crimson_spider",
            () -> new Bolt("crimson_spider", List.of(U, U, U, R, D, D, U, U), EntityTypeTags.ARTHROPOD, 0, 1));
    public static final DeferredHolder<SymbologySpell, SymbologySpell> UNDEAD = SPELLS.register("crimson_undead",
            () -> new Bolt("crimson_undead", List.of(D, D, D, U, L, R, R), EntityTypeTags.UNDEAD, 0, 1));
    /**
     * R10 (user: both strokes as drawn): the original's second stroke, always level 2 (damage nextInt(30) + 4) at double cost (2^(level-1) * cost).
     * Five cells down from the start does not fit the remake's screen as shipped: see SymbologyGridWidenMixin.
     */
    public static final DeferredHolder<SymbologySpell, SymbologySpell> UNDEAD_GREATER = SPELLS.register("crimson_undead_greater",
            () -> new Bolt("crimson_undead_greater", List.of(D, D, D, D, D, U, U, L, L, R, R, R, R), EntityTypeTags.UNDEAD, 2, 2));
    public static final DeferredHolder<SymbologySpell, SymbologySpell> BLIND = SPELLS.register("crimson_blind",
            () -> new Blind(List.of(U, U, L, L, D, D, R, R)));

    private CrimsonSpells() {
    }

    public static void init(IEventBus modBus) {
        SPELLS.register(modBus);
        NeoForge.EVENT_BUS.addListener(CrimsonSpells::onCompleted);
        NeoForge.EVENT_BUS.addListener(CrimsonSpells::onLogin);
    }

    // ------------------------------------------------------------------------------------------------ unlock

    /** Original TCEntityEventHandler.onResearch: finishing CRIMSONSPELLS teaches all five. */
    private static void onCompleted(ResearchEvent.Completed event) {
        if (event.player() instanceof ServerPlayer player && event.research().equals(RESEARCH)) {
            learnAll(player);
        }
    }

    /** Catch-up for a player who finished the research while the remake was not installed. */
    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && PlayerResearchAccess.of(player).hasCompleted(RESEARCH)) {
            for (DeferredHolder<SymbologySpell, SymbologySpell> spell : all()) {
                if (!SymbologyPlayerAttachment.INSTANCE.hasLearnedSpell(player, spell.getId())) {
                    SymbologyPlayerAttachment.INSTANCE.learnSpell(player, spell.getId());
                }
            }
        }
    }

    public static void learnAll(ServerPlayer player) {
        for (DeferredHolder<SymbologySpell, SymbologySpell> spell : all()) {
            SymbologyPlayerAttachment.INSTANCE.learnSpell(player, spell.getId());
        }
    }

    public static List<DeferredHolder<SymbologySpell, SymbologySpell>> all() {
        return List.of(INCARCEROUS, ARROW, SPIDER, UNDEAD, UNDEAD_GREATER, BLIND);
    }

    // ------------------------------------------------------------------------------------------------ helpers

    private static InfusionType infusion(ServerPlayer player) {
        return InfusionPlayerAttachment.getData(player).getType();
    }

    private static boolean is(ServerPlayer player, Supplier<? extends InfusionType> type) {
        return infusion(player) == type.get();
    }

    private static boolean infernal(ServerPlayer player) {
        return is(player, WitcheryInfusions.INSTANCE.getINFERNAL());
    }

    /** The original's Thaumcraft aspect pool, as Thaumaturge's. */
    private static int pool(ServerLevel level, ServerPlayer player, net.minecraft.resources.ResourceKey<IAspect> aspect) {
        Holder<IAspect> holder = level.registryAccess().registryOrThrow(IAspect.REGISTRY_KEY).getHolderOrThrow(aspect);
        return AspectPools.amount(player, holder);
    }

    private abstract static class Base implements SymbologySpell {
        private final ResourceLocation id;
        private final List<GridDirection> path;

        private final int costFactor;

        Base(String name, List<GridDirection> path) {
            this(name, path, 1);
        }

        Base(String name, List<GridDirection> path, int costFactor) {
            this.costFactor = costFactor;
            this.id = ThaumicConcilium.id(name);
            this.path = path;
        }

        @Override
        public ResourceLocation getId() {
            return this.id;
        }

        @Override
        public List<GridDirection> getGridPath() {
            return this.path;
        }

        @Override
        public int getCost() {
            return COST * this.costFactor;
        }

        @Override
        public int getMaxLevel() {
            return 1;
        }
    }

    // ------------------------------------------------------------------------------------------------ Incarcerous

    /** Infernal: a thrown Ethereal Shackles. Otherwise 2-4 Sloth powder orbs (the original's ShardPowder type 12) and the ghast fireball sound. */
    private static final class Incarcerous extends Base {
        Incarcerous(List<GridDirection> path) {
            super("crimson_incarcerous", path);
        }

        @Override
        public void onUse(ServerLevel level, ServerPlayer player, Vec3 look, int spellLevel) {
            if (infernal(player)) {
                EtherealShackles shackles = new EtherealShackles(level, player);
                shackles.moveTo(player.getX(), player.getEyeY(), player.getZ());
                Vec3 dir = player.getLookAngle();
                for (int i = 0; i < 3; i++) {
                    shackles.move(MoverType.SELF, dir);
                }
                shackles.shoot(dir.x, dir.y, dir.z, 2.0F, 2.0F);
                level.addFreshEntity(shackles);
                level.playSound(null, shackles.getX(), shackles.getY(), shackles.getZ(), TCSounds.SHACKLES_THROW.get(), SoundSource.PLAYERS, 0.5F, 1.0F);
                return;
            }
            int orbs = 2 + level.random.nextInt(3);
            level.levelEvent(null, LevelEvent.SOUND_GHAST_FIREBALL, player.blockPosition(), 0);
            double yaw = Math.toRadians(player.getYRot());
            Vec3 eye = player.getEyePosition();
            Vec3 at = new Vec3(eye.x + look.x - Math.cos(yaw) * 0.16F, eye.y + look.y - 0.5, eye.z + look.z - Math.sin(yaw) * 0.16F);
            for (int i = 0; i < orbs; i++) {
                // The original normalises the look and only then adds the jitter: 1 block per tick (spray scales by 0.4, so 2.5 here).
                Vec3 v = look.normalize().add(level.random.nextGaussian() * 0.0075 * 12.0, level.random.nextGaussian() * 0.0075 * 12.0,
                        level.random.nextGaussian() * 0.0075 * 12.0);
                ShardPowder.spray(level, player, at, v.scale(2.5), ShardKind.SLOTH, 0);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ Intrare Polosae

    /** A critical arrow: overworld infusion sets its damage by the weapon pool, otherwise base damage + 5. */
    private static final class ArrowSpell extends Base {
        ArrowSpell(List<GridDirection> path) {
            super("crimson_arrow", path);
        }

        @Override
        public void onUse(ServerLevel level, ServerPlayer player, Vec3 look, int spellLevel) {
            Arrow arrow = new Arrow(level, player, new ItemStack(Items.ARROW), null);
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 1.0F);
            arrow.setCritArrow(true);
            arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            if (is(player, WitcheryInfusions.INSTANCE.getOVERWORLD())) {
                arrow.setBaseDamage(1.0 + Math.sqrt(pool(level, player, TTAspects.AVERSIO)) * 1.5);
            } else {
                arrow.setBaseDamage(arrow.getBaseDamage() + 5.0);
            }
            level.addFreshEntity(arrow);
        }
    }

    // ------------------------------------------------------------------------------------------------ Ariania Exumai, Trup Ehsolvi

    /** The pointed monster of the right kind takes magic damage: light infusion by the lux pool, otherwise nextInt(30) + level * 2. */
    private static final class Bolt extends Base {
        private final net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> kind;

        private final int forcedLevel;

        /** {@code forcedLevel} 0 = the remake's level; {@code costFactor} multiplies {@link #COST}. */
        Bolt(String name, List<GridDirection> path, net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> kind, int forcedLevel, int costFactor) {
            super(name, path, costFactor);
            this.forcedLevel = forcedLevel;
            this.kind = kind;
        }

        @Override
        public void onUse(ServerLevel level, ServerPlayer player, Vec3 look, int spellLevel) {
            Entity e = Looking.at(player, RANGE, 1.0);
            if (!(e instanceof Monster mob) || !mob.getType().is(this.kind) || !mob.isAlive()) {
                return;
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.CRAFTFAIL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            Vec3 from = new Vec3(player.getX(), player.getY() + player.getBbHeight() - 0.5, player.getZ());
            Vec3 to = new Vec3(mob.getX(), mob.getY() + mob.getBbHeight() / 2.0F, mob.getZ());
            // Look spec (prebrief 7.1): Thaumaturge's white bolt particle and one sparkle at the target, to every player within 32 (forced).
            BoltParticleOptions bolt = new BoltParticleOptions(to.x, to.y, to.z, 1.0F, 1.0F, 1.0F, 0.45F);
            SparkleParticleOptions sparkle = new SparkleParticleOptions(0xFFFFFF, 1.5F, 0, 0.0F, 0.0F, 18, false);
            for (ServerPlayer viewer : level.players()) {
                if (viewer.distanceToSqr(player) <= RANGE * RANGE) {
                    level.sendParticles(viewer, bolt, true, from.x, from.y, from.z, 1, 0.0, 0.0, 0.0, 0.0);
                    level.sendParticles(viewer, sparkle, true, to.x, to.y, to.z, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
            DamageSource source = level.damageSources().indirectMagic(player, player);
            if (is(player, WitcheryInfusions.INSTANCE.getLIGHT())) {
                mob.hurt(source, (float) (1.0 + Math.sqrt(pool(level, player, TTAspects.LUX)) * 2.5));
            } else {
                mob.hurt(source, (float) (level.random.nextInt(30) + (this.forcedLevel > 0 ? this.forcedLevel : spellLevel) * 2));
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ Oculus Madesco

    /** Blurred vision on the pointed mob; infernal adds the tenebrae pool to the time and Blindness. */
    private static final class Blind extends Base {
        Blind(List<GridDirection> path) {
            super("crimson_blind", path);
        }

        @Override
        public void onUse(ServerLevel level, ServerPlayer player, Vec3 look, int spellLevel) {
            boolean infernal = infernal(player);
            int dur = 200;
            if (infernal) {
                dur += pool(level, player, TTAspects.TENEBRAE);
            }
            if (!(Looking.at(player, RANGE, 1.0) instanceof Mob mob)) {
                return;
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.CRAFTFAIL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            // The original clears the effects' curative items (milk, in 1.7.10); in 1.21 that is the cures set.
            MobEffectInstance blurred = new MobEffectInstance(TTMobEffects.BLURRED_VISION, dur + 200, 60);
            blurred.getCures().clear();
            mob.addEffect(blurred);
            if (infernal) {
                MobEffectInstance blind = new MobEffectInstance(MobEffects.BLINDNESS, dur, 60);
                blind.getCures().clear();
                mob.addEffect(blind);
            }
        }
    }
}
