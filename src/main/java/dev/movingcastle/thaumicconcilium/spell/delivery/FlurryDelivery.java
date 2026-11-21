package dev.movingcastle.thaumicconcilium.spell.delivery;

import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellStats;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.thaumicconcilium.entity.FlurryShot;
import dev.movingcastle.thaumicconcilium.registry.TCSpellBehaviors;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;

/**
 * Delivery: the Cooling focus. Each pulse throws one snowball, or with {@code shot} = frost shard (old
 * Refreeze upgrade) a fragile 1-damage ice shard at 1.5 speed. Meant for channelled casting, where Thaumaturge's
 * pulse interval sets the fire rate. Child parts, if any, resume where each shot lands.
 */
public record FlurryDelivery(float snowballSpeed, float shardSpeed) implements SpellBehavior {
    public static final MapCodec<FlurryDelivery> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.floatRange(0.1F, 4.0F).optionalFieldOf("snowball_speed", 1.5F).forGetter(FlurryDelivery::snowballSpeed),
            Codec.floatRange(0.1F, 4.0F).optionalFieldOf("shard_speed", 1.5F).forGetter(FlurryDelivery::shardSpeed)
    ).apply(i, FlurryDelivery::new));

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpellBehaviors.FLURRY.get();
    }

    @Override
    public void execute(CastContext ctx, List<SpellTarget> incoming) {
        LivingEntity caster = ctx.caster();
        if (caster == null) {
            return;
        }
        boolean shard = ctx.setting("shot") == 1;
        float speed = (shard ? this.shardSpeed : this.snowballSpeed) * ctx.state().get(SpellStats.SPEED);
        var payload = ctx.hasNext() ? Optional.of(ctx.continuation()) : Optional.<com.leclowndu93150.thaumaturge.api.spell.cast.SpellContinuation>empty();
        for (SpellTarget origin : incoming) {
            FlurryShot.throwFrom(ctx.level(), caster, origin.position(), origin.direction(), shard, speed, payload);
        }
    }
}
