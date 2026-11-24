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

    /** The outfit table (data {@code crimson_outfits/<name>.json}) our mob dresses from; null for a mob with its own robe. */
    protected String outfitTable() {
        return BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath();
    }

    /** Host equipment (weapon, and the original's armour as the fallback), then a weighted outfit over its armour slots. */
    @org.jetbrains.annotations.Nullable
    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty, net.minecraft.world.entity.MobSpawnType reason,
            @org.jetbrains.annotations.Nullable net.minecraft.world.entity.SpawnGroupData groupData) {
        net.minecraft.world.entity.SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        String table = outfitTable();
        if (table != null) {
            dev.movingcastle.thaumicconcilium.world.CrimsonOutfits.dress(this, table);
        }
        return data;
    }

    @Override
    protected ResourceKey<LootTable> equipmentTable() {
        return ResourceKey.create(Registries.LOOT_TABLE,
                ThaumicConcilium.id("equipment/" + BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath()));
    }
}
