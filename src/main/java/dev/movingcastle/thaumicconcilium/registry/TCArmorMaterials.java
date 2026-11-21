package dev.movingcastle.thaumicconcilium.registry;

import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Armour materials. Each material's layer is {@code textures/models/armor/<name>_layer_1.png} (helmet, chest, boots)
 * and {@code _layer_2.png} (legs), the vanilla 64x32 layout the 1.7.10 originals also used.
 */
public final class TCArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, ThaumicConcilium.MODID);

    /** Original: TCWINDINGS, reduction 1 in every slot, enchantability 30. */
    public static final Holder<ArmorMaterial> WINDINGS = register("windings", 1, 1, 1, 1, 30);
    /** Original: Thaumcraft's armorMatSpecial (reduction 1/3/2/1, enchantability 25). */
    public static final Holder<ArmorMaterial> DUMP_JACKBOOTS = register("dump_jackboots", 1, 3, 2, 1, 25);
    public static final Holder<ArmorMaterial> SURFER_SHORTS = register("elemental_surfer_shorts", 1, 3, 2, 1, 25);

    private TCArmorMaterials() {
    }

    private static Holder<ArmorMaterial> register(String name, int helmet, int chest, int legs, int boots, int enchantability) {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.HELMET, helmet);
        defense.put(ArmorItem.Type.CHESTPLATE, chest);
        defense.put(ArmorItem.Type.LEGGINGS, legs);
        defense.put(ArmorItem.Type.BOOTS, boots);
        defense.put(ArmorItem.Type.BODY, chest);
        return MATERIALS.register(name, () -> new ArmorMaterial(defense, enchantability, SoundEvents.ARMOR_EQUIP_LEATHER,
                () -> Ingredient.EMPTY, List.of(new ArmorMaterial.Layer(ThaumicConcilium.id(name))), 0.0F, 0.0F));
    }
}
