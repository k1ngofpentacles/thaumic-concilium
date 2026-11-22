package dev.movingcastle.thaumicconcilium.client;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.world.Enslave;
import net.minecraft.client.player.Input;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

/**
 * Original ClientProxy:362-380. While the server says this player is enslaved and an enslaver (entity type tag
 * {@link Enslave#ENSLAVERS}) is within 32 blocks, the player walks forward, strafing a little at random, the yaw is re-rolled every
 * 60 ticks, and jump and sneak are off. With no enslaver near, the flag clears 40% per tick. Costs one boolean test per tick when idle.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID, value = Dist.CLIENT)
public final class EnslaveClient {
    private static boolean flagged;
    private static boolean enslaverNear;

    private EnslaveClient() {
    }

    public static void set(boolean enslaved) {
        flagged = enslaved;
        enslaverNear = enslaved;
    }

    public static boolean isFlagged() {
        return flagged;
    }

    @SubscribeEvent
    public static void input(MovementInputUpdateEvent event) {
        if (!flagged) {
            return;
        }
        Player player = event.getEntity();
        if (player.tickCount % 5 == 0) {
            enslaverNear = !player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(32.0),
                    e -> e.getType().is(Enslave.ENSLAVERS)).isEmpty();
        }
        if (!enslaverNear && player.getRandom().nextInt(10) > 5) {
            flagged = false;
            return;
        }
        if (player.tickCount % 60 == 0) {
            float yaw = 360.0F * (-0.5F + player.getRandom().nextFloat());
            player.setYRot(yaw);
            player.yRotO = yaw;
        }
        Input input = event.getInput();
        input.forwardImpulse = 1.0F;
        input.leftImpulse = -0.5F + player.getRandom().nextFloat();
        input.jumping = false;
        input.shiftKeyDown = false;
        input.up = true;
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        flagged = false;
    }
}
