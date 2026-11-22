package dev.movingcastle.thaumicconcilium.entity.mob;

import com.leclowndu93150.thaumaturge.content.entity.EntityCultist;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Shared base of the Crimson Paladin, Archer and Ranger (original: extend Thaumcraft's EntityCultist). Builds on Thaumaturge's own
 * cultist, so they are allied with Thaumaturge's cultists, never attack them, cloak as they do and share their idle and target
 * goals. Equipment comes from our data tables {@code thaumicconcilium:equipment/<entity id>}; drops from
 * {@code thaumicconcilium:entities/<entity id>}.
 */
public abstract class CrimsonCultist extends EntityCultist {
    protected CrimsonCultist(EntityType<? extends CrimsonCultist> type, Level level) {
        super(type, level);
    }

    /** Original: follow 32, speed 0.28, attack 8.0, and the mob's own health. */
    protected static AttributeSupplier.Builder crimsonAttributes(double health) {
        return createCultistAttributes().add(Attributes.MAX_HEALTH, health).add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 8.0);
    }

    @Override
    protected ResourceKey<LootTable> equipmentTable() {
        return ResourceKey.create(Registries.LOOT_TABLE,
                ThaumicConcilium.id("equipment/" + BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath()));
    }
}
