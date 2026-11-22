package dev.movingcastle.thaumicconcilium.world;

import com.leclowndu93150.thaumaturge.content.entity.EntityCultist;
import com.leclowndu93150.thaumaturge.content.infusion.BlockEntityPedestal;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.entity.mob.CrimsonPaladin;
import dev.movingcastle.thaumicconcilium.entity.mob.LesserCrimsonPortal;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import dev.movingcastle.thaumicconcilium.registry.TCItems;
import dev.movingcastle.thaumicconcilium.registry.TCSounds;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * The hostile Crimson Raid a snapped binding opens (D1-c2). A Lesser Crimson Portal that belongs to nobody opens a few blocks from the
 * altar; every cultist it sends is persistent, and the first is the champion, our Crimson Paladin, with a boss bar. The portal closes as
 * any raid portal does ({@link LesserCrimsonPortal#RAID_LIFETIME}); the champion and the cultists stay.
 *
 * <p>The blade: the dagger waits on the centre pedestal. If the raid kills the binder before they hold it, the champion takes it from
 * the pedestal; if the raid kills the binder while they carry it, from their drops. The champion drops it when it dies, always. Only the
 * binder counts (on a LAN, other players are not the binder).
 */
@EventBusSubscriber(modid = ThaumicConcilium.MODID)
public final class BindingRaid {
    /** The portal opens this far (horizontally) from the matrix at least, clear of the portal's own burn range of three blocks. */
    public static final double MIN_DISTANCE = 4.0;
    public static final double MAX_DISTANCE = 6.0;

    private BindingRaid() {
    }

    /** Opens the raid beside {@code matrix} for {@code binder}; {@code pedestal} is where the dagger waits. Returns the champion, or null. */
    public static CrimsonPaladin open(ServerLevel level, BlockPos matrix, UUID binder, BlockPos pedestal) {
        LesserCrimsonPortal portal = LesserCrimsonPortal.summonHostile(level, spot(level, matrix));
        if (portal == null) {
            return null;
        }
        EntityCultist first = portal.spawnMinion(level, LesserCrimsonPortal.Minion.PALADIN);
        if (!(first instanceof CrimsonPaladin champion)) {
            return null;
        }
        champion.makeChampion();
        BindingSessions.data(level).putRaid(new BindingData.Raid(binder, champion.getUUID(), pedestal.immutable()));
        return champion;
    }

    /** A free spot four to six blocks from the matrix, at the altar's height or a little above; the air above the matrix when none fits. */
    static Vec3 spot(ServerLevel level, BlockPos matrix) {
        var dims = TCEntities.LESSER_CRIMSON_PORTAL.get().getDimensions();
        var random = level.getRandom();
        for (int i = 0; i < 32; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = MIN_DISTANCE + random.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
            double x = matrix.getX() + 0.5 + Math.cos(angle) * distance;
            double z = matrix.getZ() + 0.5 + Math.sin(angle) * distance;
            for (int dy : new int[] {1, 2, 0, 3}) {
                double y = matrix.getY() + dy;
                if (level.noCollision(dims.makeBoundingBox(x, y, z))) {
                    return new Vec3(x, y, z);
                }
            }
        }
        return Vec3.atCenterOf(matrix).add(0.0, 3.0, 0.0);
    }

    /** True when {@code source} is the raid: a cultist (or a shot of one, whose entity is its shooter) or a portal. */
    public static boolean byRaid(DamageSource source) {
        Entity attacker = source.getEntity();
        return attacker instanceof EntityCultist || attacker instanceof LesserCrimsonPortal;
    }

    /** The binder dies of any cause while the raid lives: the champion takes the dagger from the drops, or from the pedestal if it was never picked up. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer binder)) {
            return;
        }
        for (ServerLevel level : binder.server.getAllLevels()) {
            BindingData data = BindingSessions.data(level);
            if (data.raids().isEmpty()) {
                continue;
            }
            for (BindingData.Raid raid : new ArrayList<>(data.raids().values())) {
                if (!raid.binder().equals(binder.getUUID()) || !(level.getEntity(raid.champion()) instanceof CrimsonPaladin champion)
                        || !champion.isAlive()) {
                    continue;
                }
                ItemStack taken = ItemStack.EMPTY;
                for (Iterator<ItemEntity> it = event.getDrops().iterator(); it.hasNext(); ) {
                    ItemEntity drop = it.next();
                    if (drop.getItem().is(TCItems.CRIMSON_DAGGER.get())) {
                        taken = drop.getItem().copy();
                        it.remove();
                        break;
                    }
                }
                if (taken.isEmpty() && level.isLoaded(raid.pedestal()) && level.getBlockEntity(raid.pedestal()) instanceof BlockEntityPedestal ped
                        && ped.getItem().is(TCItems.CRIMSON_DAGGER.get())) {
                    taken = ped.getItem().copy();
                    ped.setItem(ItemStack.EMPTY);
                }
                if (!taken.isEmpty()) {
                    champion.holdDagger(taken);
                    level.playSound(null, champion.blockPosition(), TCSounds.SHACKLES.get(), SoundSource.HOSTILE, 1.0F, 0.5F);
                    level.getServer().getPlayerList().broadcastSystemMessage(
                            Component.translatable("message." + ThaumicConcilium.MODID + ".raid_champion_took", binder.getDisplayName()), false);
                }
            }
        }
    }

    /** A champion that dies ends its raid's blade claim (its dagger, if it held one, dropped with it). */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof CrimsonPaladin champion && champion.isChampion() && champion.level() instanceof ServerLevel level) {
            for (ServerLevel l : level.getServer().getAllLevels()) {
                BindingSessions.data(l).removeRaid(champion.getUUID());
            }
        }
    }
}
