package dev.movingcastle.thaumicconcilium.item;

import com.leclowndu93150.thaumaturge.registry.TTMobEffects;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.effect.Bleeding;
import dev.movingcastle.thaumicconcilium.integration.neovitae.NeoVitaeCut;
import dev.movingcastle.thaumicconcilium.registry.TCEffects;
import dev.movingcastle.thaumicconcilium.spell.ServerTasks;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import dev.movingcastle.thaumicconcilium.network.DaggerFxPayload;
import dev.movingcastle.thaumicconcilium.network.DaggerStabPayload;

/**
 * The Crimson Dagger (ours; user design 2026-11-21): weak and fast, the Crimson Initiation tool.
 *
 * <ul>
 *   <li><b>Right-click, held:</b> the raise. After {@link #RAISE_TICKS} it is raised; while it stays raised and the player has
 *       Thaumaturge's Death Gaze, crimson cultists within {@link #GAZE_RANGE} blocks in line of sight are stunned (tag
 *       {@link #STUN_TARGETS}) or slowed (tag {@link #SLOW_TARGETS}); bosses are in neither. The gaze's wither is untouched.</li>
 *   <li><b>Left-click after the raise</b> (Minecraft swallows attacks while an item is in use, so the raise is released
 *       first): the stab applies Bleed for {@link #STAB_WINDOW_TICKS} after the release (see {@link Bleeding}).</li>
 *   <li><b>Sneak + right-click:</b> the self-cut, LP into the nearest Ara Vitae (Neo Vitae only, {@link NeoVitaeCut}).</li>
 *   <li>With Enchanted loaded the stab and bleed use {@code enchanted:sacrifice} damage.</li>
 * </ul>
 */
public class CrimsonDagger extends Item {
    /** PROVISIONAL (a branch decision): ticks of holding right-click before the dagger counts as raised. Proposal 15 (0.75 s). */
    public static final int RAISE_TICKS = 15;
    /** PROVISIONAL: ticks after releasing a completed raise in which a stab applies Bleed. */
    public static final int STAB_WINDOW_TICKS = 60;
    /** PROVISIONAL: when false every stab bleeds. */
    public static final boolean STAB_REQUIRES_RAISE = true;
    /** User decision: the gaze's reach for the stun. */
    public static final double GAZE_RANGE = 8.0;
    /** Ticks between gaze scans while raised. */
    public static final int GAZE_INTERVAL = 5;
    /** How long each scan's stun or slow lasts (three scans' worth, so one missed scan does not free them). */
    public static final int GAZE_EFFECT_TICKS = 15;
    /** Slowness II for the Paladins, Rangers and captains. */
    public static final int SLOW_AMPLIFIER = 1;
    public static final int USE_DURATION = 72000;

    /** Sub-decision (memo): a stab while raised spends the charge, so the raise (and the stun) recharges from nothing. */
    public static final boolean STAB_SPENDS_CHARGE = true;
    /**
     * Decision D9-b: true = a left click while raised stabs at once (the client polls the attack key and sends a
     * {@link DaggerStabPayload}); the trunk's release-then-stab path ({@link #STAB_WINDOW_TICKS}) works either way.
     * false = the trunk's D9-a behaviour only.
     */
    public static final boolean STAB_WHILE_RAISED = true;
    /** Animation clock lengths, shared with the client ({@code DaggerAnim}). */
    public static final int STAB_TICKS = 6;
    public static final int CUT_TICKS = 16;
    public static final int LOWER_TICKS = 4;
    /**
     * The tick of the cut animation on which the blade meets the palm: the health and the LP are paid then
     * ({@link #cutAtMark}), and the harness marker {@code dagger_cut} is stamped.
     */
    public static final int CUT_MARK_TICK = 8;
    private static final String CUT_PAID_KEY = "thaumicconcilium_dagger_cut_paid";
    private static final String SPENT_KEY = "thaumicconcilium_dagger_spent";

    /** Rank and file: Thaumaturge Knight and Cleric, our Archer. */
    public static final TagKey<EntityType<?>> STUN_TARGETS = TagKey.create(Registries.ENTITY_TYPE, ThaumicConcilium.id("dagger_stun"));
    /** Paladins, Rangers and captains. */
    public static final TagKey<EntityType<?>> SLOW_TARGETS = TagKey.create(Registries.ENTITY_TYPE, ThaumicConcilium.id("dagger_slow"));
    /** Every crimson cultist, bosses included (the Crimson Blood drop). */
    public static final TagKey<EntityType<?>> CRIMSON_CULTISTS = TagKey.create(Registries.ENTITY_TYPE, ThaumicConcilium.id("crimson_cultists"));

    /** Attack damage 3 (player base 1 + 2) and speed 2.6: a vanilla sword's 1.6, plus 1. */
    public static final double ATTACK_DAMAGE_BONUS = 2.0;
    public static final double ATTACK_SPEED_MODIFIER = -1.4;

    private static final String PRIMED_KEY = "thaumicconcilium_dagger_primed_until";

    public CrimsonDagger(Properties properties) {
        super(properties);
    }

    /** Unbreakable, one per stack, the attack damage and speed above (the same attribute slots a vanilla sword fills). */
    public static Properties properties() {
        return new Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)
                .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                        .add(Attributes.ATTACK_DAMAGE, new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_DAMAGE_ID,
                                ATTACK_DAMAGE_BONUS, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ATTACK_SPEED, new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_SPEED_ID,
                                ATTACK_SPEED_MODIFIER, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                        .build());
    }

    // ---------------------------------------------------------------- raise and gaze
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                fx(sp, DaggerFxPayload.CUT, hand == InteractionHand.MAIN_HAND);
                if (!ModList.get().isLoaded("neovitae")) {
                    sp.displayClientMessage(Component.translatable("message.thaumicconcilium.dagger_no_vitae"), true);
                } else if (NeoVitaeCut.inReach(sp)) {
                    // The cooldown starts with the motion; the health and the LP change hands when the blade meets the palm.
                    player.getCooldowns().addCooldown(this, NeoVitaeCut.COOLDOWN_TICKS);
                    cutAtMark(sp);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        player.startUsingItem(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            sp.getPersistentData().putInt(SPENT_KEY, 0);
            fx(sp, DaggerFxPayload.RAISE, hand == InteractionHand.MAIN_HAND);
        }
        return InteractionResultHolder.consume(stack);
    }

    /**
     * The self-cut is paid {@link #CUT_MARK_TICK} ticks after it starts, when the animation's blade meets the palm (one
     * queued task, {@link ServerTasks}; nothing ticks while no cut is under way). Dropped if the player died, left, or put
     * the dagger away in between. Only called with Neo Vitae loaded.
     */
    private static void cutAtMark(ServerPlayer player) {
        long due = player.level().getGameTime() + CUT_MARK_TICK;
        ServerTasks.add(() -> {
            if (player.isRemoved() || !player.isAlive()) {
                return false;
            }
            long now = player.level().getGameTime();
            if (now < due) {
                return true;
            }
            if ((player.getMainHandItem().getItem() instanceof CrimsonDagger || player.getOffhandItem().getItem() instanceof CrimsonDagger)
                    && NeoVitaeCut.cut(player)) {
                player.getPersistentData().putLong(CUT_PAID_KEY, now);
            }
            return false;
        });
    }

    /** The game time at which this player's last self-cut was paid into an altar; 0 when none was (kept for tests). */
    public static long cutPaidAt(LivingEntity entity) {
        return entity.getPersistentData().getLong(CUT_PAID_KEY);
    }

    private static void fx(ServerPlayer player, byte mode, boolean mainHand) {
        if (player instanceof net.neoforged.neoforge.common.util.FakePlayer) {
            return;
        }
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new DaggerFxPayload(player.getId(), mode, mainHand));
    }

    /** Ticks of the current raise, with a spent charge (see {@link #stab}) taken off. */
    private static int charged(LivingEntity entity, int usedTicks) {
        return usedTicks - entity.getPersistentData().getInt(SPENT_KEY);
    }

    /** True when the player is raised right now (a completed raise, not spent). */
    public static boolean raised(LivingEntity entity, int usedTicks) {
        return charged(entity, usedTicks) >= RAISE_TICKS;
    }

    /**
     * The stab while raised ({@link #STAB_WHILE_RAISED}, sent by the client when the attack key goes down): hits what the
     * player is looking at within reach, bleeds it, and spends the charge ({@link #STAB_SPENDS_CHARGE}). Returns false when
     * the player is not raised. A stab at empty air still plays and still spends the charge.
     */
    public static boolean stab(ServerPlayer player, int usedTicks) {
        if (!STAB_WHILE_RAISED || !raised(player, usedTicks)) {
            return false;
        }
        Entity target = lookTarget(player);
        if (target != null) {
            // The raise is complete, so this hit bleeds: the window is open for this tick only.
            player.getPersistentData().putLong(PRIMED_KEY, player.level().getGameTime());
            player.attack(target);
            player.getPersistentData().putLong(PRIMED_KEY, -1L);
        } else {
            player.resetAttackStrengthTicker();
        }
        if (STAB_SPENDS_CHARGE) {
            // The arm recovers for STAB_TICKS, then the raise starts again from nothing.
            player.getPersistentData().putInt(SPENT_KEY, usedTicks + STAB_TICKS);
        }
        fx(player, DaggerFxPayload.STAB, player.getUsedItemHand() == InteractionHand.MAIN_HAND);
        return true;
    }

    /** The server's handling of a {@link DaggerStabPayload}. */
    public static void stabFromClient(ServerPlayer player) {
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof CrimsonDagger) {
            stab(player, player.getTicksUsingItem());
        }
    }

    private static Entity lookTarget(ServerPlayer player) {
        double reach = player.entityInteractionRange();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(reach));
        double block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
                .getLocation().distanceToSqr(eye);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end,
                player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0),
                e -> !e.isSpectator() && e.isPickable() && e.isAttackable(), reach * reach);
        return hit != null && hit.getLocation().distanceToSqr(eye) <= block ? hit.getEntity() : null;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }
        int elapsed = charged(player, USE_DURATION - remaining);
        if (elapsed == RAISE_TICKS) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6F, 1.6F);
        }
        if (elapsed >= RAISE_TICKS && (elapsed - RAISE_TICKS) % GAZE_INTERVAL == 0) {
            gaze(player);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (!level.isClientSide()) {
            if (raised(entity, USE_DURATION - remaining)) {
                entity.getPersistentData().putLong(PRIMED_KEY, level.getGameTime() + STAB_WINDOW_TICKS);
            }
            entity.getPersistentData().putInt(SPENT_KEY, 0);
            if (entity instanceof ServerPlayer player) {
                fx(player, DaggerFxPayload.END, entity.getUsedItemHand() == InteractionHand.MAIN_HAND);
            }
        }
    }

    /**
     * One gaze scan: with Death Gaze, stun or slow the crimson cultists in reach and in sight. Returns how many it touched.
     * Runs every {@link #GAZE_INTERVAL} ticks only while the dagger is raised.
     */
    public static int gaze(ServerPlayer player) {
        if (!player.hasEffect(TTMobEffects.DEATH_GAZE)) {
            return 0;
        }
        ServerLevel level = player.serverLevel();
        double reach = GAZE_RANGE * GAZE_RANGE;
        int touched = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(GAZE_RANGE),
                e -> e != player && e.isAlive() && (e.getType().is(STUN_TARGETS) || e.getType().is(SLOW_TARGETS)))) {
            if (target.distanceToSqr(player) > reach || !player.hasLineOfSight(target)) {
                continue;
            }
            if (target.getType().is(STUN_TARGETS)) {
                target.addEffect(new MobEffectInstance(TCEffects.STUNNED, GAZE_EFFECT_TICKS, 0, false, true, true));
            } else {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, GAZE_EFFECT_TICKS, SLOW_AMPLIFIER, false, true, true));
            }
            touched++;
        }
        return touched;
    }

    // ---------------------------------------------------------------- the stab
    public static boolean primed(LivingEntity entity) {
        return entity.getPersistentData().getLong(PRIMED_KEY) >= entity.level().getGameTime();
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof ServerPlayer player && (!STAB_REQUIRES_RAISE || primed(player))) {
            Bleeding.apply(target, player);
        }
        return true;
    }

    /**
     * With Enchanted loaded the hit is its own: the attack is taken over (return true) and dealt as {@code enchanted:sacrifice}
     * damage: attack damage scaled by the cooldown as vanilla does, a little knockback, no sweep or critical. Without it the
     * vanilla attack runs and {@link #hurtEnemy} adds the bleed.
     */
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        if (!ModList.get().isLoaded("enchanted")) {
            return false;
        }
        if (player.level() instanceof ServerLevel level && target instanceof LivingEntity living && living.isAttackable()
                && !living.skipAttackInteraction(player)) {
            float scale = player.getAttackStrengthScale(0.5F);
            float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * (0.2F + scale * scale * 0.8F);
            DamageSource source = Bleeding.source(level, player);
            if (living.hurt(source, damage)) {
                living.knockback(0.4F * scale, Mth.sin(player.getYRot() * Mth.DEG_TO_RAD), -Mth.cos(player.getYRot() * Mth.DEG_TO_RAD));
                this.hurtEnemy(stack, living, player);
            }
        }
        player.resetAttackStrengthTicker();
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(Component.translatable("thaumicconcilium.tooltip.crimson_dagger").withStyle(ChatFormatting.DARK_RED));
    }
}
