package dev.movingcastle.thaumicconcilium.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Terra-cast Gem's cast: the block it absorbed (original NBT {@code name}, {@code meta}) and the dimension it was in ({@code dim}).
 * The dimension is a key, not the 1.7.10 numeric id, so a cast of a dimension that is no longer there falls back to the overworld.
 */
public record GemCast(BlockState state, ResourceKey<Level> dimension) {
    public static final Codec<GemCast> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockState.CODEC.fieldOf("state").forGetter(GemCast::state),
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(GemCast::dimension)).apply(i, GemCast::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GemCast> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), GemCast::state,
            ResourceKey.streamCodec(Registries.DIMENSION), GemCast::dimension,
            GemCast::new);
}
