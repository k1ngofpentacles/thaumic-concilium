package dev.movingcastle.thaumicconcilium.entity.mob;

import dev.movingcastle.thaumicconcilium.compat.witchery.ImpPact;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

/**
 * Original {@code ImpDealTargetAI}: the nearest player as a target, unless the player has a pact with an imp or hit this mob
 * (then the mob lets them be; a Chort says so, at most once in 1200 ticks). Players farther than the follow range are not
 * looked at (the original scanned a box of that size), so the line is never said to someone far away.
 */
public final class ImpDealTargetGoal extends NearestAttackableTargetGoal<Player> {
    private final Mob attacker;

    public ImpDealTargetGoal(Mob mob) {
        // chance 0: every check, as the original's 0; must see the target
        super(mob, Player.class, 0, true, false, null);
        this.attacker = mob;
        this.targetConditions.selector(this::allows);
    }

    public boolean allows(LivingEntity target) {
        if (!(target instanceof Player player)) {
            return false;
        }
        double range = this.attacker.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (this.attacker.distanceToSqr(player) > range * range) {
            return false;
        }
        boolean pact = ImpPact.has(player);
        boolean attackedMob = this.attacker.getLastHurtByMob() == player;
        if (!pact && !attackedMob) {
            return true;
        }
        if (pact && this.attacker instanceof Chort chort && chort.chatCooldown == 0) {
            player.sendSystemMessage(Component.translatable("thaumicconcilium.chort.has_pact").withStyle(ChatFormatting.DARK_RED));
            chort.chatCooldown = Chort.PACT_LINE_COOLDOWN;
        }
        return false;
    }
}
