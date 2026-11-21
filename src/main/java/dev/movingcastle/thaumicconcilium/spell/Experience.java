package dev.movingcastle.thaumicconcilium.spell;

import net.minecraft.world.entity.player.Player;

/** Experience-point arithmetic (vanilla formulas), for draining exact amounts of experience. */
public final class Experience {
    private Experience() {
    }

    /** Experience needed to go from level 0 to {@code level}. */
    public static int forLevel(int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        }
        if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        }
        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }

    public static int total(Player player) {
        return forLevel(player.experienceLevel) + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
    }

    /** Removes {@code amount} points if the player has them; returns whether it did. */
    public static boolean drain(Player player, int amount) {
        int total = total(player);
        if (total < amount) {
            return false;
        }
        int left = total - amount;
        int level = 0;
        while (forLevel(level + 1) <= left) {
            level++;
        }
        player.experienceLevel = level;
        player.totalExperience = left;
        int needed = player.getXpNeededForNextLevel();
        player.experienceProgress = needed > 0 ? (float) (left - forLevel(level)) / needed : 0.0F;
        // Re-send the bar: giveExperiencePoints(0) marks the player's experience dirty for the client.
        player.giveExperiencePoints(0);
        return true;
    }
}
