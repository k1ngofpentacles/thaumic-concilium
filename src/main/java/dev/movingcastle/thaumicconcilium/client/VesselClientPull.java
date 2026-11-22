package dev.movingcastle.thaumicconcilium.client;

import dev.movingcastle.thaumicconcilium.entity.Vessel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * The Vessel's pull on the local player (phase 5c). The server never sets a player's velocity for the pull (that rubber-bands);
 * each Vessel's client tick calls this once, and it adds the pull to this client's own player only, from what the client knows: the
 * Vessel's synced pull scale, its own sneak state, the block under its feet and its effects. The numbers come from
 * {@link Vessel#pullOn}, which also reads this client's copy of the common config: keep the {@code vessel.fight.pull} values equal
 * between the host and anyone joining over LAN.
 */
public final class VesselClientPull {
    private VesselClientPull() {
    }

    public static void pull(Vessel vessel) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.level() != vessel.level() || !player.isAlive()) {
            return;
        }
        Vec3 push = vessel.pullOn(player);
        if (push != Vec3.ZERO) {
            player.setDeltaMovement(player.getDeltaMovement().add(push));
        }
    }
}
