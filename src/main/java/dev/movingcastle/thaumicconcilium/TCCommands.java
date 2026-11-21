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
                                        .then(Commands.literal("lesser").executes(c -> hex(c, true, false)))))
                        .then(Commands.literal("rift")
                                .then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
                                        .then(Commands.argument("size", com.mojang.brigadier.arguments.IntegerArgumentType.integer(5, 100))
                                                .executes(TCCommands::rift))))
                        .then(Commands.literal("crystal")
                                .then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
                                        .then(Commands.argument("aspect", ResourceLocationArgument.id())
                                                .then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 1000))
                                                        .executes(TCCommands::crystal)))))));
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> presets(
            CommandContext<CommandSourceStack> ctx, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                java.util.stream.Stream.concat(java.util.stream.Stream.of("none"), FocusPresets.ALL.stream().map(FocusPresets.Preset::id)), builder);
    }

    private static ItemStack preset(String id) {
        return FocusPresets.ALL.stream().filter(p -> p.id().equals(id)).findFirst().map(FocusPresets.Preset::stack).orElse(ItemStack.EMPTY);
    }

    private static int research(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ResearchGrants.grantAll(player);
        AspectPools.grantAllForCommand(player, 100);
        ctx.getSource().sendSuccess(() -> Component.literal("QA: all research and aspects granted"), false);
        return 1;
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
