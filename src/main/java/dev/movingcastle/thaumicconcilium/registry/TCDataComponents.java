package dev.movingcastle.thaumicconcilium.registry;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import dev.movingcastle.thaumicconcilium.item.ShardMill;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item data components. */
public final class TCDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ThaumicConcilium.MODID);

    /** A Shard Mill's lid, selected shard and loaded powder (the original's NBT: IsOpen, Type, Loaded, Amount). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ShardMill.State>> MILL_STATE =
            COMPONENTS.registerComponentType("mill_state", b -> b.persistent(ShardMill.State.CODEC)
                    .networkSynchronized(ShardMill.State.STREAM_CODEC));

    private TCDataComponents() {
    }
}
