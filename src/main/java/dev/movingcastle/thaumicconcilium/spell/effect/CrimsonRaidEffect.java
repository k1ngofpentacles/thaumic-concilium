package dev.movingcastle.thaumicconcilium.spell.effect;

import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.mojang.serialization.MapCodec;
import dev.movingcastle.thaumicconcilium.entity.mob.LesserCrimsonPortal;
import dev.movingcastle.thaumicconcilium.item.PontifexRobe;
import dev.movingcastle.thaumicconcilium.registry.TCEntities;
import dev.movingcastle.thaumicconcilium.registry.TCSpellBehaviors;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Crimson Raid (original: the Ars Magica 2 component {@code CrimsonRaid}). At the point the spell lands, a caster wearing the
 * full Pontifex Robe who has no raid portal open opens a Lesser Crimson Portal at stage 0 that is theirs and lasts 400 ticks;
 * anyone else casts it to no effect (the cast still spends). A target that is itself a raid portal is not an impact.
 */
public final class CrimsonRaidEffect extends AbstractEffectBehavior {
    public static final CrimsonRaidEffect INSTANCE = new CrimsonRaidEffect();
    public static final MapCodec<CrimsonRaidEffect> CODEC = MapCodec.unit(INSTANCE);

    private CrimsonRaidEffect() {
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpellBehaviors.CRIMSON_RAID.get();
    }

    @Override
    protected boolean widens() {
        return false;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        if (index > 0 || !(ctx.caster() instanceof ServerPlayer player)) {
            return;
        }
        Entity hit = target.entity().orElse(null);
        if (hit instanceof LesserCrimsonPortal portal && portal.owner() != null) {
            return;
        }
        raid(ctx.level(), player, target.position());
    }

    /** The raid itself: the new portal, or null when the caster lacks the robe or already has a live raid portal. */
    @Nullable
    public static LesserCrimsonPortal raid(ServerLevel level, ServerPlayer caster, Vec3 at) {
        if (!PontifexRobe.isFullSet(caster) || hasLivePortal(level, caster)) {
            return null;
        }
        LesserCrimsonPortal portal = LesserCrimsonPortal.summon(level, at, false);
        if (portal != null) {
            portal.setStage(0);
            portal.setOwner(caster.getUUID(), LesserCrimsonPortal.RAID_LIFETIME);
        }
        return portal;
    }

    /** True while one of the caster's raid portals stands in the world. */
    public static boolean hasLivePortal(ServerLevel level, ServerPlayer caster) {
        return !level.getEntities(TCEntities.LESSER_CRIMSON_PORTAL.get(),
                p -> p.isAlive() && caster.getUUID().equals(p.owner())).isEmpty();
    }
}
