package flynnz.dreaming.client.entities.wizard;

import flynnz.dreaming.entities.wizard.Wizard;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

public class WizardRenderer extends MobRenderer<Wizard, WizardRenderState, WizardModel> {
    private static final Identifier WIZARD_LOCATION = Identifier.fromNamespaceAndPath("dreaming", "textures/entity/wizard.png");

    public WizardRenderer(final EntityRendererProvider.Context context) {
        super(context, new WizardModel(context.bakeLayer(WizardModelLayers.WIZARD)), 0.375F);
//        this.addLayer(new ItemInHandLayer<WizardRenderState, WizardModel>(this));
    }

    public @NonNull WizardRenderState createRenderState() {
        return new WizardRenderState();
    }

    public void extractRenderState(final @NonNull Wizard entity, final @NonNull WizardRenderState state, final float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        // TODO: understand
        ArmedEntityRenderState.extractArmedEntityRenderState(entity, state, this.itemModelResolver, 1.0f);
        state.entityId = entity.getId();
        ItemStack mainHandItem = entity.getMainHandItem();
        state.isHoldingItem = !mainHandItem.isEmpty();
    }

    @Override
    public @NonNull Identifier getTextureLocation(@NonNull WizardRenderState state) {
        return WIZARD_LOCATION;
    }
}
