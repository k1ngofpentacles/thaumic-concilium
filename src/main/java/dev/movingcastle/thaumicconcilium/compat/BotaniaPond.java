package dev.movingcastle.thaumicconcilium.compat;

import dev.movingcastle.thaumicconcilium.registry.TCBlocks;
import dev.movingcastle.thaumicconcilium.world.GrovePaintJob;
import dev.movingcastle.thaumicconcilium.world.GrovePond;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import velrondevs.botania.api.BotaniaCapabilities;
import velrondevs.botania.api.mana.ManaPool;

/**
 * The mana pond's Botania side: each pond block answers Botania's mana-receiver query with a {@link ManaPool} view of the grove's one shared
 * ledger ({@link GrovePond}). Block capabilities need no block entity. Botania's spreader draws from any {@code ManaPool} beside it, which is
 * the only draw there is: nothing is ever pushed into the pond, and a pool beside it pulls nothing (a pool does not pull from neighbours).
 * Reached only behind {@code ModList.isLoaded("botania")}; the only class of ours (with the tests) naming Botania for the pond.
 */
public final class BotaniaPond {
    private BotaniaPond() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlock(BotaniaCapabilities.MANA_RECEIVER, (level, pos, state, be, side) -> {
            if (level instanceof ServerLevel server) {
                GrovePaintJob job = GrovePond.jobAt(server, pos);
                if (job != null) {
                    return new Source(server, pos, job);
                }
            }
            return null;
        }, TCBlocks.MANA_POND.get());
    }

    /** One pond block as a mana pool: a spreader's {@code getCurrentMana} then {@code receiveMana(-n)} is a draw on the ledger. */
    public record Source(ServerLevel level, BlockPos pos, GrovePaintJob job) implements ManaPool {
        @Override
        public boolean isOutputtingPower() {
            return false;
        }

        @Override
        public int getMaxMana() {
            return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, this.job.residual));
        }

        @Override
        public Optional<DyeColor> getColor() {
            return Optional.empty();
        }

        @Override
        public void setColor(Optional<DyeColor> color) {
        }

        @Override
        public Level getManaReceiverLevel() {
            return this.level;
        }

        @Override
        public BlockPos getManaReceiverPos() {
            return this.pos;
        }

        @Override
        public int getCurrentMana() {
            return GrovePond.offer(this.level, this.job);
        }

        /** Always full: nothing may be pushed in (bursts, sparks). */
        @Override
        public boolean isFull() {
            return true;
        }

        /** A negative amount is a draw; receiving mana is refused. */
        @Override
        public void receiveMana(int mana) {
            if (mana < 0) {
                GrovePond.draw(this.level, this.job, -(long) mana);
            }
        }

        @Override
        public boolean canReceiveManaFromBursts() {
            return false;
        }
    }
}
