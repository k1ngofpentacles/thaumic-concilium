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

    /** A research hint's entry (original: the note's {@code ..._HINT} tag). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.resources.ResourceLocation>> HINT =
            COMPONENTS.registerComponentType("hint", b -> b.persistent(net.minecraft.resources.ResourceLocation.CODEC)
                    .networkSynchronized(net.minecraft.resources.ResourceLocation.STREAM_CODEC));

    /** Wrapped in Nether Membrane (original: the {@code TAG_MEMBRANE} NBT flag); the item also gets fire_resistant. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.util.Unit>> MEMBRANE =
            COMPONENTS.registerComponentType("nether_membrane", b -> b.persistent(net.minecraft.util.Unit.CODEC)
                    .networkSynchronized(net.minecraft.network.codec.StreamCodec.unit(net.minecraft.util.Unit.INSTANCE)));

    private TCDataComponents() {
    }
}
