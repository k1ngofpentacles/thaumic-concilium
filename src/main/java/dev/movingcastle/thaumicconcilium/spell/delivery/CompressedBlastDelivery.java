package dev.movingcastle.thaumicconcilium.spell.delivery;

import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellStats;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.movingcastle.thaumicconcilium.entity.CompressedBlastEntity;
import dev.movingcastle.thaumicconcilium.registry.TCSpellBehaviors;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;

/**
 * Delivery: launches a {@link CompressedBlastEntity} that flies straight and hands the rest of the spell its
 * detonation point when the fuse runs out.
 * <p>
 * Settings (replacing focus upgrades):
 * <ul>
 *   <li>{@code fuse} 0-3: +10 ticks per step (the old "Extend" upgrade). Scaled by Thaumaturge's duration stat.</li>
 *   <li>{@code draw} none/vacuum: the old Impulse "Vacuum" upgrade, dragging mobs into the blast as it flies.</li>
 * </ul>
 */
public record CompressedBlastDelivery(float speed, int baseFuse, int fusePerStep, int fuseJitter) implements SpellBehavior {
    public static final MapCodec<CompressedBlastDelivery> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.floatRange(0.1F, 4.0F).optionalFieldOf("speed", 1.0F).forGetter(CompressedBlastDelivery::speed),
            Codec.intRange(0, 200).optionalFieldOf("base_fuse", 5).forGetter(CompressedBlastDelivery::baseFuse),
            Codec.intRange(0, 100).optionalFieldOf("fuse_per_step", 10).forGetter(CompressedBlastDelivery::fusePerStep),
            Codec.intRange(0, 100).optionalFieldOf("fuse_jitter", 20).forGetter(CompressedBlastDelivery::fuseJitter)
    ).apply(i, CompressedBlastDelivery::new));

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpellBehaviors.COMPRESSED_BLAST.get();
    }

    @Override
    public void execute(CastContext ctx, List<SpellTarget> incoming) {
        LivingEntity caster = ctx.caster();
        if (caster == null) {
            return;
        }
        float duration = Math.max(0.25F, ctx.state().get(SpellStats.DURATION));
        float velocity = this.speed * ctx.state().get(SpellStats.SPEED);
        boolean vacuum = ctx.setting("draw") == 1;
        for (SpellTarget origin : incoming) {
            int fuse = Math.round((this.baseFuse + this.fusePerStep * ctx.setting("fuse")) * duration);
            if (this.fuseJitter > 0) {
                fuse += ctx.random().nextInt(this.fuseJitter);
            }
            CompressedBlastEntity.launch(ctx.level(), caster, ctx.continuation(), origin, velocity, fuse, vacuum);
        }
    }

    @Override
    public boolean standalone() {
        return false;
    }
}
