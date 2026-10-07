package flynnz.dreaming.mobs;

import flynnz.dreaming.Dreaming;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;

public class DreamingEntityTypeIds {
    public static ResourceKey<EntityType<?>> WIZARD = create("wizard");

    private static ResourceKey<EntityType<?>> create(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Dreaming.id(name));
    }
}
