package dev.movingcastle.thaumicconcilium.spell.delivery;

import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellStats;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.mojang.serialization.MapCodec;
import dev.movingcastle.thaumicconcilium.registry.TCSpellBehaviors;
import dev.movingcastle.thaumicconcilium.spell.BraceletCasting;
import dev.movingcastle.thaumicconcilium.spell.Riposte;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

/**
 * Delivery: the Riposte focus, Thaumaturge's {@code mine} with a raised shield as the trigger. Casting pays the whole spell and
 * arms it in the caster ({@link Riposte#arm}) for the {@code window} setting (5, 10, 15 or 20 seconds per step, times the
 * duration modifiers); the next hit the shield blocks inside the parry window releases the children on the attacker at power
 * x1.5. The incoming targets are ignored: it always arms the caster. Stores the continuation exactly as
 * {@code AbstractCarrierDelivery.execute} does for mines and walls ({@code ctx.continuation()}).
 */
public final class RiposteDelivery implements SpellBehavior {
    public static final MapCodec<RiposteDelivery> CODEC = MapCodec.unit(new RiposteDelivery());
    private static final int SECONDS_PER_STEP = 5;

    @Override
    public SpellBehaviorType<?> type() {
        return TCSpellBehaviors.RIPOSTE.get();
    }

    @Override
    public void execute(CastContext ctx, List<SpellTarget> incoming) {
        if (ctx.caster() instanceof ServerPlayer player && ctx.hasNext()) {
            int ticks = Math.round(Math.max(1, ctx.setting("window")) * SECONDS_PER_STEP * 20 * ctx.state().get(SpellStats.DURATION));
            Riposte.arm(player, ctx.continuation(), ticks, BraceletCasting.volleyToken());
        }
    }

    @Override
    public boolean standalone() {
        return false;
    }
}
