package dev.movingcastle.thaumicconcilium.registry;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.block.DestabilizedCrystalBlock;
import dev.movingcastle.thaumicconcilium.block.DestabilizedCrystalBlockEntity;
import dev.movingcastle.thaumicconcilium.block.HexBlock;
import dev.movingcastle.thaumicconcilium.block.HexBlockEntity;
import dev.movingcastle.thaumicconcilium.block.VisCondenserBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Blocks and block entities (phase 6a: the rift workshop). Block items are in {@link TCItems}. */
public final class TCBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ThaumicConcilium.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThaumicConcilium.MODID);

    /** Original: 0.7 hardness, 100 resistance, light 0.5 (7 of 15), stone sounds. */
    public static final DeferredBlock<HexBlock> HEX = BLOCKS.register("hex_of_predictability",
            () -> new HexBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.7F, 100.0F).lightLevel(s -> 7)
                    .sound(SoundType.STONE).noLootTable().noOcclusion()));

    public static final DeferredBlock<DestabilizedCrystalBlock> DESTABILIZED_CRYSTAL = BLOCKS.register("destabilized_crystal",
            () -> new DestabilizedCrystalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.3F)
                    .lightLevel(s -> 8).sound(SoundType.AMETHYST_CLUSTER).noLootTable().noOcclusion()));

    public static final DeferredBlock<VisCondenserBlock> VIS_CONDENSER = BLOCKS.register("vis_condenser",
            () -> new VisCondenserBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0F, 10.0F)
                    .sound(SoundType.STONE).noOcclusion()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HexBlockEntity>> HEX_BLOCK_ENTITY = BLOCK_ENTITIES.register(
            "hex_of_predictability", () -> BlockEntityType.Builder.of(HexBlockEntity::new, HEX.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DestabilizedCrystalBlockEntity>> DESTABILIZED_CRYSTAL_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("destabilized_crystal",
                    () -> BlockEntityType.Builder.of(DestabilizedCrystalBlockEntity::new, DESTABILIZED_CRYSTAL.get()).build(null));

    private TCBlocks() {
    }
}
