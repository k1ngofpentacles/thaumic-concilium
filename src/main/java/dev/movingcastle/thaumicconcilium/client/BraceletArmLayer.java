package dev.movingcastle.thaumicconcilium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.movingcastle.thaumicconcilium.item.CastingBracelet;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Third person: draws bracelets on the wrists. One held in the main hand goes on the main arm; one held in the off hand
 * (the shield slot) or worn in the Curios slot goes on the off arm; with both, the worn one, which is the one
 * that casts. A worn bracelet hidden with Curios' visibility
 * toggle isn't drawn. The held item itself is invisible (its item model scales to nothing in the hand), so only the band
 * shows, over the sleeve and under armour; a shield or other item in the off hand is drawn as usual.
 */
public final class BraceletArmLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public BraceletArmLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        HumanoidArm main = player.getMainArm();
        ItemStack mainStack = player.getMainHandItem();
        ItemStack offStack = player.getOffhandItem();
        if (mainStack.getItem() instanceof CastingBracelet) {
            draw(pose, buffers, light, player, main, mainStack);
        }
        // The off wrist shows the bracelet that casts: a worn one before one in the shield slot (BraceletCasting.worn).
        ItemStack off = visibleCurio(player);
        if (off.isEmpty() && offStack.getItem() instanceof CastingBracelet) {
            off = offStack;
        }
        if (!off.isEmpty()) {
            draw(pose, buffers, light, player, main.getOpposite(), off);
        }
    }

    private static ItemStack visibleCurio(AbstractClientPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(inv -> inv.findFirstCurio(stack -> stack.getItem() instanceof CastingBracelet))
                .filter(result -> result.slotContext().visible())
                .map(result -> result.stack()).orElse(ItemStack.EMPTY);
    }

    private void draw(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, HumanoidArm arm,
                      ItemStack stack) {
        CastingBracelet bracelet = (CastingBracelet) stack.getItem();
        PlayerModel<AbstractClientPlayer> model = this.getParentModel();
        pose.pushPose();
        (arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm).translateAndRotate(pose);
        BraceletBandRenderer.draw(pose, buffers, bracelet.kind(), arm, player.getSkin().model() == PlayerSkin.Model.SLIM,
                light, BraceletBandRenderer.gemColor(stack));
        pose.popPose();
    }
}
