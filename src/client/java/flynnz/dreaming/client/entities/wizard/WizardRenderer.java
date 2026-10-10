package flynnz.dreaming.client.entities.wizard;

import com.mojang.blaze3d.vertex.PoseStack;
import flynnz.dreaming.client.entities.DreamingEntitiesModelLayers;
import flynnz.dreaming.entities.wizard.Wizard;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

public class WizardRenderer extends MobRenderer<Wizard, WizardRenderState, WizardModel> {
    private static final Identifier WIZARD_LOCATION = Identifier.fromNamespaceAndPath("dreaming", "textures/entity/wizard.png");

    public WizardRenderer(final EntityRendererProvider.Context context) {
        super(context, new WizardModel(context.bakeLayer(DreamingEntitiesModelLayers.WIZARD)), 0.375F);
        this.addLayer(new WizardItemInHandLayer(this));
    }

    public void extractRenderState(final @NonNull Wizard entity, final @NonNull WizardRenderState state, final float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        ArmedEntityRenderState.extractArmedEntityRenderState(entity, state, this.itemModelResolver, partialTicks);
        ItemStack mainHandItem = entity.getMainHandItem();
        state.isHoldingItem = !mainHandItem.isEmpty();
        state.isCastingSpell = entity.isCastingSpell();
        state.mainArm = entity.getMainArm();
        state.ticksUsingItem = entity.getTicksUsingItem(partialTicks);
        state.armPose = entity.getArmPose();
    }

    public @NonNull WizardRenderState createRenderState() {
        return new WizardRenderState();
    }

    @Override
    public @NonNull Identifier getTextureLocation(@NonNull WizardRenderState state) {
        return WIZARD_LOCATION;
    }

    private static class WizardItemInHandLayer extends ItemInHandLayer<WizardRenderState, WizardModel> {
        public WizardItemInHandLayer(RenderLayerParent<WizardRenderState, WizardModel> renderer) {
            super(renderer);
        }

        public void submit(
                final @NonNull PoseStack poseStack,
                final @NonNull SubmitNodeCollector submitNodeCollector,
                final int lightCords,
                final @NonNull WizardRenderState state,
                final float yRot,
                final float xRot
        ) {
            if (state.armPose != Wizard.WizardArmPose.CROSSED) {
                this.submitArmWithItem(state, state.rightHandItemState, state.rightHandItemStack, HumanoidArm.RIGHT, poseStack, submitNodeCollector, lightCords);
                this.submitArmWithItem(state, state.leftHandItemState, state.leftHandItemStack, HumanoidArm.LEFT, poseStack, submitNodeCollector, lightCords);
            }
        }
    }
}

