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

    /** Original: 3.0 hardness, 17 resistance, metal sounds. Made by a Quicksilver Elemental, not crafted. */
    public static final DeferredBlock<dev.movingcastle.thaumicconcilium.block.QuicksilverReactorBlock> QUICKSILVER_REACTOR = BLOCKS.register(
            "quicksilver_reactor", () -> new dev.movingcastle.thaumicconcilium.block.QuicksilverReactorBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 17.0F).sound(SoundType.METAL).noOcclusion()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.movingcastle.thaumicconcilium.block.QuicksilverReactorBlockEntity>> QUICKSILVER_REACTOR_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("quicksilver_reactor", () -> BlockEntityType.Builder.of(
                    dev.movingcastle.thaumicconcilium.block.QuicksilverReactorBlockEntity::new, QUICKSILVER_REACTOR.get()).build(null));

    /** Original: 3.0 hardness, 17 resistance, metal sounds. Infused from a Quicksilver Reactor. */
    public static final DeferredBlock<dev.movingcastle.thaumicconcilium.block.FleshCrucibleBlock> FLESH_CRUCIBLE = BLOCKS.register(
            "flesh_crucible", () -> new dev.movingcastle.thaumicconcilium.block.FleshCrucibleBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 17.0F).sound(SoundType.METAL).noOcclusion()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.movingcastle.thaumicconcilium.block.FleshCrucibleBlockEntity>> FLESH_CRUCIBLE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("flesh_crucible", () -> BlockEntityType.Builder.of(
                    dev.movingcastle.thaumicconcilium.block.FleshCrucibleBlockEntity::new, FLESH_CRUCIBLE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HexBlockEntity>> HEX_BLOCK_ENTITY = BLOCK_ENTITIES.register(
            "hex_of_predictability", () -> BlockEntityType.Builder.of(HexBlockEntity::new, HEX.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DestabilizedCrystalBlockEntity>> DESTABILIZED_CRYSTAL_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("destabilized_crystal",
                    () -> BlockEntityType.Builder.of(DestabilizedCrystalBlockEntity::new, DESTABILIZED_CRYSTAL.get()).build(null));

    /** Original: 3.0 hardness, 17 resistance, metal sounds. Built over a bed with a wand, also craftable as an item. */
    public static final DeferredBlock<dev.movingcastle.thaumicconcilium.block.LithographerBlock> LITHOGRAPHER = BLOCKS.register("lithographer",
            () -> new dev.movingcastle.thaumicconcilium.block.LithographerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD)
                    .strength(3.0F, 17.0F).sound(SoundType.METAL).noOcclusion()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.movingcastle.thaumicconcilium.block.LithographerBlockEntity>> LITHOGRAPHER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("lithographer", () -> BlockEntityType.Builder.of(
                    dev.movingcastle.thaumicconcilium.block.LithographerBlockEntity::new, LITHOGRAPHER.get()).build(null));

    /** Original: unbreakable, resistance 6000, cloth sounds. The Causal Bouillon's floor. */
    public static final DeferredBlock<net.minecraft.world.level.block.Block> VOID_REALITY = BLOCKS.register("void_reality",
            () -> new net.minecraft.world.level.block.Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0F, 6000.0F).sound(SoundType.WOOL).noLootTable()));

    /** Our own design (the idea is Thaumcraft 4's lifter): a greatwood and brass chamber, a full block. */
    public static final DeferredBlock<dev.movingcastle.thaumicconcilium.block.ArcaneLevitatorBlock> ARCANE_LEVITATOR = BLOCKS.register("arcane_levitator",
            () -> new dev.movingcastle.thaumicconcilium.block.ArcaneLevitatorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    .strength(2.0F, 6.0F).sound(SoundType.WOOD)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.movingcastle.thaumicconcilium.block.ArcaneLevitatorBlockEntity>> ARCANE_LEVITATOR_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("arcane_levitator", () -> BlockEntityType.Builder.of(
                    dev.movingcastle.thaumicconcilium.block.ArcaneLevitatorBlockEntity::new, ARCANE_LEVITATOR.get()).build(null));

    private TCBlocks() {
    }
}
