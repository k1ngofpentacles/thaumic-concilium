package dev.movingcastle.thaumicconcilium.compat;

import com.dtteam.dynamictrees.block.sapling.DynamicSaplingBlock;
import com.dtteam.dynamictrees.block.soil.SoilBlock;
import com.dtteam.dynamictrees.tree.TreeHelper;
import com.dtteam.dynamictrees.tree.species.Species;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.fml.ModList;

/**
 * Phase 5d cycle 2c: the only class that names Dynamic Trees. Plants Thaumaturge's greatwood or silverwood as a Dynamic Trees sapling
 * (species {@code thaumaturge:greatwood} / {@code thaumaturge:silverwood}). Never touch this class unless {@link #available()}.
 */
public final class DynamicTreesSaplings {
    private DynamicTreesSaplings() {
    }

    public static boolean available() {
        return ModList.get().isLoaded("dynamictrees");
    }

    /** Whether a block is a Dynamic Trees branch (any family). */
    public static boolean isBranch(net.minecraft.world.level.block.state.BlockState state) {
        return TreeHelper.isBranch(state);
    }

    /** Whether the block at {@code pos} is a Dynamic Trees sapling. */
    public static boolean isSapling(net.minecraft.world.level.Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof DynamicSaplingBlock;
    }

    /**
     * Cycle 4e: turns the Dynamic Trees sapling at {@code pos} into its young tree (the species' own transition, which fires Dynamic Trees' event):
     * a branch there and rooty soil at fertility 15 below. False when it is not a sapling or the transition was refused (no room above).
     */
    public static boolean transition(net.minecraft.world.level.Level level, BlockPos pos) {
        if (!(level.getBlockState(pos).getBlock() instanceof DynamicSaplingBlock sapling)) {
            return false;
        }
        return sapling.getSpecies().transitionToTree(level, pos);
    }

    /**
     * One growth pulse of the tree rooted at {@code soil} (Dynamic Trees' own {@code updateTree}, as {@code TreeHelper.growPulse}), with the leaves
     * aged every {@code ageEvery}th pulse. False when the soil is no longer rooty (the tree decayed or was cut).
     */
    public static boolean pulse(net.minecraft.world.level.Level level, BlockPos soil, boolean age) {
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(soil);
        SoilBlock rooty = TreeHelper.getRooty(state);
        if (rooty == null) {
            return false;
        }
        rooty.updateTree(state, level, soil, level.random, false);
        if (age) {
            TreeHelper.ageVolume(level, soil, 6, 24, 1, false);
        }
        return TreeHelper.isRooty(level.getBlockState(soil));
    }

    /** Plants the species' own sapling at {@code pos} (which must be a replaceable block on suitable soil); false when it cannot stand there. */
    public static boolean plant(LevelAccessor level, BlockPos pos, boolean silverwood) {
        Species species = Species.REGISTRY.get(ResourceLocation.fromNamespaceAndPath("thaumaturge", silverwood ? "silverwood" : "greatwood"));
        return species.isValid() && species.plantSapling(level, pos, false);
    }
}
