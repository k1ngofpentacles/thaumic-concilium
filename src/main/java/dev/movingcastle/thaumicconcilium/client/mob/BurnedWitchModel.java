package dev.movingcastle.thaumicconcilium.client.mob;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;

/**
 * Original BurnedWitchModel: the 1.7 biped always in the seat pose (she sits on a broom), arms still swinging with the walk cycle.
 * The broom is drawn by {@link BurnedWitchRenderer}. Look memo: docs/design/phase10-look-prebrief.md 0, 3.
 */
public class BurnedWitchModel<T extends LivingEntity> extends HumanoidModel<T> {
    public BurnedWitchModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.riding = false; // 1.21's riding pose differs from the original's: the seat pose below replaces it
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        ChortModel.seatPose(this);
    }
}
