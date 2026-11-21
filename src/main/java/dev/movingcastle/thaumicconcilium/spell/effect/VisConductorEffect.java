package dev.movingcastle.thaumicconcilium.spell.effect;

import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.content.entity.EntityFluxRift;
import com.leclowndu93150.thaumaturge.content.entity.WispEntity;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import com.mojang.serialization.MapCodec;
import dev.movingcastle.thaumicconcilium.block.DestabilizedCrystalBlockEntity;
import dev.movingcastle.thaumicconcilium.entity.ChargedWisp;
import dev.movingcastle.thaumicconcilium.registry.TCSpellBehaviors;
import dev.movingcastle.thaumicconcilium.rift.ConciliumRifts;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Vis Conductor, ported from the original focus. Channel it at a destabilized crystal cluster to draw its charge into
 * the focus ({@code 1 + potency} every 3 ticks, while within 8 blocks); let go:
 * <ul>
 *   <li>at nothing: the drained cluster bursts (an explosion of 2);</li>
 *   <li>with <b>Dematerialization</b>, at a creature: it takes the drained charge / (6 - potency) as damage; an auram
 *   cluster spent into a rift shrinks it by the drained charge x (1 + potency) (a rift shrunk past nothing simply
 *   closes, without its taint);</li>
 *   <li>with <b>Wisp Launcher</b> instead: channelling pulls the wisp you look at to you; let go within reach and it is
 *   flung as a {@link ChargedWisp}.</li>
 * </ul>
 * Settings: {@code potency} 0-3, {@code upgrade} (none / dematerialization / wisp launcher), {@code extend} 0-2 (the
 * charged wisp's lifetime).
 */
public final class VisConductorEffect extends AbstractEffectBehavior {
    public static final VisConductorEffect INSTANCE = new VisConductorEffect();
    public static final MapCodec<VisConductorEffect> CODEC = MapCodec.unit(INSTANCE);
    public static final int NONE = 0;
    public static final int DEMATERIALIZATION = 1;
    public static final int WISP_LAUNCHER = 2;

    private record Bound(BlockPos pos, ResourceLocation aspect, int amount, int potency, int upgrade) {
    }

    private record Held(int wispId, int potency, int extend) {
    }

    private static final Map<UUID, Bound> BOUND = new HashMap<>();
    private static final Map<UUID, Held> HELD = new HashMap<>();
    private static final Map<UUID, Long> LAST = new HashMap<>();

    private VisConductorEffect() {
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpellBehaviors.VIS_CONDUCTOR.get();
    }

    @Override
    protected boolean widens() {
        return false;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        if (!(ctx.caster() instanceof ServerPlayer player) || index > 0) {
            return;
        }
        ServerLevel level = ctx.level();
        int potency = ctx.setting("potency");
        int upgrade = ctx.setting("upgrade");
        // Original canApplyUpgrade: Dematerialization needs its research.
        if (upgrade == DEMATERIALIZATION && !com.leclowndu93150.thaumaturge.api.capability.PlayerResearchAccess.of(player)
                .hasCompleted(dev.movingcastle.thaumicconcilium.ThaumicConcilium.id("dematerialization"))) {
            upgrade = NONE;
        }
        if (upgrade == WISP_LAUNCHER) {
            pullWisp(level, player, potency, ctx.setting("extend"));
            return;
        }
        UUID id = player.getUUID();
        long now = level.getGameTime();
        long last = LAST.getOrDefault(id, now);
        LAST.put(id, now);
        Bound bound = BOUND.get(id);
        if (bound == null) {
            BlockHitResult hit = level.clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getLookAngle().scale(5.0)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.BLOCK && level.getBlockEntity(hit.getBlockPos()) instanceof DestabilizedCrystalBlockEntity crystal
                    && crystal.aspect() != null && crystal.capacity() > 0) {
                BOUND.put(id, new Bound(hit.getBlockPos(), crystal.aspect(), crystal.capacity(), potency, upgrade));
            }
            return;
        }
        if (player.distanceToSqr(Vec3.atCenterOf(bound.pos())) > 64.0
                || !(level.getBlockEntity(bound.pos()) instanceof DestabilizedCrystalBlockEntity crystal)
                || !bound.aspect().equals(crystal.aspect())) {
            BOUND.remove(id);
            player.stopUsingItem();
            return;
        }
        // Original: every 3 ticks, 1 + potency of the cluster's charge.
        int steps = (int) Math.max(1, (now - last) / 3);
        crystal.setCapacity(crystal.capacity() - steps * (1 + potency));
        if (now % 10 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.JACOBS.get(), SoundSource.PLAYERS, 0.5F,
                    1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F);
        }
        arc(level, Vec3.atCenterOf(bound.pos()), player.getEyePosition().subtract(0.0, 0.4, 0.0), crystal.color());
    }

    private static void pullWisp(ServerLevel level, ServerPlayer player, int potency, int extend) {
        Entity pointed = pointed(level, player, 32.0, 2.0, e -> e instanceof WispEntity);
        if (!(pointed instanceof WispEntity wisp)) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 to = new Vec3(player.getX() + look.x * 1.5, player.getY() + look.y * 1.5 + 1.0, player.getZ() + look.z * 1.5);
        wisp.setDeltaMovement(to.subtract(wisp.position()).normalize());
        HELD.put(player.getUUID(), new Held(wisp.getId(), potency, extend));
        if (level.getGameTime() % 10 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.JACOBS.get(), SoundSource.PLAYERS, 0.5F, 1.0F);
        }
    }

    /** The channel let go (from {@link dev.movingcastle.thaumicconcilium.spell.ChannelEvents}). */
    public static void released(Player player) {
        LAST.remove(player.getUUID());
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Held held = HELD.remove(player.getUUID());
        if (held != null) {
            Entity e = level.getEntity(held.wispId());
            if (e instanceof WispEntity wisp && wisp.distanceToSqr(player) < 6.0) {
                String aspect = wisp.aspect().unwrapKey().map(k -> k.location().toString()).orElse("thaumaturge:aer");
                level.addFreshEntity(ChargedWisp.launched(level, wisp.position(), player, aspect, held.extend(), held.potency()));
                wisp.discard();
            }
        }
        Bound bound = BOUND.remove(player.getUUID());
        if (bound == null || !(level.getBlockEntity(bound.pos()) instanceof DestabilizedCrystalBlockEntity crystal)) {
            return;
        }
        Entity look = pointed(level, player, 32.0, 1.1, e -> e instanceof LivingEntity || e instanceof EntityFluxRift);
        if (look == null) {
            // Original: let go at nothing, the drained cluster bursts.
            level.removeBlock(bound.pos(), false);
            boolean griefing = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
            level.explode(null, bound.pos().getX() + 0.5, bound.pos().getY() + 0.5, bound.pos().getZ() + 0.5, 2.0F,
                    griefing ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
            return;
        }
        if (bound.upgrade() != DEMATERIALIZATION) {
            return;
        }
        int drained = bound.amount() - crystal.capacity();
        if (look instanceof LivingEntity living) {
            living.hurt(level.damageSources().indirectMagic(player, player), (float) drained / (6.0F - bound.potency()));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.SHOCK.get(), SoundSource.PLAYERS, 0.8F,
                    level.random.nextFloat() * 0.1F + 0.9F);
            arc(level, look.position().add(0.0, 0.5, 0.0), player.position(), crystal.color());
        }
        if (look instanceof EntityFluxRift rift && bound.aspect().equals(ResourceLocation.fromNamespaceAndPath("thaumaturge", "auram"))) {
            ConciliumRifts.dematerialize(rift, rift.currentSize() - drained * (1 + bound.potency()));
            arc(level, rift.position(), player.position().add(0.0, 0.5, 0.0), crystal.color());
        }
    }

    @Nullable
    private static Entity pointed(ServerLevel level, Player player, double range, double width, java.util.function.Predicate<Entity> filter) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        AABB box = player.getBoundingBox().expandTowards(player.getLookAngle().scale(range)).inflate(width);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, end, box,
                e -> e != player && filter.test(e) && !e.isSpectator(), (float) width);
        return hit == null ? null : hit.getEntity();
    }

    private static void arc(ServerLevel level, Vec3 from, Vec3 to, int rgb) {
        DustParticleOptions dust = new DustParticleOptions(new Vector3f((rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F), 0.8F);
        int steps = Math.max(4, (int) (from.distanceTo(to) * 4));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.lerp(to, i / (double) steps);
            level.sendParticles(dust, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
        }
    }
}
