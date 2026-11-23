package dev.movingcastle.thaumicconcilium.compat;

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

    /** Plants the species' own sapling at {@code pos} (which must be a replaceable block on suitable soil); false when it cannot stand there. */
    public static boolean plant(LevelAccessor level, BlockPos pos, boolean silverwood) {
        Species species = Species.REGISTRY.get(ResourceLocation.fromNamespaceAndPath("thaumaturge", silverwood ? "silverwood" : "greatwood"));
        return species.isValid() && species.plantSapling(level, pos, false);
    }
}
