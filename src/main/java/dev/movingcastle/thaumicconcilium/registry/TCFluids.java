package dev.movingcastle.thaumicconcilium.registry;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.fluid.ManaPondFluid;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Phase 5d cycle 3: the mana pond's fluid type and its fluids (no bucket item, see {@link ManaPondFluid}). */
public final class TCFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, ThaumicConcilium.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, ThaumicConcilium.MODID);

    /** Block light the pond gives off. */
    public static final int LIGHT = 12;

    public static final DeferredHolder<FluidType, FluidType> MANA_POND_TYPE = FLUID_TYPES.register("mana_pond",
            () -> new FluidType(FluidType.Properties.create().lightLevel(LIGHT).density(1000).viscosity(1500).canExtinguish(false).canHydrate(false)
                    .canPushEntity(false).supportsBoating(false)));

    public static final DeferredHolder<Fluid, ManaPondFluid.Source> MANA_POND = FLUIDS.register("mana_pond", () -> new ManaPondFluid.Source(properties()));
    public static final DeferredHolder<Fluid, ManaPondFluid.Flowing> MANA_POND_FLOWING = FLUIDS.register("flowing_mana_pond",
            () -> new ManaPondFluid.Flowing(properties()));

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(MANA_POND_TYPE, MANA_POND, MANA_POND_FLOWING).block(() -> (net.minecraft.world.level.block.LiquidBlock) TCBlocks.MANA_POND.get())
                .slopeFindDistance(0).levelDecreasePerBlock(8);
    }

    private TCFluids() {
    }
}
