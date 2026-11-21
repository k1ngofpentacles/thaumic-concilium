package dev.movingcastle.thaumicconcilium.spell.effect;

import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.thaumicconcilium.network.FireworkBurstPayload;
import dev.movingcastle.thaumicconcilium.registry.TCSpellBehaviors;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Effect: the Signal focus's flare, a harmless firework burst so allies can find you.
 * <ul>
 *   <li>{@code color} 0-16: the original's 16 dye colours, then "chaos" (a random colour each cast).
 *   Replaces the old Shift+RMB colour cycling.</li>
 *   <li>{@code shape} 0-5: small ball, large ball, star, creeper, burst, random. Replaces the five shape upgrades.</li>
 *   <li>{@code flashbang} off/on: blinds everything within 10 blocks for 5 seconds (old Flashbang upgrade).</li>
 * </ul>
 * As in the original, 5% of bursts randomly twinkle or trail.
 */
public final class SignalEffect extends AbstractEffectBehavior {
    public static final MapCodec<SignalEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.floatRange(1.0F, 32.0F).optionalFieldOf("flash_radius", 10.0F).forGetter(SignalEffect::flashRadius)
    ).apply(i, SignalEffect::new));

    /** The original COLORS table (vanilla dye firework colours). */
    static final int[] COLORS = {
            11743532, 15435844, 14602026, 4312372, 3887386, 6719955, 2651799, 2437522,
            8073150, 12801229, 14188952, 15790320, 11250603, 4408131, 1973019, 5320730
    };
    private static final FireworkExplosion.Shape[] SHAPES = {
            FireworkExplosion.Shape.SMALL_BALL, FireworkExplosion.Shape.LARGE_BALL, FireworkExplosion.Shape.STAR,
            FireworkExplosion.Shape.CREEPER, FireworkExplosion.Shape.BURST
    };

    private final float flashRadius;

    public SignalEffect(float flashRadius) {
        this.flashRadius = flashRadius;
    }

    public float flashRadius() {
        return this.flashRadius;
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpellBehaviors.SIGNAL.get();
    }

    @Override
    protected boolean widens() {
        return false;
    }

    @Override
    protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
        ServerLevel level = ctx.level();
        RandomSource random = ctx.random();
        Vec3 at = target.position();

        int colorSetting = ctx.setting("color");
        int color = colorSetting >= COLORS.length ? COLORS[random.nextInt(COLORS.length)] : COLORS[colorSetting];
        int shapeSetting = ctx.setting("shape");
        FireworkExplosion.Shape shape = shapeSetting >= SHAPES.length ? SHAPES[random.nextInt(SHAPES.length)] : SHAPES[shapeSetting];
        boolean flair = random.nextDouble() < 0.05;
        boolean twinkle = flair && random.nextBoolean();
        boolean trail = flair && !twinkle;
        FireworkExplosion explosion = new FireworkExplosion(shape, IntList.of(color), IntList.of(), trail, twinkle);

        Vec3 motion = target.direction();
        PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(BlockPos.containing(at)),
                new FireworkBurstPayload(at, motion, List.of(explosion)));

        if (ctx.setting("flashbang") == 1) {
            for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(this.flashRadius))) {
                near.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 3));
            }
        }
    }
}
