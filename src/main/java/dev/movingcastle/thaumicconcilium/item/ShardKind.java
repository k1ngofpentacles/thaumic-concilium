package dev.movingcastle.thaumicconcilium.item;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaCrystalAccess;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * The shards a Shard Mill grinds, in the original's order: the Greatwood mill takes Thaumaturge's primal crystals
 * (TC4 shard metadata 0-5: air, fire, water, earth, order, entropy), the Silverwood mill Forbidden Magic's deadly
 * shards (TC4 types 7-13: wrath, envy, taint, pride, lust, sloth, greed). The original's balanced shard (type 6) is
 * Thaumic Horizons' {@code thaumichorizons:balanced_shard} (tag {@code c:balanced_shards}); its powder does nothing, as
 * in the original ({@code case 6: default: break}). The gluttony shard (type 14) is left out.
 * The powder takes its colour from the shard's aspect, as the original's renderer did.
 * <p>
 * Thaumaturge's primal shards are essentia crystals ({@code thaumaturge:essentia_crystal} with a crystal aspect),
 * which crystal clusters drop; the cluster block items are accepted too.
 */
public enum ShardKind {
    AER("thaumaturge:crystal_aer", "thaumaturge:aer"),
    IGNIS("thaumaturge:crystal_ignis", "thaumaturge:ignis"),
    AQUA("thaumaturge:crystal_aqua", "thaumaturge:aqua"),
    TERRA("thaumaturge:crystal_terra", "thaumaturge:terra"),
    ORDO("thaumaturge:crystal_ordo", "thaumaturge:ordo"),
    PERDITIO("thaumaturge:crystal_perditio", "thaumaturge:perditio"),
    WRATH("forbiddenmagic:wrath_shard", "forbiddenmagic:ira"),
    ENVY("forbiddenmagic:envy_shard", "forbiddenmagic:invidia"),
    TAINT("forbiddenmagic:taint_shard", "thaumaturge:vitium"),
    PRIDE("forbiddenmagic:pride_shard", "forbiddenmagic:superbia"),
    LUST("forbiddenmagic:lust_shard", "forbiddenmagic:luxuria"),
    SLOTH("forbiddenmagic:sloth_shard", "forbiddenmagic:desidia"),
    GREED("forbiddenmagic:greed_shard", "thaumaturge:desiderium"),
    /** Appended last so saved mill state (shard ordinals) stays valid. Powder colour: auram. */
    BALANCED("thaumichorizons:balanced_shard", "thaumaturge:auram");

    public static final List<ShardKind> GREATWOOD = List.of(AER, IGNIS, AQUA, TERRA, ORDO, PERDITIO, BALANCED);
    public static final List<ShardKind> SILVERWOOD = List.of(WRATH, ENVY, TAINT, PRIDE, LUST, SLOTH, GREED);

    public static final TagKey<Item> BALANCED_TAG = TagKey.create(Registries.ITEM, ResourceLocation.parse("c:balanced_shards"));

    private final ResourceLocation item;
    private final ResourceLocation aspect;
    private int color = -1;

    ShardKind(String item, String aspect) {
        this.item = ResourceLocation.parse(item);
        this.aspect = ResourceLocation.parse(aspect);
    }

    /** Whether {@code stack} is a shard of this kind. */
    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (this == BALANCED) {
            return stack.is(BALANCED_TAG);
        }
        if (stack.is(this.item())) {
            return true;
        }
        Holder<IAspect> crystal = EssentiaCrystalAccess.aspect(stack);
        return this.ordinal() < WRATH.ordinal() && crystal != null && crystal.is(this.aspect);
    }

    public Item item() {
        return BuiltInRegistries.ITEM.get(this.item);
    }

    /** The aspect's colour, looked up once (aspects are datapack entries, so this needs the registries). */
    public int color(RegistryAccess registries) {
        if (this.color < 0) {
            this.color = registries.registry(IAspect.REGISTRY_KEY)
                    .flatMap(r -> r.getOptional(this.aspect))
                    .map(IAspect::color)
                    .orElse(0xFFFFFF) & 0xFFFFFF;
        }
        return this.color;
    }

    public static ShardKind byOrdinal(int ordinal) {
        ShardKind[] all = values();
        return all[Math.floorMod(ordinal, all.length)];
    }
}
