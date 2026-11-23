package dev.movingcastle.thaumicconcilium.client;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/**
 * Phase 5d cycles 3 and 4d: how the mana pond is drawn. Only Minecraft's standard fluid path (a still texture from the block atlas, translucent
 * render type set in {@link TCClient}), so it renders under Iris/Sodium shader packs like any modded fluid: no custom shader, no
 * block-entity renderer, no custom GL.
 *
 * <p>Cycle 4d, the chart (docs/design/5d-pond-look.md): the surface is one animated picture of 8 x 8 blocks cut into 64 tiles
 * (tools/gen_phase5d_pond.py), and a block shows the tile of its own position, so neighbours continue each other and no pond is wide
 * enough to see the picture repeat. Vanilla's {@code LiquidBlockRenderer} and Sodium's fluid renderer both ask for the texture with the
 * position ({@code FluidSpriteCache.getFluidSprites}); a still top face maps the whole sprite with u along x and v along z.
 */
public final class ManaPondClient implements IClientFluidTypeExtensions {
    /** The chart's side in blocks. */
    public static final int TILES = 8;
    private static final ResourceLocation[] TILE = new ResourceLocation[TILES * TILES];

    static {
        for (int j = 0; j < TILES; j++) {
            for (int i = 0; i < TILES; i++) {
                TILE[j * TILES + i] = ThaumicConcilium.id("block/mana_pond/t_" + i + "_" + j);
            }
        }
    }

    /** What anything without a position shows (a tank, a recipe viewer): the first tile. */
    public static final ResourceLocation STILL = TILE[0];

    /** The tile of a block column. */
    public static ResourceLocation tile(int x, int z) {
        return TILE[(z & (TILES - 1)) * TILES + (x & (TILES - 1))];
    }

    @Override
    public ResourceLocation getStillTexture() {
        return STILL;
    }

    @Override
    public ResourceLocation getFlowingTexture() {
        return STILL; // never flows
    }

    @Override
    public ResourceLocation getStillTexture(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
        return tile(pos.getX(), pos.getZ());
    }

    @Override
    public ResourceLocation getFlowingTexture(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
        return tile(pos.getX(), pos.getZ()); // the sides, where a pond has any: the same tile
    }

    @Override
    public int getTintColor() {
        return 0xFFFFFFFF;
    }
}
