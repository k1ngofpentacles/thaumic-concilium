package dev.movingcastle.thaumicconcilium.compat.witchery;

import dev.movingcastle.thaumicconcilium.world.BurningSiteElement;
import dev.sterner.witchery.content.entity.goal.HuntSupernaturalTargetGoal;
import dev.sterner.witchery.feature.transformation.TransformationHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Phase 10-4 (R11 A): the Burning Site's Witch Hunter keeps the remake's crossbow, bear traps and looks, but hunts by the original's rule
 * ({@code EntityWitchHunter.func_82704_a}, meaning only): undead, demonic creatures, witches and werewolves; a player only if a vampire, a
 * shifted werewolf, or carrying a voodoo or vampiric poppet (original {@code CreatureUtil.isWitch}; the targetPlayerName clause had no
 * setter in the original's site and is left out; the remake has no Beast infusion). The remake's own hunters are untouched: only an entity
 * with {@link BurningSiteElement#HUNTER_TAG} is changed. Names remake classes: loaded only behind {@link WitcheryGate}.
 */
public final class SiteHunter {
    /** PROVISIONAL(R11): true = option A (swap the hunt goal). Option B (the remake's hunter unchanged) = false. */
    public static final boolean SWAP_HUNT_GOAL = true;
    /** The goal priority the remake's hunt goal sits at (HunterEntity.registerGoals, targetSelector). */
    public static final int PRIORITY = 2;

    private static final TagKey<EntityType<?>> DEMONIC = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("witchery", "demonic_entity"));
    private static final TagKey<EntityType<?>> SUPERNATURAL_PREY = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("witchery", "supernatural_prey"));
    private static final ResourceLocation VOODOO_POPPET = ResourceLocation.fromNamespaceAndPath("witchery", "voodoo_poppet");
    private static final ResourceLocation VAMPIRIC_POPPET = ResourceLocation.fromNamespaceAndPath("witchery", "vampiric_poppet");

    private SiteHunter() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(SiteHunter::onJoin);
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (!SWAP_HUNT_GOAL || event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)
                || !mob.getTags().contains(BurningSiteElement.HUNTER_TAG)) {
            return;
        }
        swap(mob);
    }

    /** Replaces the remake's hunt goal with the original's rule. Returns false if the mob had no remake hunt goal (nothing changed). */
    public static boolean swap(Mob mob) {
        boolean[] had = {false};
        mob.targetSelector.removeAllGoals(g -> {
            if (g instanceof HuntSupernaturalTargetGoal<?>) {
                had[0] = true;
                return true;
            }
            return false;
        });
        if (!had[0]) {
            return false;
        }
        // same arguments as the remake's goal (random interval 10, must see, not must reach), only the predicate differs
        mob.targetSelector.addGoal(PRIORITY, new NearestAttackableTargetGoal<>(mob, LivingEntity.class, 10, true, false, SiteHunter::isTarget));
        return true;
    }

    /** The original's rule (meaning only). */
    public static boolean isTarget(LivingEntity e) {
        if (e instanceof Player player) {
            return vampireOrWerewolf(player) || carriesPoppet(player);
        }
        EntityType<?> type = e.getType();
        return type.is(EntityTypeTags.UNDEAD) || type.is(DEMONIC) || type.is(SUPERNATURAL_PREY)
                // original CreatureUtil.isDemonic also lists these vanilla mobs (the remake's tag has only its own three)
                || e instanceof Ghast || e instanceof Blaze || e instanceof MagmaCube
                || e instanceof Witch;
    }

    private static boolean vampireOrWerewolf(Player player) {
        return WitchAfflictions.vampire(player) || TransformationHandler.isWerewolf(player);
    }

    private static boolean carriesPoppet(Player player) {
        Item voodoo = BuiltInRegistries.ITEM.get(VOODOO_POPPET);
        Item vampiric = BuiltInRegistries.ITEM.get(VAMPIRIC_POPPET);
        Inventory inv = player.getInventory();
        for (java.util.List<ItemStack> list : java.util.List.of(inv.items, inv.armor, inv.offhand)) {
            for (ItemStack s : list) {
                if (!s.isEmpty() && (s.is(voodoo) || s.is(vampiric))) {
                    return true;
                }
            }
        }
        return false;
    }
}
