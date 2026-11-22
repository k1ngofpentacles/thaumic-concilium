package dev.movingcastle.thaumicconcilium.world;

import com.leclowndu93150.thaumaturge.content.entity.EntityCultist;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.mob.CrimsonPontifex;
import dev.movingcastle.thaumicconcilium.item.PontifexRobe;
import dev.movingcastle.thaumicconcilium.registry.TCSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

/**
 * The Pontifex's robe toggle (original: PacketTogglePontifexRobe, PontifexRobe.onArmorTick, TCEntityEventHandler:181, :461).
 * The state lives in {@link CrimsonState} ({@code pontifexRobeToggle}, {@code ethereal}, {@code chainedTime}); the ethereal rules
 * themselves are in {@link Ethereal}. Wearing the full set and pressing the toggle key lets go of material wholeness and holds the
 * wearer {@link Chained} while it lasts. Taking a piece off, dying or toggling again ends it. Cultists that are not the Pontifex
 * never aim at a wearer of the full set.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class PontifexToggle {
    /** Original: chainedTime = 100 each armour tick while toggled. */
    public static final int CHAIN_TICKS = 100;

    private PontifexToggle() {
    }

    public static boolean isOn(Player player) {
        return CrimsonState.get(player).pontifexRobeToggle();
    }

    /** What the toggle key asks for. False (and nothing changes) unless the full set is worn, or the wearer is ethereal by other means. */
    public static boolean toggle(ServerPlayer player) {
        if (!PontifexRobe.isFullSet(player)) {
            return false;
        }
        CrimsonState.State s = CrimsonState.get(player);
        if (!s.pontifexRobeToggle() && s.ethereal()) {
            return false; // original PacketTogglePontifexRobe:50-52: ethereal without the toggle (the Peeler) stays
        }
        set(player, !s.pontifexRobeToggle());
        return true;
    }

    public static void set(ServerPlayer player, boolean on) {
        CrimsonState.State s = CrimsonState.get(player);
        if (s.pontifexRobeToggle() == on) {
            return;
        }
        if (on) {
            CrimsonState.set(player, s.withToggle(true).withEthereal(true).withChained(CHAIN_TICKS));
            player.level().playSound(null, player.blockPosition(), TCSounds.SHACKLES.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        } else {
            CrimsonState.set(player, s.withToggle(false).withEthereal(false));
        }
    }

    /** Original TCEntityEventHandler:181: a cultist drops its target when that is a player in the full set. */
    @SubscribeEvent
    public static void aggro(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof EntityCultist && !(event.getEntity() instanceof CrimsonPontifex)
                && event.getNewAboutToBeSetTarget() instanceof Player player && PontifexRobe.isFullSet(player)) {
            event.setNewAboutToBeSetTarget(null);
        }
    }
}
