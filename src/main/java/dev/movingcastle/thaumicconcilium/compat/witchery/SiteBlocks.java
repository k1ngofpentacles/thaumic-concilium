package dev.movingcastle.thaumicconcilium.compat.witchery;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.block.StockadeBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Phase 10-4 (R12 C): the Burning Site's own blocks. Their register is put on the mod bus only when {@code witchery} is loaded
 * ({@link #init}); callers check {@link WitcheryGate#loaded()} before touching this class. It names no remake class.
 */
public final class SiteBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ThaumicConcilium.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ThaumicConcilium.MODID);

    /** Original: wood, hardness 25, resistance 20 (oak only: the site used metadata 0). */
    public static final DeferredBlock<StockadeBlock> STOCKADE = BLOCKS.register("stockade",
            () -> new StockadeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(25.0F, 20.0F).sound(SoundType.WOOD)
                    .noOcclusion().isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)
                    .ignitedByLava()));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> STOCKADE_ITEM = ITEMS.registerSimpleBlockItem("stockade", STOCKADE);

    /** The Burning Site's stake (a separate, blunt post; same wood strength as the stockade). */
    public static final DeferredBlock<dev.movingcastle.thaumicconcilium.block.BurningStakeBlock> BURNING_STAKE = BLOCKS.register("burning_stake",
            () -> new dev.movingcastle.thaumicconcilium.block.BurningStakeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    .strength(25.0F, 20.0F).sound(SoundType.WOOD).noOcclusion().isRedstoneConductor((s, l, p) -> false)
                    .isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false).ignitedByLava()));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> BURNING_STAKE_ITEM = ITEMS.registerSimpleBlockItem("burning_stake", BURNING_STAKE);

    private SiteBlocks() {
    }

    public static void init(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }
}
