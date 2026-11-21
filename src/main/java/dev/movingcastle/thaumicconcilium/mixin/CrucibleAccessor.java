package dev.movingcastle.thaumicconcilium.mixin;

import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.content.crucible.BlockEntityCrucible;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Thaumaturge's crucible keeps its aspects in a private field and offers no setter or remover. The Quicksilver Reactor
 * merges two of them into a compound, which needs to rewrite the list; this accessor is the whole patch.
 */
@Mixin(value = BlockEntityCrucible.class, remap = false)
public interface CrucibleAccessor {
    @Accessor("aspects")
    AspectList thaumicconcilium$getAspects();

    @Accessor("aspects")
    void thaumicconcilium$setAspects(AspectList aspects);
}
