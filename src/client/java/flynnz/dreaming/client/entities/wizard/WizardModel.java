package flynnz.dreaming.client.entities.wizard;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.VillagerLikeModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.npc.InventoryCarrier;
import org.jspecify.annotations.NonNull;

public class WizardModel
        extends EntityModel<WizardRenderState>
        implements HeadedModel,
        VillagerLikeModel<WizardRenderState>,
        ArmedModel<WizardRenderState>,
        InventoryCarrier {
    protected final ModelPart nose;
    private final ModelPart head;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart arms;

    public WizardModel(final ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.nose = this.head.getChild("nose");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.arms = root.getChild("arms");
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition mesh = VillagerModel.createBodyModel();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F), PartPose.ZERO);
        PartDefinition hat = head.addOrReplaceChild(
                "hat", CubeListBuilder.create().texOffs(0, 64).addBox(0.0F, 0.0F, 0.0F, 10.0F, 2.0F, 10.0F), PartPose.offset(-5.0F, -10.03125F, -5.0F)
        );
        PartDefinition hat2 = hat.addOrReplaceChild(
                "hat2",
                CubeListBuilder.create().texOffs(0, 76).addBox(0.0F, 0.0F, 0.0F, 7.0F, 4.0F, 7.0F),
                PartPose.offsetAndRotation(1.75F, -4.0F, 2.0F, -0.05235988F, 0.0F, 0.02617994F)
        );
        PartDefinition hat3 = hat2.addOrReplaceChild(
                "hat3",
                CubeListBuilder.create().texOffs(0, 87).addBox(0.0F, 0.0F, 0.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offsetAndRotation(1.75F, -4.0F, 2.0F, -0.10471976F, 0.0F, 0.05235988F)
        );
        hat3.addOrReplaceChild(
                "hat4",
                CubeListBuilder.create().texOffs(0, 95).addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.25F)),
                PartPose.offsetAndRotation(1.75F, -2.0F, 2.0F, (float) (-Math.PI / 15), 0.0F, 0.10471976F)
        );
        return LayerDefinition.create(mesh, 64, 128);
    }

    public void setupAnim(final @NonNull WizardRenderState state) {
        super.setupAnim(state);
        this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
        this.head.xRot = state.xRot * (float) (Math.PI / 180.0);
        this.rightLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.4F * state.walkAnimationSpeed * 0.5F;
        this.leftLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F + (float) Math.PI) * 1.4F * state.walkAnimationSpeed * 0.5F;
        float speed = 0.01F * (state.entityId % 10);
        this.nose.xRot = Mth.sin(state.ageInTicks * speed) * 4.5F * (float) (Math.PI / 180.0);
        this.nose.zRot = Mth.cos(state.ageInTicks * speed) * 2.5F * (float) (Math.PI / 180.0);
        if (state.isHoldingItem) {
            this.nose.setPos(0.0F, 1.0F, -1.5F);
            this.nose.xRot = -0.9F;
        }
    }

    @Override
    public @NonNull ModelPart getHead() {
        return this.head;
    }

    @Override
    public void translateToArms(final @NonNull WizardRenderState state, final @NonNull PoseStack outputPoseStack) {
        this.root.translateAndRotate(outputPoseStack);
        this.arms.translateAndRotate(outputPoseStack);
    }

    @Override
    public void translateToHand(@NonNull WizardRenderState state, @NonNull HumanoidArm arm, @NonNull PoseStack poseStack) {}

    @Override
    public @NonNull SimpleContainer getInventory() {
        // TODO: implement
        return new SimpleContainer(1);
    }
}
