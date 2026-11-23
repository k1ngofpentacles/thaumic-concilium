package dev.movingcastle.thaumicconcilium.golem;

import com.leclowndu93150.thaumaturge.api.casters.FocusCaster;
import com.leclowndu93150.thaumaturge.api.spell.Spell;
import com.leclowndu93150.thaumaturge.api.spell.SpellNode;
import com.leclowndu93150.thaumaturge.api.spell.Spells;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.GolemBydlo;
import dev.movingcastle.thaumicconcilium.spell.SpellTrees;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * The Golem Bydlo's wand commands (phase 8; original {@code GolemBydlo.livingUpdate}). While a player rides a Bydlo and holds a wand
 * with a focus, a left click is a command; it costs nothing (no vis, no wear: as the original, user decision) and has a
 * {@link #COOLDOWN} tick cooldown kept as a game-time stamp, so nothing ticks. Event driven: a click on a block, a punch at a
 * creature, and a click on nothing (the client sends {@code BydloSwingPayload}).
 * <ul>
 *   <li><b>Excavation</b> (the focus has Thaumaturge's {@code break} part): the 5x5 plane of blocks around the clicked one, on the
 *   face's plane, is broken as if by a diamond-grade multi-tool; the focus's {@code fortune} level and {@code silk_touch} part apply.
 *   <b>Every block fires a {@link BlockEvent.BreakEvent}</b> and respects {@code mayInteract} (the original fired none: fix).</li>
 *   <li><b>Wrath Attraction</b> (the focus has our {@code wrath_attraction} part): every living thing near the golem (box inflated by
 *   2, 1, 2) is punched for {@link GolemBydlo#punchDamage}, knocked up 0.4. Not the rider, players, tamed pets or the rider's allies
 *   (the original hit all: fix).</li>
 * </ul>
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class BydloCommands {
    public static final int COOLDOWN = 6;
    public static final double REACH = 5.0;
    public static final int RADIUS = 2;
    private BydloCommands() {
    }

    // ---------------------------------------------------------------- what the held focus says
    /** The wand focus in the rider's main hand (empty when none). */
    public static ItemStack focusOf(Player rider) {
        ItemStack held = rider.getMainHandItem();
        return held.getItem() instanceof FocusCaster caster ? caster.socketedFocus(held) : ItemStack.EMPTY;
    }

    public static boolean excavates(Spell spell) {
        return SpellTrees.contains(spell, ResourceKey.create(com.leclowndu93150.thaumaturge.api.spell.part.SpellPart.REGISTRY_KEY,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumaturge", "break")));
    }

    public static boolean wrathful(Spell spell) {
        return SpellTrees.contains(spell, SpellTrees.part("wrath_attraction"));
    }

    private static boolean has(Spell spell, String path) {
        return SpellTrees.contains(spell, ResourceKey.create(com.leclowndu93150.thaumaturge.api.spell.part.SpellPart.REGISTRY_KEY,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumaturge", path)));
    }

    /** The highest {@code level} setting of a {@code fortune} part in the spell, 0 for none (the part's default level is 1). */
    public static int fortuneLevel(Spell spell) {
        return spell == null ? 0 : fortune(spell.root());
    }

    private static int fortune(SpellNode node) {
        int best = node.part().location().equals(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumaturge", "fortune"))
                ? node.settings().getOrDefault("level", 1) : 0;
        for (SpellNode child : node.children()) {
            best = Math.max(best, fortune(child));
        }
        return best;
    }

    public static boolean silkTouch(Spell spell) {
        return spell != null && has(spell, "silk_touch");
    }

    // ---------------------------------------------------------------- events
    @SubscribeEvent
    public static void onClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.getVehicle() instanceof GolemBydlo golem)) {
            return;
        }
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }
        Spell spell = Spells.spellOf(focusOf(player));
        if (spell == null) {
            return;
        }
        if (command(player, golem, spell, event.getPos(), event.getFace())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.getVehicle() instanceof GolemBydlo golem)) {
            return;
        }
        Spell spell = Spells.spellOf(focusOf(player));
        if (spell != null) {
            command(player, golem, spell, null, null);
        }
    }

    /** The click on nothing (client packet): the wrath command. */
    public static void onSwingAtAir(ServerPlayer player) {
        if (player.getVehicle() instanceof GolemBydlo golem) {
            Spell spell = Spells.spellOf(focusOf(player));
            if (spell != null) {
                command(player, golem, spell, null, null);
            }
        }
    }

    // ---------------------------------------------------------------- the commands
    /**
     * Runs the commands the focus asks for, once per {@link #COOLDOWN} ticks. {@code block} is the clicked block or null.
     * Returns whether a command ran.
     */
    public static boolean command(ServerPlayer player, GolemBydlo golem, Spell spell, BlockPos block, Direction face) {
        ServerLevel level = player.serverLevel();
        boolean dig = block != null && face != null && excavates(spell);
        boolean wrath = wrathful(spell);
        if (!dig && !wrath) {
            return false;
        }
        long now = level.getGameTime();
        if (now - golem.lastCommand < COOLDOWN) {
            return true;   // inside the cooldown: swallow the click, do nothing
        }
        golem.lastCommand = now;
        golem.raiseArms();
        level.playSound(null, golem.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        if (dig) {
            excavate(level, player, block, face, fortuneLevel(spell), silkTouch(spell));
        }
        if (wrath) {
            punchAround(level, player, golem);
        }
        return true;
    }

    /** Whether a punch may hit this creature. */
    public static boolean fair(Player rider, GolemBydlo golem, LivingEntity target) {
        return target != golem && target != rider && target.isAlive() && !(target instanceof Player)
                && !(target instanceof TamableAnimal pet && pet.isTame()) && !target.isAlliedTo(rider)
                && !target.isPassengerOfSameVehicle(rider);
    }

    /** The punch on everything fair in the golem's box inflated by (2, 1, 2). Returns the creatures hit. */
    public static List<LivingEntity> punchAround(ServerLevel level, Player rider, GolemBydlo golem) {
        AABB box = golem.getBoundingBox().inflate(2.0, 1.0, 2.0);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box, t -> fair(rider, golem, t));
        DamageSource source = level.damageSources().mobAttack(golem);
        for (LivingEntity target : targets) {
            if (target.hurt(source, GolemBydlo.punchDamage(golem.getRandom()))) {
                target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.4, 0.0));
                target.hurtMarked = true;
            }
        }
        return targets;
    }

    /** The multi-tool of the original's Primal Crusher: mines anything a pickaxe, axe, shovel or hoe mines. */
    private static boolean mineable(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_AXE)
                || state.is(BlockTags.MINEABLE_WITH_SHOVEL) || state.is(BlockTags.MINEABLE_WITH_HOE);
    }

    /** The 5x5 plane perpendicular to the clicked face, centred on the clicked block (offsets -2..2 on the two other axes). */
    public static List<BlockPos> plane(BlockPos centre, Direction face) {
        java.util.ArrayList<BlockPos> cells = new java.util.ArrayList<>(25);
        Direction.Axis axis = face.getAxis();
        for (int a = -RADIUS; a <= RADIUS; a++) {
            for (int b = -RADIUS; b <= RADIUS; b++) {
                cells.add(switch (axis) {
                    case X -> centre.offset(0, a, b);
                    case Y -> centre.offset(a, 0, b);
                    case Z -> centre.offset(a, b, 0);
                });
            }
        }
        return cells;
    }

    /** Breaks the plane. Returns the number of blocks broken. */
    public static int excavate(ServerLevel level, ServerPlayer player, BlockPos centre, Direction face, int fortune, boolean silk) {
        BlockState clicked = level.getBlockState(centre);
        if (clicked.isAir() || !mineable(clicked)) {
            return 0;   // original: the crusher had to be effective on the clicked block
        }
        ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        if (silk) {
            tool.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
        } else if (fortune > 0) {
            tool.enchant(enchantments.getOrThrow(Enchantments.FORTUNE), fortune);
        }
        int broken = 0;
        for (BlockPos pos : plane(centre, face)) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F || !mineable(state) || !level.mayInteract(player, pos)) {
                continue;
            }
            BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, player);
            if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
                continue;
            }
            Block.dropResources(state, level, pos, level.getBlockEntity(pos), player, tool);
            level.destroyBlock(pos, false, player);
            broken++;
        }
        return broken;
    }
}
