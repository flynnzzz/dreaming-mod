package flynnz.dreaming.client.entities.wizard;

import flynnz.dreaming.entities.wizard.Wizard;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.HumanoidArm;

public class WizardRenderState extends HumanoidRenderState {
    public boolean isHoldingItem;
    public Wizard.WizardArmPose armPose = Wizard.WizardArmPose.CROSSED;
    public HumanoidArm mainArm = HumanoidArm.RIGHT;
}
