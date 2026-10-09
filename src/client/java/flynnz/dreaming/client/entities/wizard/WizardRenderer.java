package flynnz.dreaming.client.entities.wizard;

import flynnz.dreaming.client.entities.DreamingEntitiesModelLayers;
import flynnz.dreaming.entities.wizard.Wizard;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

public class WizardRenderer extends MobRenderer<Wizard, WizardRenderState, WizardModel> {
    private static final Identifier WIZARD_LOCATION = Identifier.fromNamespaceAndPath("dreaming", "textures/entity/wizard.png");

    public WizardRenderer(final EntityRendererProvider.Context context) {
        super(context, new WizardModel(context.bakeLayer(DreamingEntitiesModelLayers.WIZARD)), 0.375F);
    }

    public void extractRenderState(final @NonNull Wizard entity, final @NonNull WizardRenderState state, final float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        ArmedEntityRenderState.extractArmedEntityRenderState(entity, state, this.itemModelResolver, partialTicks);
        ItemStack mainHandItem = entity.getMainHandItem();
        state.isHoldingItem = !mainHandItem.isEmpty();
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
}
