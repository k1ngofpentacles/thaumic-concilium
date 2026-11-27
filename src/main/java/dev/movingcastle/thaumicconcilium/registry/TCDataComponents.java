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

    /** Who signed a wand (original: the wand's {@code Xylography} NBT string); see {@link dev.movingcastle.thaumicconcilium.item.Xylography}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<dev.movingcastle.thaumicconcilium.item.Xylography.Signer>> XYLOGRAPHY =
            COMPONENTS.registerComponentType("xylography", b -> b.persistent(dev.movingcastle.thaumicconcilium.item.Xylography.Signer.CODEC)
                    .networkSynchronized(dev.movingcastle.thaumicconcilium.item.Xylography.Signer.STREAM_CODEC));

    /** The xylographed wand held in an Astral Monitor (original: the monitor's {@code wand} NBT). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.world.item.ItemStack>> MONITOR_WAND =
            COMPONENTS.registerComponentType("monitor_wand", b -> b.persistent(net.minecraft.world.item.ItemStack.CODEC)
                    .networkSynchronized(net.minecraft.world.item.ItemStack.STREAM_CODEC));

    /** What the monitor last read from the watched player (original: the monitor's {@code vals} int array). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<dev.movingcastle.thaumicconcilium.lithograph.PartyStats>> MONITOR_STATS =
            COMPONENTS.registerComponentType("monitor_stats", b -> b.persistent(dev.movingcastle.thaumicconcilium.lithograph.PartyStats.CODEC)
                    .networkSynchronized(dev.movingcastle.thaumicconcilium.lithograph.PartyStats.STREAM_CODEC));

    /** A Gem of Rifts' casts, selection and curvature (original NBT MAX_GEMS, CURR, NAME*, META*, DIM*, REVERSE). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<dev.movingcastle.thaumicconcilium.item.RiftGem.State>> RIFT_STATE =
            COMPONENTS.registerComponentType("rift_state", b -> b.persistent(dev.movingcastle.thaumicconcilium.item.RiftGem.State.CODEC)
                    .networkSynchronized(dev.movingcastle.thaumicconcilium.item.RiftGem.State.STREAM_CODEC));

    /** The block a Terra-cast Gem absorbed and its dimension (original NBT name, meta, dim). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<dev.movingcastle.thaumicconcilium.item.GemCast>> TERRA_CAST =
            COMPONENTS.registerComponentType("terra_cast", b -> b.persistent(dev.movingcastle.thaumicconcilium.item.GemCast.CODEC)
                    .networkSynchronized(dev.movingcastle.thaumicconcilium.item.GemCast.STREAM_CODEC));

    private TCDataComponents() {
    }
}
