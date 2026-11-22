package dev.movingcastle.thaumicconcilium.aura;

import com.leclowndu93150.thaumaturge.api.capability.PlayerResearchAccess;
import com.leclowndu93150.thaumaturge.content.wands.ItemWand;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import com.leclowndu93150.thaumaturge.content.world.crystal.BlockCrystal;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.AuraCoreEntity;
import dev.movingcastle.thaumicconcilium.item.AuraCore;
import dev.movingcastle.thaumicconcilium.item.CastingBracelet;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Starting an attunement (original: Gadomancy's {@code WandHandler.tryAuraCoreCreation}): a wand struck on a primal crystal cluster,
 * with a blank Aura Core lying within ten blocks, spends 84 vis of the cluster's primal, breaks the cluster and turns the core into an
 * {@link AuraCoreEntity}. Needs the Aura Core research.
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class AuraCores {
    public static final ResourceLocation RESEARCH = ThaumicConcilium.id("aura_core");
    public static final double RANGE = 10.0;
    /** Original: 84 vis of the cluster's aspect (centivis here, as the Hex's costs). */
    public static final int VIS = 84;

    private AuraCores() {
    }

    @SubscribeEvent
    public static void onWand(PlayerInteractEvent.RightClickBlock event) {
        ItemStack wand = event.getItemStack();
        if (!(wand.getItem() instanceof ItemWand) || wand.getItem() instanceof CastingBracelet
                || !(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockCrystal crystal) || crystal.isFlux()
                || !PlayerResearchAccess.of(player).hasCompleted(RESEARCH)) {
            return;
        }
        ItemEntity core = findBlank(level, pos);
        if (core == null) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!WandVisHelper.payCosts(wand, player, Map.of(crystal.aspect(), VIS * 100), true, true)) {
            player.displayClientMessage(Component.translatable("message.thaumicconcilium.aura_core_no_vis", VIS), true);
            return;
        }
        start(level, pos, crystal, core);
    }

    /** The first blank core item (not already attuning) within {@link #RANGE} blocks of the cluster. */
    @Nullable
    public static ItemEntity findBlank(ServerLevel level, BlockPos pos) {
        AABB box = new AABB(pos).inflate(RANGE);
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (!(e instanceof AuraCoreEntity) && !e.isRemoved() && e.getItem().getItem() instanceof AuraCore
                    && AuraCore.type(e.getItem()).isBlank()) {
                return e;
            }
        }
        return null;
    }

    /** Breaks the cluster (no block damage: a burst and a sound) and swaps the core item for an {@link AuraCoreEntity}. */
    public static AuraCoreEntity start(ServerLevel level, BlockPos pos, BlockCrystal crystal, ItemEntity core) {
        level.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
        level.playSound(null, pos, TTSounds.WAND.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.removeBlock(pos, false);
        AuraCoreEntity attuning = new AuraCoreEntity(level, core.getX(), core.getY(), core.getZ(), core.getItem(), pos, crystal.aspect());
        attuning.setDeltaMovement(core.getDeltaMovement());
        level.addFreshEntity(attuning);
        core.discard();
        return attuning;
    }
}
