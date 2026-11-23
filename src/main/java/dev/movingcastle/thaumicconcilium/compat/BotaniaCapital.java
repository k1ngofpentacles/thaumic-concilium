package dev.movingcastle.thaumicconcilium.compat;

import dev.movingcastle.thaumicconcilium.block.VerdantCapitalBlockEntity;
import dev.movingcastle.thaumicconcilium.registry.TCBlocks;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import velrondevs.botania.api.BotaniaCapabilities;
import velrondevs.botania.api.mana.ManaReceiver;
import velrondevs.botania.api.mana.spark.ManaSpark;
import velrondevs.botania.api.mana.spark.SparkAttachable;

/**
 * The Verdant Capital's Botania side: a mana receiver (bursts) and a spark socket over the capital's own buffer. This is the only class
 * of ours (with the tests) that names Botania's types for the capital; it is reached only behind {@code ModList.isLoaded("botania")}.
 */
public final class BotaniaCapital {
    private BotaniaCapital() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(BotaniaCapabilities.MANA_RECEIVER, TCBlocks.VERDANT_CAPITAL_BLOCK_ENTITY.get(),
                (capital, side) -> new Receiver(capital, side));
        event.registerBlockEntity(BotaniaCapabilities.SPARK_ATTACHABLE, TCBlocks.VERDANT_CAPITAL_BLOCK_ENTITY.get(),
                (capital, side) -> new Socket(capital));
    }

    /** Bursts and spark transfers land here; the side the query came from is kept as the capital's "last fed from". */
    private record Receiver(VerdantCapitalBlockEntity capital, @Nullable Direction side) implements ManaReceiver {
        @Override
        public Level getManaReceiverLevel() {
            return this.capital.getLevel();
        }

        @Override
        public BlockPos getManaReceiverPos() {
            return this.capital.getBlockPos();
        }

        @Override
        public int getCurrentMana() {
            return this.capital.mana();
        }

        @Override
        public boolean isFull() {
            return this.capital.isFull();
        }

        @Override
        public void receiveMana(int mana) {
            this.capital.addMana(mana, this.side);
        }

        @Override
        public boolean canReceiveManaFromBursts() {
            return true;
        }
    }

    /** A spark sits in the block above, as with a Botania pool. */
    private record Socket(VerdantCapitalBlockEntity capital) implements SparkAttachable {
        @Override
        public boolean canAttachSpark(ItemStack stack) {
            return true;
        }

        @Override
        public int getAvailableSpaceForMana() {
            return Math.max(0, VerdantCapitalBlockEntity.capacity() - this.capital.mana());
        }

        @Nullable
        @Override
        public ManaSpark getAttachedSpark() {
            Level level = this.capital.getLevel();
            if (level == null) {
                return null;
            }
            Vec3 low = Vec3.atLowerCornerOf(this.capital.getBlockPos().above());
            List<Entity> sparks = level.getEntitiesOfClass(Entity.class, new AABB(low, low.add(1, 1, 1)), e -> e instanceof ManaSpark);
            return sparks.size() == 1 ? (ManaSpark) sparks.get(0) : null;
        }

        @Override
        public boolean areIncomingTranfersDone() {
            return false;
        }
    }
}
