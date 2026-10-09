package flynnz.dreaming.client;

import flynnz.dreaming.client.entities.DreamingEntitiesModelLayers;
import flynnz.dreaming.client.entities.wizard.WizardRenderer;
import flynnz.dreaming.entities.DreamingEntityTypes;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

public class DreamingClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		DreamingEntitiesModelLayers.registerModelLayers();
		EntityRenderers.register(DreamingEntityTypes.WIZARD, WizardRenderer::new);
	}
}