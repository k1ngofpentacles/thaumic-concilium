package dev.movingcastle.thaumicconcilium.entity;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

/**
 * The six primal arrow kinds, in the original's index order (aer, ignis, aqua, terra, ordo, perditio).
 * Each carries its item id, its primal aspect (colour and recipe crystal) and its damage multiplier.
 */
public enum PrimalType {
    AER("aer", TTAspects.AER, 1.0, "air_arrow"),
    IGNIS("ignis", TTAspects.IGNIS, 1.0, "fire_arrow"),
    AQUA("aqua", TTAspects.AQUA, 1.0, null),
    TERRA("terra", TTAspects.TERRA, 1.5, null),
    ORDO("ordo", TTAspects.ORDO, 0.8, "order_arrow"),
    PERDITIO("perditio", TTAspects.PERDITIO, 0.8, null);

    private final String id;
    private final ResourceKey<IAspect> primal;
    private final double damageMultiplier;
    @Nullable
    private final ResourceKey<DamageType> damageType;

    PrimalType(String id, ResourceKey<IAspect> primal, double damageMultiplier, @Nullable String damageType) {
        this.id = id;
        this.primal = primal;
        this.damageMultiplier = damageMultiplier;
        this.damageType = damageType == null ? null : ResourceKey.create(Registries.DAMAGE_TYPE, ThaumicConcilium.id(damageType));
    }

    public String id() {
        return id;
    }

    /** Item registry name: {@code primal_arrow_<id>}. */
    public String itemName() {
        return "primal_arrow_" + id;
    }

    public ResourceKey<IAspect> primal() {
        return primal;
    }

    public double damageMultiplier() {
        return damageMultiplier;
    }

    /** The damage type of this kind's hits, or null for the vanilla arrow type. */
    @Nullable
    public ResourceKey<DamageType> damageType() {
        return damageType;
    }

    public static PrimalType byIndex(int i) {
        PrimalType[] all = values();
        return all[Math.floorMod(i, all.length)];
    }
}
