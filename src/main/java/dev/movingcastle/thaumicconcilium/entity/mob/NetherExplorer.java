package dev.movingcastle.thaumicconcilium.entity.mob;

import com.leclowndu93150.thaumaturge.content.research.ResearchManager;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.registry.TCItems;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Nether Explorer (original: {@code corpse.NetherExplorer}): a would-be conqueror of the Nether, drifting on the lava
 * sea. Hitting or touching it frees a Nether Membrane (fire-proof, so it survives the lava) and teaches the Nether
 * Membrane research.
 */
public class NetherExplorer extends Corpse {
    public static final ResourceLocation RESEARCH = ThaumicConcilium.id("nether_membrane");

    public NetherExplorer(EntityType<? extends NetherExplorer> type, Level level) {
        super(type, level);
    }

    @Override
    protected void disturb(@Nullable Player player) {
        ItemStack membrane = new ItemStack(TCItems.NETHER_MEMBRANE.get());
        // Hit: the membrane flies out. Touched: straight into the inventory if there's room.
        boolean taken = player != null && !this.wasHit && player.getInventory().add(membrane);
        if (!taken) {
            ItemEntity item = new ItemEntity(this.level(), this.getX(), this.getY() + 0.5, this.getZ(), membrane);
            item.setDeltaMovement(item.getDeltaMovement().add(0.0, 0.3, 0.0));
            item.setPickUpDelay(40);
            this.level().addFreshEntity(item);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            grantResearch(serverPlayer);
        }
        this.discard();
    }

    private boolean wasHit;

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        this.wasHit = true;
        return super.hurt(source, amount);
    }

    /** Original: completes NETHER_MEMBRANE, with a heartbeat. */
    public static void grantResearch(ServerPlayer player) {
        if (ResearchManager.complete(player, RESEARCH)) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.HEARTBEAT.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
