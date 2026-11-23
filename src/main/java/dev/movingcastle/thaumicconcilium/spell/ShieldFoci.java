package dev.movingcastle.thaumicconcilium.spell;

import com.leclowndu93150.thaumaturge.api.spell.part.SpellPart;
import dev.movingcastle.thaumicconcilium.ThaumicConcilium;
import net.minecraft.core.RegistryAccess;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbilities;

/**
 * The guard stance (decided with the user 2026-11-23): a worn bracelet casts with a shield in the off hand only if the spell
 * needs no aim past the shield. The gate is the drum's first-order walk ({@link DrumCastable}) against the delivery tag
 * {@link #GUARD_CASTABLE} ({@code thaumicconcilium:guard_castable}: self, aura, cloud, touch, wall and our riposte); an
 * effect straight off the origin passes, and the aimed effects of {@link DrumCastable#AIMED_EFFECTS} are refused.
 * <p>
 * {@link #TAG} ({@code thaumicconcilium:shield_foci}) only marks our shield parts (riposte, aegis) for tooltips and research
 * text; it is no longer the gate (the old "any tagged node" rule let self + projectile through).
 * A shield in the main hand stays ungated (user decision 8).
 */
public final class ShieldFoci {
    public static final TagKey<SpellPart> TAG = TagKey.create(SpellPart.REGISTRY_KEY, ThaumicConcilium.id("shield_foci"));
    public static final TagKey<SpellPart> GUARD_CASTABLE = TagKey.create(SpellPart.REGISTRY_KEY, ThaumicConcilium.id("guard_castable"));

    private ShieldFoci() {
    }

    public static boolean holdsShield(LivingEntity entity) {
        return entity.getOffhandItem().canPerformAction(ItemAbilities.SHIELD_BLOCK);
    }

    /** Whether the focus's spell may be cast from behind a shield: every branch is delivered unaimed from the body. */
    public static boolean castableBehindShield(ItemStack focus, RegistryAccess registries) {
        return DrumCastable.isCastable(focus, registries, GUARD_CASTABLE);
    }
}
