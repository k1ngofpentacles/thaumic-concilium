package dev.movingcastle.thaumicconcilium.world;

import com.leclowndu93150.thaumaturge.api.capability.PlayerResearch;
import com.leclowndu93150.thaumaturge.api.capability.PlayerResearchAccess;
import com.leclowndu93150.thaumaturge.api.research.ResearchEvent;
import com.leclowndu93150.thaumaturge.content.research.ResearchManager;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.registry.TCItems;
import dev.movingcastle.thaumicconcilium.rift.Hexes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Phase 5c cycle 4: the Vessel's field notes (research R1, our tab). Each note is handed over by the event that earns it, never polled:
 * 1 Rifts That Hold with the hex research, 2 What It Keeps when a hex you fed passes a quarter of its threshold, 3 The Vessel at the
 * first break (it appears; the subdue completes it), 6 Primordial Mote with the first mote. 4 Drawing It Out is ordinary research
 * (parents: entry 3 and Thaumaturge's {@code unlock_eldritch}) and gates the rite.
 *
 * <p>The notes are granted with Thaumaturge's own {@code ResearchManager.complete} (no parent check: an event may arrive before its
 * parent entry reads as complete) plus the entry's warp, which {@code complete} does not apply.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class VesselResearch {
    public static final ResourceLocation RIFTS_THAT_HOLD = ThaumicConcilium.id("rifts_that_hold");
    public static final ResourceLocation WHAT_IT_KEEPS = ThaumicConcilium.id("what_it_keeps");
    public static final ResourceLocation THE_VESSEL = ThaumicConcilium.id("the_vessel");
    public static final ResourceLocation DRAWING_IT_OUT = ThaumicConcilium.id("drawing_it_out");
    public static final ResourceLocation MOTE = ThaumicConcilium.id("primordial_mote");
    public static final ResourceLocation UNLOCK_ELDRITCH = ResourceLocation.fromNamespaceAndPath("thaumaturge", "unlock_eldritch");

    private VesselResearch() {
    }

    /** Whether the player may draw from a Quiescent Vessel: Thaumaturge's eldritch gate and our Drawing It Out. */
    public static boolean mayDrain(ServerPlayer player) {
        PlayerResearch r = PlayerResearchAccess.of(player);
        return r.hasCompleted(UNLOCK_ELDRITCH) && r.hasCompleted(DRAWING_IT_OUT);
    }

    /** Completes a field note (once) and applies its warp. Returns whether it was newly completed. */
    public static boolean grantNote(ServerPlayer player, ResourceLocation id, int warp) {
        if (PlayerResearchAccess.of(player).hasCompleted(id)) {
            return false;
        }
        boolean done = ResearchManager.complete(player, id);
        if (done && warp > 0) {
            ResearchManager.applyWarp(player, warp);
        }
        return done;
    }

    /** Makes an entry appear in the book, unfinished (entry 3 at the first break). */
    public static void appear(ServerPlayer player, ResourceLocation id) {
        PlayerResearch r = PlayerResearchAccess.of(player);
        if (!r.knows(id) && r.learn(id)) {
            r.queueSync(player);
        }
    }

    /** The hex research is done: the first field note. */
    @SubscribeEvent
    public static void onCompleted(ResearchEvent.Completed event) {
        if (event.player() instanceof ServerPlayer player && event.research().equals(Hexes.RESEARCH)) {
            grantNote(player, RIFTS_THAT_HOLD, 1);
        }
    }

    /** Catch-up for players who finished the hex research before this version, and the motes owed to a participant who was away. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (PlayerResearchAccess.of(player).hasCompleted(Hexes.RESEARCH)) {
                grantNote(player, RIFTS_THAT_HOLD, 1);
            }
        }
    }

    /** The first mote in a player's hands, however it came. */
    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() instanceof ServerPlayer player && event.getOriginalStack().is(TCItems.PRIMORDIAL_MOTE.get())) {
            grantNote(player, MOTE, 0);
        }
    }
}
