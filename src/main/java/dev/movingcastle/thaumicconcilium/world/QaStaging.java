package dev.movingcastle.thaumicconcilium.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.content.research.ResearchGrants;
import com.leclowndu93150.thaumaturge.content.research.ResearchProgression;
import com.leclowndu93150.thaumaturge.content.research.pool.AspectPools;
import net.minecraft.resources.ResourceLocation;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

/**
 * One-click QA staging for the generated FTB Quests QA chapters ({@code tools/qa-quests}). The generator writes
 * {@code config/thaumicconcilium/qa_stages.json}: per check (key {@code <chapter>.<check>}) a site slot, the kit, the
 * build commands, its quest id, steps, expectations and the next check's key.
 * <ul>
 *   <li>{@code qa stage <key>}: generates the slot's far-away chunks if needed, clears a 31x28x31 box at y 120, lays a
 *   grass pad with a glass rim, empties and heals the player, teleports them onto the pad facing south (+z), runs the
 *   kit as them and the build at the pad's centre, then prints the steps, expectations and the verdict buttons in
 *   chat. A check with {@code "site": "here"} skips the pad and the teleport (it needs real terrain).</li>
 *   <li>{@code qa verdict <key> pass|fail|skip}: logs the verdict to {@code qa_verdicts.log} in the game folder,
 *   completes the check's quest on a pass, and stages the next check.</li>
 * </ul>
 * Nothing here runs unless a tester calls it; no state is kept between calls (the file is read each time).
 */
@EventBusSubscriber(modid = dev.movingcastle.thaumicconcilium.ThaumicConcilium.MODID)
public final class QaStaging {
    public static final int Y = 120;
    private static final int R = 15;        // box half-width; the glass rim is at 13, the pad inside it
    private static final int RIM = 13;
    private static final int HEIGHT = 24;   // air above the pad

    private QaStaging() {
    }

    public static BlockPos origin(int slot) {
        return new BlockPos(48000 + (slot % 20) * 96, Y, 48000 + (slot / 20) * 96);
    }

    private static JsonObject entry(String key) {
        Path file = FMLPaths.CONFIGDIR.get().resolve("thaumicconcilium").resolve("qa_stages.json");
        if (!Files.exists(file)) {
            return null;
        }
        try {
            JsonObject all = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            return all.has(key) ? all.getAsJsonObject(key) : null;
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    /** @return 1 when staged, 0 when the key is unknown */
    public static int stage(CommandSourceStack source, ServerPlayer player, String key) {
        JsonObject e = entry(key);
        if (e == null) {
            source.sendFailure(Component.literal("QA: no staging for " + key + " (regenerate with tools/qa-quests/gen.py, then /ftbquests reload)"));
            return 0;
        }
        MinecraftServer server = source.getServer();
        ServerLevel level = player.serverLevel();
        Vec3 at = player.position();
        boolean here = e.has("site") && "here".equals(e.get("site").getAsString());
        if (!here) {
            level = server.overworld();
            BlockPos o = origin(e.get("slot").getAsInt());
            buildSite(level, o);
            at = Vec3.atBottomCenterOf(o);
            player.teleportTo(level, at.x, at.y, at.z, 0.0F, 10.0F);
            if (player.position().distanceToSqr(at) > 1.0) {   // a player with no connection is not moved by the packet
                player.moveTo(at.x, at.y, at.z, 0.0F, 10.0F);
            }
        }
        // a clean, healthy player
        player.getInventory().clearContent();
        player.getInventory().selected = 0;   // the kit fills the hotbar from slot 1: the tester holds the first item
        if (player.connection != null) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
        }
        player.removeAllEffects();
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0F);
        player.clearFire();
        player.fallDistance = 0.0F;
        player.setDeltaMovement(Vec3.ZERO);
        CommandSourceStack as = player.createCommandSourceStack().withPermission(4).withSuppressedOutput();
        if (ModList.get().isLoaded("curios")) {
            run(server, as, "curios clear @s");
        }
        research(player, array(e, "research"));
        for (JsonElement c : array(e, "kit")) {
            run(server, as, c.getAsString());
        }
        // the build waits a few ticks: the player's arrival makes the site's chunks entity-ticking, so summoned mobs
        // are found by selectors (@e) and start their AI
        CommandSourceStack build = as.withLevel(level).withPosition(at);
        JsonArray cmds = array(e, "build");
        PENDING.add(new Pending(server.getTickCount() + (here ? 0 : BUILD_DELAY), () -> {
            for (JsonElement c : cmds) {
                run(server, build, c.getAsString());
            }
            player.sendSystemMessage(card(key, e));
        }));
        return 1;
    }

    private static final int BUILD_DELAY = 5;
    private static final List<Pending> PENDING = new ArrayList<>();

    private record Pending(int due, Runnable task) {
    }

    /** Runs a staging's build once it is due; nothing to do (an empty list) unless a tester just staged a check. */
    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) {
            return;
        }
        int now = event.getServer().getTickCount();
        List<Pending> due = PENDING.stream().filter(p -> p.due() <= now).toList();
        PENDING.removeAll(due);
        due.forEach(p -> p.task().run());
    }

    public static int verdict(CommandSourceStack source, ServerPlayer player, String key, String verdict) {
        JsonObject e = entry(key);
        if (e == null) {
            source.sendFailure(Component.literal("QA: no staging for " + key));
            return 0;
        }
        String line = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\t" + verdict + "\t" + key + "\t" + str(e, "title") + System.lineSeparator();
        try {
            Files.writeString(FMLPaths.GAMEDIR.get().resolve("qa_verdicts.log"), line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            source.sendFailure(Component.literal("QA: could not write qa_verdicts.log: " + ex.getMessage()));
        }
        if ("pass".equals(verdict) && e.has("quest") && ModList.get().isLoaded("ftbquests")) {
            run(source.getServer(), source.getServer().createCommandSourceStack().withSuppressedOutput(),
                    "ftbquests change_progress " + player.getGameProfile().getName() + " complete " + e.get("quest").getAsString());
        }
        player.sendSystemMessage(Component.literal("QA: " + verdict + " logged for " + str(e, "title")).withStyle(ChatFormatting.GRAY));
        String next = str(e, "next");
        if (next.isEmpty()) {
            player.sendSystemMessage(Component.literal("QA: that was the last check of " + str(e, "chapter") + ".").withStyle(ChatFormatting.GOLD));
            return 1;
        }
        return stage(source, player, next);
    }

    /**
     * Only the research a check needs: everything is forgotten, then each listed entry is completed with its parents
     * and required research ({@code ResearchProgression.grant}); {@code "*"} grants all. Every aspect is discovered
     * with 100 points either way, and warp (which completing research can add) is cleared, so no warp effects or
     * unrelated unlocks follow the tester from check to check.
     */
    private static void research(ServerPlayer player, JsonArray ids) {
        ResearchProgression.reset(player);
        for (JsonElement id : ids) {
            if ("*".equals(id.getAsString())) {
                ResearchGrants.grantAll(player);
            } else {
                ResourceLocation rl = ResourceLocation.tryParse(id.getAsString());
                if (rl != null) {
                    ResearchProgression.grant(player, rl);
                }
            }
        }
        ResearchGrants.discoverAspects(player, player.registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY).listElements().toList());
        AspectPools.grantAllForCommand(player, 100);
        WarpHelper.getWarp(player).clear();
    }

    /**
     * {@code qa wear}: the main-hand item goes into the first empty Curios slot that takes it (a charged bracelet from
     * {@code qa caster}, a ring, an amulet). @return 1 when worn
     */
    public static int wear(CommandSourceStack source, ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("QA: nothing in the main hand to wear"));
            return 0;
        }
        var inv = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).orElse(null);
        if (inv != null) {
            for (String slot : top.theillusivec4.curios.api.CuriosApi.getItemStackSlots(stack, player).keySet()) {
                var handler = inv.getCurios().get(slot);
                if (handler == null) {
                    continue;
                }
                for (int i = 0; i < handler.getStacks().getSlots(); i++) {
                    if (handler.getStacks().getStackInSlot(i).isEmpty()) {
                        handler.getStacks().setStackInSlot(i, stack.copy());
                        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                        return 1;
                    }
                }
            }
        }
        source.sendFailure(Component.literal("QA: no free Curios slot takes " + stack.getHoverName().getString()));
        return 0;
    }

    /**
     * {@code qa jars <pos>}: a wall of creative jars, one per aspect, 9 wide (+x from pos) and as many rows up as needed,
     * so an infusion altar (Thaumaturge drains jars within 12 blocks) or tubes have every essentia without limit.
     */
    public static int jars(ServerLevel level, BlockPos pos) {
        var aspects = level.registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY).listElements().toList();
        int i = 0;
        for (var aspect : aspects) {
            BlockPos p = pos.offset(i % 9, i / 9, 0);
            level.setBlock(p, com.leclowndu93150.thaumaturge.registry.TTBlocks.JAR_CREATIVE.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(p) instanceof com.leclowndu93150.thaumaturge.content.essentia.jar.AbstractBlockEntityJar jar) {
                jar.setStorageContents(aspect.key(), 1);
                jar.setChanged();
                level.sendBlockUpdated(p, jar.getBlockState(), jar.getBlockState(), Block.UPDATE_ALL);
            }
            i++;
        }
        return i;
    }

    /** Generates the chunks if needed and clears the box, the pad and its rim; public for the pack test. */
    public static void buildSite(ServerLevel level, BlockPos o) {
        for (int cx = (o.getX() - R) >> 4; cx <= (o.getX() + R) >> 4; cx++) {
            for (int cz = (o.getZ() - R) >> 4; cz <= (o.getZ() + R) >> 4; cz++) {
                level.getChunk(cx, cz);   // generates it on the spot if it never was
            }
        }
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                boolean inside = Math.abs(dx) < RIM && Math.abs(dz) < RIM;
                boolean rim = Math.max(Math.abs(dx), Math.abs(dz)) == RIM;
                for (int dy = -3; dy <= HEIGHT; dy++) {
                    BlockState s = Blocks.AIR.defaultBlockState();
                    if (inside || rim) {
                        if (dy == -1) {
                            s = Blocks.GRASS_BLOCK.defaultBlockState();
                        } else if (dy < -1) {
                            s = Blocks.STONE.defaultBlockState();
                        } else if (rim && dy <= 1) {
                            s = Blocks.GLASS.defaultBlockState();
                        }
                    }
                    p.set(o.getX() + dx, o.getY() + dy, o.getZ() + dz);
                    if (level.getBlockState(p) != s) {
                        level.setBlock(p, s, flags);
                    }
                }
            }
        }
        // golem seals are level data, not blocks: a re-stage would find the last one still there
        for (var seal : com.leclowndu93150.thaumaturge.content.golem.seals.SealHandler.getSealsInRange(level, o, R + 4)) {
            com.leclowndu93150.thaumaturge.content.golem.seals.SealHandler.removeSealEntity(level, seal.pos(), true);
        }
        // after the blocks, so anything a cleared container dropped goes too
        AABB box = new AABB(o.getX() - R - 2, o.getY() - 4, o.getZ() - R - 2, o.getX() + R + 3, o.getY() + HEIGHT + 8, o.getZ() + R + 3);
        for (Entity ent : level.getEntities((Entity) null, box, ent -> !(ent instanceof Player))) {
            ent.discard();
        }
    }

    private static Component card(String key, JsonObject e) {
        MutableComponent out = Component.empty();
        out.append(Component.literal("━━ " + str(e, "chapter") + ": ").withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.literal(str(e, "title")).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        int i = 1;
        for (JsonElement s : array(e, "steps")) {
            out.append("\n").append(Component.literal((i++) + ". ").withStyle(ChatFormatting.GRAY)).append(s.getAsString());
        }
        for (JsonElement s : array(e, "expect")) {
            out.append("\n").append(Component.literal("→ ").withStyle(ChatFormatting.GREEN)).append(Component.literal(s.getAsString()).withStyle(ChatFormatting.GREEN));
        }
        out.append("\n")
                .append(button("[✔ Pass, next]", ChatFormatting.GREEN, "/thaumicconcilium qa verdict " + key + " pass", "Tick this check's quest, log it, stage the next check"))
                .append(" ")
                .append(button("[✘ Fail, next]", ChatFormatting.RED, "/thaumicconcilium qa verdict " + key + " fail", "Log a failure (type what you saw in chat), stage the next check"))
                .append(" ")
                .append(button("[↻ Again]", ChatFormatting.YELLOW, "/thaumicconcilium qa stage " + key, "Rebuild this check's site and kit"))
                .append(" ")
                .append(button("[⏭ Skip]", ChatFormatting.GRAY, "/thaumicconcilium qa verdict " + key + " skip", "Log a skip, stage the next check"));
        return out;
    }

    private static Component button(String text, ChatFormatting colour, String command, String hover) {
        return Component.literal(text).withStyle(s -> s.withColor(colour).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover))));
    }

    private static void run(MinecraftServer server, CommandSourceStack source, String command) {
        server.getCommands().performPrefixedCommand(source, command.startsWith("/") ? command.substring(1) : command);
    }

    private static JsonArray array(JsonObject e, String k) {
        return e.has(k) ? e.getAsJsonArray(k) : new JsonArray();
    }

    private static String str(JsonObject e, String k) {
        return e.has(k) ? e.get(k).getAsString() : "";
    }
}
