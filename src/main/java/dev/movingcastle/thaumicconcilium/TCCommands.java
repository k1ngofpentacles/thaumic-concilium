package dev.movingcastle.thaumicconcilium;

import com.leclowndu93150.thaumaturge.api.casters.FocusCaster;
import com.leclowndu93150.thaumaturge.content.research.ResearchGrants;
import com.leclowndu93150.thaumaturge.content.research.pool.AspectPools;
import com.leclowndu93150.thaumaturge.content.wands.ItemWand;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.movingcastle.thaumicconcilium.spell.FocusPresets;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * QA commands (op level 2), used by the generated FTB Quests QA chapters' rewards to hand out test kits
 * ({@code tools/qa-quests}):
 * <ul>
 *   <li>{@code /thaumicconcilium qa research}: all research and 100 of every aspect;</li>
 *   <li>{@code /thaumicconcilium qa focus <preset>}: a ready-inscribed focus ({@link FocusPresets});</li>
 *   <li>{@code /thaumicconcilium qa caster <item> <preset|none>}: a wand or bracelet with that focus socketed and its
 *   vis full.</li>
 *   <li>{@code /thaumicconcilium qa lithograph}: lithographed, watching yourself (the HUD without the bed).</li>
 * </ul>
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class TCCommands {
    private TCCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(ThaumicConcilium.MODID).requires(s -> s.hasPermission(2))
                .then(Commands.literal("qa")
                        .then(Commands.literal("research").executes(TCCommands::research))
                        // one-click staging of a QA check (world/QaStaging, tools/qa-quests): a fresh far-away site, the kit, the build
                        .then(Commands.literal("stage")
                                .then(Commands.argument("key", StringArgumentType.word())
                                        .executes(c -> dev.movingcastle.thaumicconcilium.world.QaStaging.stage(c.getSource(), c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "key")))))
                        .then(Commands.literal("wear").executes(c -> dev.movingcastle.thaumicconcilium.world.QaStaging.wear(c.getSource(), c.getSource().getPlayerOrException())))
                        .then(Commands.literal("jars")
                                .then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
                                        .executes(c -> dev.movingcastle.thaumicconcilium.world.QaStaging.jars(c.getSource().getLevel(),
                                                net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(c, "pos")))))
                        .then(Commands.literal("verdict")
                                .then(Commands.argument("key", StringArgumentType.word())
                                        .then(Commands.literal("pass").executes(c -> verdict(c, "pass")))
                                        .then(Commands.literal("fail").executes(c -> verdict(c, "fail")))
                                        .then(Commands.literal("skip").executes(c -> verdict(c, "skip")))))
                        .then(Commands.literal("focus")
                                .then(Commands.argument("preset", StringArgumentType.word()).suggests(TCCommands::presets)
                                        .executes(TCCommands::focus)))
                        .then(Commands.literal("caster")
                                .then(Commands.argument("item", ResourceLocationArgument.id())
                                        .then(Commands.argument("preset", StringArgumentType.word()).suggests(TCCommands::presets)
                                                .executes(TCCommands::caster))))
                        // Phase 6a test scaffolding: a ready hex (full needs bedrock, which survival can't place).
                        .then(Commands.literal("hex")
                                .then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
                                        .then(Commands.literal("full").executes(c -> hex(c, false, false)))
                                        .then(Commands.literal("chained").executes(c -> hex(c, false, true)))
                                        .then(Commands.literal("lesser").executes(c -> hex(c, true, false)))
                                        // Phase 5c: a full hex holding a tamed rift (the sink: feed it, watch the cracks, let it break).
                                        .then(Commands.literal("tamed").executes(TCCommands::hexTamed))))
                        // Phase 5c: a Vessel with its four anchors, dormant or quiescent (charge 20, ready to drain), you as its contributor.
                        .then(Commands.literal("vessel")
                                .then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
                                        .then(Commands.literal("dormant").executes(c -> vessel(c, false)))
                                        .then(Commands.literal("quiescent").executes(c -> vessel(c, true)))
                                        // cycle 3c: the break sequence here (a hex at pos breaks; with none, the stand-in plays)
                                        .then(Commands.literal("breaking").executes(c -> vesselBreak(c, false)))
                                        .then(Commands.literal("breaking_standin").executes(c -> vesselBreak(c, true)))))
                        // Phase 5a: a Lesser Crimson Portal (the rift route that sets the boss flag needs the eldritch object, a later cycle).
                        .then(Commands.literal("portal")
                                .then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.Vec3Argument.vec3())
                                        .executes(c -> portal(c, false))
                                        .then(Commands.literal("boss").executes(c -> portal(c, true)))))
                        .then(Commands.literal("rift")
                                .then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
                                        .then(Commands.argument("size", com.mojang.brigadier.arguments.IntegerArgumentType.integer(5, 100))
                                                .executes(TCCommands::rift))))
                        .then(Commands.literal("crystal")
                                .then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
                                        .then(Commands.argument("aspect", ResourceLocationArgument.id())
                                                .then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 1000))
                                                        .executes(TCCommands::crystal)))))
                        // Phase 6b: lithograph the player, watching themselves (the HUD without the bed operation).
                        // Phase 5d (render harness): a crater and a painted grove at your feet, as the real ending starts one (tier 3 or 4, radius in blocks).
                        .then(Commands.literal("grove")
                                .then(Commands.argument("tier", com.mojang.brigadier.arguments.IntegerArgumentType.integer(3, 4))
                                        .then(Commands.argument("radius", com.mojang.brigadier.arguments.IntegerArgumentType.integer(4, 128))
                                                .executes(TCCommands::grove))))
                        // Phase 5d cycle 2d (render harness): one landmark tree variant on the soil block at pos (species greatwood or silverwood, variant 0-3, facing 0-3).
                        .then(Commands.literal("landmark")
                                .then(Commands.argument("silver", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                                        .then(Commands.argument("variant", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 9))
                                                .then(Commands.argument("facing", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 3))
                                                        .then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
                                                                .executes(TCCommands::landmark))))))
                        // Phase 5d, the teaser: a finished-rite staging at your feet that runs the real ending (record it yourself).
                        .then(Commands.literal("finale")
                                .then(Commands.argument("tier", com.mojang.brigadier.arguments.IntegerArgumentType.integer(3, 4))
                                        .executes(TCCommands::finale)))
                        .then(Commands.literal("lithograph").executes(TCCommands::lithograph))
                        // Phase 7 (render harness, QA): a Valet seal on top of the block at pos, you as its owner, its phrase item named "open sesame".
                        .then(Commands.literal("valet")
                                .then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
                                        .executes(TCCommands::valet)))
                        // Shield foci look (render harness, QA): arm <charges> | release | aegis_block | clear. See Riposte.qa.
                        .then(Commands.literal("guard")
                                .then(Commands.literal("arm")
                                        .then(Commands.argument("charges", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 10))
                                                .executes(c -> guard(c, "arm", com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "charges")))))
                                .then(Commands.literal("release").executes(c -> guard(c, "release", 0)))
                                .then(Commands.literal("aegis_block").executes(c -> guard(c, "aegis_block", 0)))
                                .then(Commands.literal("clear").executes(c -> guard(c, "clear", 0))))
                        // Phase 5d cycle 4b (render harness): a Taint Seed at pos and the grove killing it, without a painter job.
                        .then(Commands.literal("seeddeath")
                                .then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
                                        .executes(TCCommands::seedDeath)))
                        // Phase 5b: the crimson state (flesh amount, the ethereal state).
                        .then(Commands.literal("flesh")
                                .then(Commands.argument("n", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 200))
                                        .executes(c -> flesh(c, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "n")))))
                        .then(Commands.literal("ethereal")
                                .then(Commands.literal("on").executes(c -> ethereal(c, true)))
                                .then(Commands.literal("off").executes(c -> ethereal(c, false))))));
    }

    private static int flesh(CommandContext<CommandSourceStack> ctx, int n) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        var s = dev.movingcastle.thaumicconcilium.world.CrimsonState.get(player);
        dev.movingcastle.thaumicconcilium.world.CrimsonState.set(player, s.withFlesh(n));
        ctx.getSource().sendSuccess(() -> Component.literal("QA: flesh " + n), false);
        return 1;
    }

    private static int ethereal(CommandContext<CommandSourceStack> ctx, boolean on) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        var s = dev.movingcastle.thaumicconcilium.world.CrimsonState.get(player);
        dev.movingcastle.thaumicconcilium.world.CrimsonState.set(player, s.withEthereal(on));
        ctx.getSource().sendSuccess(() -> Component.literal("QA: ethereal " + on), false);
        return 1;
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> presets(
            CommandContext<CommandSourceStack> ctx, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                java.util.stream.Stream.concat(java.util.stream.Stream.of("none", DRUM_AURA), FocusPresets.ALL.stream().map(FocusPresets.Preset::id)), builder);
    }

    /** QA-only focus (not a preset: no research or lang): Aura > Frost, to see the drum stance's radius and cost. */
    private static final String DRUM_AURA = "qa_drum_aura";

    private static ItemStack drumAura() {
        ItemStack focus = new ItemStack(com.leclowndu93150.thaumaturge.registry.TTItems.FOCUS_3.get());
        ResourceLocation aura = ResourceLocation.fromNamespaceAndPath("thaumaturge", "aura");
        ResourceLocation frost = ResourceLocation.fromNamespaceAndPath("thaumaturge", "frost");
        java.util.function.BiFunction<ResourceLocation, java.util.List<com.leclowndu93150.thaumaturge.api.spell.SpellNode>, com.leclowndu93150.thaumaturge.api.spell.SpellNode> node =
                (part, kids) -> new com.leclowndu93150.thaumaturge.api.spell.SpellNode(
                        net.minecraft.resources.ResourceKey.create(com.leclowndu93150.thaumaturge.api.spell.part.SpellPart.REGISTRY_KEY, part),
                        java.util.Optional.empty(), java.util.Map.of(), kids);
        var root = node.apply(com.leclowndu93150.thaumaturge.api.spell.Spell.ORIGIN.location(),
                java.util.List.of(node.apply(aura, java.util.List.of(node.apply(frost, java.util.List.of())))));
        com.leclowndu93150.thaumaturge.api.spell.Spells.setSpell(focus,
                new com.leclowndu93150.thaumaturge.api.spell.Spell(com.leclowndu93150.thaumaturge.api.spell.CastStyle.INSTANT, root));
        focus.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("QA Aura > Frost"));
        return focus;
    }

    private static ItemStack preset(String id) {
        if (id.equals(DRUM_AURA)) {
            return drumAura();
        }
        return FocusPresets.ALL.stream().filter(p -> p.id().equals(id)).findFirst().map(FocusPresets.Preset::stack).orElse(ItemStack.EMPTY);
    }

    private static int guard(CommandContext<CommandSourceStack> ctx, String what, int n) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        dev.movingcastle.thaumicconcilium.spell.Riposte.qa(ctx.getSource().getPlayerOrException(), what, n);
        ctx.getSource().sendSuccess(() -> Component.literal("QA: guard " + what), false);
        return 1;
    }

    private static int lithograph(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        dev.movingcastle.thaumicconcilium.lithograph.Lithograph.set(player, new dev.movingcastle.thaumicconcilium.lithograph.Lithograph.State(
                true, false, java.util.List.of(dev.movingcastle.thaumicconcilium.lithograph.Lithograph.playerTarget(player.getUUID()), "", "", "")));
        ctx.getSource().sendSuccess(() -> Component.literal("QA: lithographed, watching yourself"), false);
        return 1;
    }

    private static int valet(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        net.minecraft.core.BlockPos pos = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        net.minecraft.server.level.ServerLevel level = ctx.getSource().getLevel();
        var at = new com.leclowndu93150.thaumaturge.api.golems.seals.SealPos(pos, net.minecraft.core.Direction.UP);
        if (!com.leclowndu93150.thaumaturge.content.golem.seals.SealHandler.place(level, at, ThaumicConcilium.id("valet"),
                dev.movingcastle.thaumicconcilium.registry.TCSeals.VALET.get(), player)) {
            ctx.getSource().sendFailure(Component.literal("QA: a seal is already there"));
            return 0;
        }
        var seal = com.leclowndu93150.thaumaturge.content.golem.seals.SealHandler.getSealEntity(level, at);
        net.minecraft.world.item.ItemStack phrase = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER);
        phrase.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("open sesame"));
        seal.filter().orElseThrow().setStack(dev.movingcastle.thaumicconcilium.golem.ValetBehavior.PHRASE_SLOT, phrase);
        seal.markChanged(level);
        ctx.getSource().sendSuccess(() -> Component.literal("QA: Valet seal placed, phrase 'open sesame'"), false);
        return 1;
    }

    private static int research(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ResearchGrants.grantAll(player);
        AspectPools.grantAllForCommand(player, 100);
        ctx.getSource().sendSuccess(() -> Component.literal("QA: all research and aspects granted"), false);
        return 1;
    }

    private static int portal(CommandContext<CommandSourceStack> ctx, boolean boss) {
        net.minecraft.world.phys.Vec3 pos = net.minecraft.commands.arguments.coordinates.Vec3Argument.getVec3(ctx, "pos");
        return dev.movingcastle.thaumicconcilium.entity.mob.LesserCrimsonPortal.summon(ctx.getSource().getLevel(), pos, boss) != null ? 1 : 0;
    }

    /** Builds the ring and centre at {@code pos} and forms the hex; {@code chained} also opens its chained rift. */
    private static int hex(CommandContext<CommandSourceStack> ctx, boolean lesser, boolean chained) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        net.minecraft.core.BlockPos pos = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        net.minecraft.server.level.ServerLevel level = ctx.getSource().getLevel();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(pos.offset(dx, 0, dz), dx == 0 && dz == 0
                        ? (lesser ? com.leclowndu93150.thaumaturge.registry.TTBlocks.PAVING_STONE_BARRIER.get() : net.minecraft.world.level.block.Blocks.BEDROCK).defaultBlockState()
                        : com.leclowndu93150.thaumaturge.registry.TTBlocks.STONE_ANCIENT.get().defaultBlockState(), 3);
            }
        }
        dev.movingcastle.thaumicconcilium.rift.Hexes.form(level, pos, lesser);
        if (chained) {
            level.setBlock(pos.above(), com.leclowndu93150.thaumaturge.registry.TTBlocks.JAR_NODE.get().defaultBlockState(), 3);
            dev.movingcastle.thaumicconcilium.rift.Hexes.chain(level, pos.above(), pos);
        }
        return 1;
    }

    private static int hexTamed(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        hex(ctx, true, false); // only a lesser hex is a sink
        net.minecraft.core.BlockPos pos = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        net.minecraft.server.level.ServerLevel level = ctx.getSource().getLevel();
        if (!(level.getBlockEntity(pos) instanceof dev.movingcastle.thaumicconcilium.block.HexBlockEntity hex)) {
            return 0;
        }
        hex.setHasRift(true);
        hex.setSinkActive(true);
        hex.rollSpreads(level.random);
        var rift = com.leclowndu93150.thaumaturge.registry.TTEntities.FLUX_RIFT.get().create(level);
        rift.reseed(level.random.nextInt());
        rift.moveTo(pos.getX() + 0.5, pos.getY() + 3.5, pos.getZ() + 0.5, 0.0F, 0.0F);
        rift.resize(25);
        level.addFreshEntity(rift);
        dev.movingcastle.thaumicconcilium.rift.ConciliumRifts.mark(rift, dev.movingcastle.thaumicconcilium.rift.ConciliumRifts.Kind.TAMED, pos);
        ctx.getSource().sendSuccess(() -> Component.literal("QA: a hex with a tamed rift; its threshold is about "
                + hex.threshold() + " burden"), false);
        return 1;
    }

    private static int landmark(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (!net.neoforged.fml.ModList.get().isLoaded("dynamictrees")) {
            ctx.getSource().sendFailure(Component.literal("QA: Dynamic Trees is not loaded"));
            return 0;
        }
        boolean silver = com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "silver");
        var shapes = dev.movingcastle.thaumicconcilium.world.LandmarkShapes.shapes(silver);
        int variant = Math.min(com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "variant"), shapes.size() - 1);
        var facing = net.minecraft.core.Direction.from2DDataValue(com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "facing"));
        var soil = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        boolean ok = dev.movingcastle.thaumicconcilium.compat.DynamicTreesLandmarks.grow(ctx.getSource().getLevel(), soil, silver, shapes.get(variant).code(), facing, shapes.get(variant).trunkCap());
        ctx.getSource().sendSuccess(() -> Component.literal("QA: landmark " + (silver ? "silverwood " : "greatwood ") + variant + (ok ? " grown" : " did not grow")), false);
        return ok ? 1 : 0;
    }

    private static int seedDeath(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var level = ctx.getSource().getLevel();
        net.minecraft.core.BlockPos pos = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        var seed = dev.movingcastle.thaumicconcilium.entity.VesselRite.spawnSeed(level, pos.below(), 0, 0);
        seed.setTaintReserve(0.0F); // as the tier 3 and 4 endings leave it
        seed.getPersistentData().putBoolean(dev.movingcastle.thaumicconcilium.entity.VesselEndings.DOOMED_KEY, true);
        dev.movingcastle.thaumicconcilium.world.GroveSeedDeath.begin(level, seed, level.getGameTime());
        ctx.getSource().sendSuccess(() -> Component.literal("QA: the grove is killing a Taint Seed at " + pos.toShortString()), false);
        return 1;
    }

    private static int grove(CommandContext<CommandSourceStack> ctx) {
        var level = ctx.getSource().getLevel();
        int tier = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "tier");
        int radius = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "radius");
        net.minecraft.core.BlockPos hex = net.minecraft.core.BlockPos.containing(ctx.getSource().getPosition()).below();
        var crater = new dev.movingcastle.thaumicconcilium.entity.VesselCrater(hex, 10.0, level.random.nextLong());
        crater.finish(level);
        var job = dev.movingcastle.thaumicconcilium.world.GrovePainter.openWithRadius(level, hex, crater, tier, radius, null);
        ctx.getSource().sendSuccess(() -> Component.literal(job == null ? "QA: no grove (plot chunk)" : "QA: a tier " + tier + " grove of radius " + radius), false);
        return job == null ? 0 : 1;
    }

    private static int verdict(CommandContext<CommandSourceStack> ctx, String verdict) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return dev.movingcastle.thaumicconcilium.world.QaStaging.verdict(ctx.getSource(), ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "key"), verdict);
    }

    private static int finale(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int tier = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "tier");
        int capitals = dev.movingcastle.thaumicconcilium.world.FinaleStaging.stage(ctx.getSource().getLevel(), player, tier);
        if (capitals < 0) {
            ctx.getSource().sendFailure(Component.literal("QA: cannot stage the ending here"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("QA: a tier " + tier + " rite with " + capitals + " capitals, finishing now (you are moved to the crater's edge)"), false);
        return 1;
    }

    private static int vesselBreak(CommandContext<CommandSourceStack> ctx, boolean standIn) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        net.minecraft.core.BlockPos pos = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        dev.movingcastle.thaumicconcilium.rift.HexSink.qaBreak(ctx.getSource().getLevel(), pos, player.getUUID(), standIn);
        ctx.getSource().sendSuccess(() -> Component.literal("QA: the break sequence"), false);
        return 1;
    }

    private static int vessel(CommandContext<CommandSourceStack> ctx, boolean quiescent) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        net.minecraft.core.BlockPos pos = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        dev.movingcastle.thaumicconcilium.rift.HexSink.qaVessel(ctx.getSource().getLevel(), pos, player.getUUID(), quiescent);
        ctx.getSource().sendSuccess(() -> Component.literal("QA: a " + (quiescent ? "quiescent" : "dormant") + " Vessel with four anchors"), false);
        return 1;
    }

    /** A native flux rift (Thaumaturge's own) of that size at {@code pos}. */
    private static int rift(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        net.minecraft.core.BlockPos pos = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        var level = ctx.getSource().getLevel();
        var rift = com.leclowndu93150.thaumaturge.registry.TTEntities.FLUX_RIFT.get().create(level);
        if (rift == null) {
            return 0;
        }
        rift.reseed(level.random.nextInt());
        rift.moveTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.0F, 0.0F);
        rift.resize(com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "size"));
        level.addFreshEntity(rift);
        return 1;
    }

    /** A destabilized crystal at {@code pos} holding {@code amount} of {@code aspect}. */
    private static int crystal(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        net.minecraft.core.BlockPos pos = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        var level = ctx.getSource().getLevel();
        level.setBlock(pos, dev.movingcastle.thaumicconcilium.registry.TCBlocks.DESTABILIZED_CRYSTAL.get().defaultBlockState(), 3);
        if (level.getBlockEntity(pos) instanceof dev.movingcastle.thaumicconcilium.block.DestabilizedCrystalBlockEntity crystal) {
            crystal.charge(ResourceLocationArgument.getId(ctx, "aspect"), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "amount"));
        }
        return 1;
    }

    private static int focus(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ItemStack stack = preset(StringArgumentType.getString(ctx, "preset"));
        if (stack.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("QA: no focus preset " + StringArgumentType.getString(ctx, "preset")));
            return 0;
        }
        ctx.getSource().getPlayerOrException().getInventory().placeItemBackInInventory(stack);
        return 1;
    }

    private static int caster(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "item");
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
        if (stack.isEmpty() || !(stack.getItem() instanceof FocusCaster caster)) {
            ctx.getSource().sendFailure(Component.literal("QA: " + id + " is not a caster (is its mod loaded?)"));
            return 0;
        }
        String presetId = StringArgumentType.getString(ctx, "preset");
        if (!presetId.equals("none")) {
            caster.socketFocus(stack, preset(presetId));
        }
        if (stack.getItem() instanceof ItemWand) {
            WandVisHelper.fill(stack);
        }
        ctx.getSource().getPlayerOrException().getInventory().placeItemBackInInventory(stack);
        return 1;
    }
}
