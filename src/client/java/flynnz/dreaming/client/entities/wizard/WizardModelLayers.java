package flynnz.dreaming.client.entities.wizard;

import flynnz.dreaming.Dreaming;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;

public class WizardModelLayers {
        public static final ModelLayerLocation WIZARD = createMain("wizard");

        private static ModelLayerLocation createMain(String name) {
            return new ModelLayerLocation(Dreaming.id(name), "main");
        }

        public static void registerModelLayers() {
            ModelLayerRegistry.registerModelLayer(WizardModelLayers.WIZARD, WizardModel::getTexturedModelData);
        }
}
