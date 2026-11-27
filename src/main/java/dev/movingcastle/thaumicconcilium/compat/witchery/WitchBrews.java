package dev.movingcastle.thaumicconcilium.compat.witchery;

import dev.movingcastle.thaumicconcilium.entity.mob.BurnedWitch;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * The Burned Witch's brews (R2 A) and her immunity to them. Everything is looked up by registry id: the remake's brews fly as its
 * own {@code witchery:thrown_brew} entity carrying its brew item, so this class names no {@code dev.sterner} class (only
 * {@link WitchAfflictions} does). Exists only with Witchery.
 *
 * <p>Original brews (a Witchery BREW item with an NBT ingredient list, thrown as an {@code EntityBrew}): roll 0 "ill-fitting", roll 1
 * the anti-vampire, anti-werewolf or plain attack brew by the target, roll 2 the attack brew. Mappings settled by the lane:
 * ill-fitting = Brew of Webs, attack = Brew of Wasting, vampire = Sun Grenade, werewolf = a splash of the wolfsbane effect
 * ({@code witchery:werewolf_bound}) with instant damage.
 *
 * <p>Immunity (original: the Witchery gas mask potion on her, every tick): a brew or wolfsbane splash that reaches her applies
 * nothing to her. {@link ProjectileImpactEvent} stamps each witch inside the splash box with the game time; her effects
 * ({@link MobEffectEvent.Applicable}) and the projectile's damage ({@link LivingIncomingDamageEvent}) are refused in that tick.
 */
public final class WitchBrews {
    public enum Kind { WEBS, SUN_GRENADE, WOLFSBANE_SPLASH, WASTING }

    public static final ResourceLocation THROWN_BREW = ResourceLocation.fromNamespaceAndPath("witchery", "thrown_brew");
    public static final ResourceLocation SUN_GRENADE = ResourceLocation.fromNamespaceAndPath("witchery", "sun_grenade");
    public static final ResourceLocation BREW_OF_WEBS = ResourceLocation.fromNamespaceAndPath("witchery", "brew_of_webs");
    public static final ResourceLocation BREW_OF_WASTING = ResourceLocation.fromNamespaceAndPath("witchery", "brew_of_wasting");
    /** The remake's wolfsbane effect (lang {@code potion_effect.wolfsbane}). */
    public static final ResourceLocation WEREWOLF_BOUND = ResourceLocation.fromNamespaceAndPath("witchery", "werewolf_bound");
    private static final int WOLFSBANE_TICKS = 600;
    /** The splash's reach, the same box as the remake's and vanilla's (4 by 2 by 4 around the flask). */
    private static final double SPLASH_X = 4.0;
    private static final double SPLASH_Y = 2.0;

    private WitchBrews() {
    }

    static void registerListeners() {
        NeoForge.EVENT_BUS.addListener(WitchBrews::onImpact);
        NeoForge.EVENT_BUS.addListener(WitchBrews::onEffectApplicable);
        NeoForge.EVENT_BUS.addListener(WitchBrews::onDamage);
    }

    /** The original's choice: {@code roll} is {@code nextInt(3)}. */
    public static Kind choose(Player target, int roll) {
        if (roll == 0) {
            return Kind.WEBS;
        }
        if (roll == 1) {
            if (WitchAfflictions.vampire(target)) {
                return Kind.SUN_GRENADE;
            }
            if (WitchAfflictions.werewolf(target)) {
                return Kind.WOLFSBANE_SPLASH;
            }
        }
        return Kind.WASTING;
    }

    /** Builds the flying projectile at the witch's hand (original: thrown from her eye line, as any thrower's), not yet shot. */
    public static Projectile make(BurnedWitch witch, Kind kind) {
        Projectile p = switch (kind) {
            case SUN_GRENADE -> create(SUN_GRENADE, witch);
            case WOLFSBANE_SPLASH -> {
                ThrownPotion potion = new ThrownPotion(witch.level(), witch);
                potion.setItem(wolfsbaneFlask());
                yield potion;
            }
            case WEBS -> brew(witch, BREW_OF_WEBS);
            case WASTING -> brew(witch, BREW_OF_WASTING);
        };
        p.setPos(witch.getX(), witch.getEyeY() - 0.1, witch.getZ());
        return p;
    }

    private static Projectile brew(BurnedWitch witch, ResourceLocation item) {
        Projectile p = create(THROWN_BREW, witch);
        ((ThrowableItemProjectile) p).setItem(new ItemStack(BuiltInRegistries.ITEM.get(item)));
        return p;
    }

    private static Projectile create(ResourceLocation type, BurnedWitch witch) {
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(type);
        Projectile p = (Projectile) entityType.create(witch.level());
        p.setOwner(witch);
        return p;
    }

    /** A vanilla splash flask holding the wolfsbane effect for 600 ticks and instant damage I (colour: the remake's wolfsbane). */
    public static ItemStack wolfsbaneFlask() {
        Holder<MobEffect> bound = BuiltInRegistries.MOB_EFFECT.getHolder(WEREWOLF_BOUND).orElseThrow();
        ItemStack flask = new ItemStack(Items.SPLASH_POTION);
        flask.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.of(0xFAB41E),
                List.of(new MobEffectInstance(bound, WOLFSBANE_TICKS, 0), new MobEffectInstance(MobEffects.HARM, 1, 0))));
        return flask;
    }

    /** Original {@code throwPotion}: heading speed 0.75, inaccuracy 8, chest height minus 1.1, an arc of 0.2 per block of distance. */
    public static void throwAt(BurnedWitch witch, Player target, int roll) {
        Projectile p = make(witch, choose(target, roll));
        double dx = target.getX() - witch.getX();
        double dz = target.getZ() - witch.getZ();
        double dy = target.getY() + target.getEyeHeight() - 1.1 - p.getY();
        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        p.shoot(dx, dy + dist * 0.2F, dz, 0.75F, 8.0F);
        witch.level().addFreshEntity(p);
    }

    // ---------------------------------------------------------------- immunity

    /** A remake brew flask, or a splash flask carrying the wolfsbane effect (hers or anyone's). */
    public static boolean isBrewProjectile(Entity e) {
        if (e == null) {
            return false;
        }
        if (BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).equals(THROWN_BREW)) {
            return true;
        }
        if (e instanceof ThrownPotion potion) {
            PotionContents contents = potion.getItem().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            for (MobEffectInstance effect : contents.getAllEffects()) {
                if (effect.getEffect().is(WEREWOLF_BOUND)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void onImpact(ProjectileImpactEvent event) {
        Projectile p = event.getProjectile();
        if (p.level().isClientSide || !isBrewProjectile(p)) {
            return;
        }
        AABB box = p.getBoundingBox().inflate(SPLASH_X, SPLASH_Y, SPLASH_X);
        for (BurnedWitch witch : p.level().getEntitiesOfClass(BurnedWitch.class, box)) {
            witch.brewShieldTime = p.level().getGameTime();
        }
    }

    private static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof BurnedWitch witch && witch.brewShielded()) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    private static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof BurnedWitch witch && witch.brewShielded()
                && isBrewProjectile(event.getSource().getDirectEntity())) {
            event.setCanceled(true);
        }
    }
}
