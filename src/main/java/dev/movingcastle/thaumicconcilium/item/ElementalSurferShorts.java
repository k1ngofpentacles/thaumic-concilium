package dev.movingcastle.thaumicconcilium.item;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.entity.EntityGolemOrb;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import com.leclowndu93150.thaumaturge.registry.TTSounds;
import dev.movingcastle.thaumicconcilium.entity.FlurryShot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Elemental Surfer Shorts (creative-only, as in the original, which gives them no recipe). While the wearer sneaks,
 * each effect is paid in vis from a wand on the hotbar (2 vis each, the original's 200 centivis):
 * <ul>
 *   <li>every tick, order and entropy: the wearer's own projectiles within 4 blocks are flung out along their flight;</li>
 *   <li>every 7th tick, fire: eight embers scatter around the wearer; water: eight fragile frost shards;</li>
 *   <li>otherwise every 13th tick, air: a golem orb at every creature within 7 blocks.</li>
 * </ul>
 */
public class ElementalSurferShorts extends ArmorItem {
    private static final int COST = 200;   // centivis

    public ElementalSurferShorts(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties.rarity(Rarity.EPIC));
    }

    @SafeVarargs
    private static boolean pay(Player player, ResourceKey<IAspect>... aspects) {
        java.util.HashMap<ResourceKey<IAspect>, Integer> cost = new java.util.HashMap<>();
        for (ResourceKey<IAspect> a : aspects) {
            cost.put(a, COST);
        }
        return WandVisHelper.consumeSpecificFromHotbar(player, Map.copyOf(cost), true);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof Player player) || !player.isShiftKeyDown()
                || player.getItemBySlot(EquipmentSlot.LEGS) != stack) {
            return;
        }
        if (pay(player, TTAspects.ORDO, TTAspects.PERDITIO)) {
            for (Projectile p : server.getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(4.0),
                    p -> p.getOwner() == player)) {
                Vec3 motion = p.getDeltaMovement();
                if (motion.lengthSqr() > 1.0E-6) {
                    p.setPos(p.position().add(motion.normalize().scale(p.distanceTo(player))));
                }
            }
        }
        RandomSource random = player.getRandom();
        if (player.tickCount % 7 == 0) {
            if (pay(player, TTAspects.IGNIS)) {
                for (int i = 0; i < 8; i++) {
                    Vec3 dir = new Vec3(-0.5 + random.nextDouble(), random.nextDouble(), -0.5 + random.nextDouble());
                    SmallFireball ember = new SmallFireball(server, player, dir.normalize());
                    ember.setPos(player.getX() + dir.x * 2.0, player.getEyeY() - 0.4 + dir.y, player.getZ() + dir.z * 2.0);
                    server.addFreshEntity(ember);
                }
            }
            if (pay(player, TTAspects.AQUA)) {
                for (int i = 0; i < 8; i++) {
                    Vec3 dir = new Vec3(-0.5 + random.nextDouble(), 2.0 * random.nextDouble(), -0.5 + random.nextDouble());
                    Vec3 at = player.position().add(dir.x * 2.0, 1.0 + player.getBbHeight() * 0.5, dir.z * 2.0);
                    FlurryShot.throwFrom(server, player, at, dir, true, 0.8F, Optional.empty());
                }
            }
            return;
        }
        if (player.tickCount % 13 == 0 && pay(player, TTAspects.AER)) {
            List<LivingEntity> around = new ArrayList<>(server.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(7.0), e -> e != player && e.isAlive()));
            Collections.shuffle(around);
            Vec3 look = player.getLookAngle();
            for (LivingEntity target : around) {
                EntityGolemOrb orb = new EntityGolemOrb(server, player, target, false);
                orb.setPos(orb.getX() + look.x, orb.getY() + 0.5, orb.getZ() + look.z);
                double dx = target.getX() + target.getDeltaMovement().x - player.getX();
                double dy = target.getY() - player.getY() - target.getBbHeight() / 2.0;
                double dz = target.getZ() + target.getDeltaMovement().z - player.getZ();
                orb.shoot(dx, dy, dz, 0.66F, 5.0F);
                server.addFreshEntity(orb);
            }
            if (!around.isEmpty()) {
                server.playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.EGATTACK.get(),
                        player.getSoundSource(), 1.0F, 1.0F + random.nextFloat() * 0.1F);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.thaumicconcilium.elemental_surfer_shorts.desc").withStyle(ChatFormatting.GOLD));
    }
}
