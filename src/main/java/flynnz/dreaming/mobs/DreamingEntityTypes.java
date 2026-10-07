package flynnz.dreaming.mobs;

import flynnz.dreaming.Dreaming;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class DreamingEntityTypes {
    public static final EntityType<WizardEntity> WIZARD = register(
            DreamingEntityTypeIds.WIZARD,
            EntityType.Builder.<WizardEntity>of(WizardEntity::new, MobCategory.MISC)
                    .sized(1f, 2f)
    );

    private static <T extends Entity> EntityType<T> register(
            ResourceKey<EntityType<?>> key, EntityType.Builder<T> builder
    ) {
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void registerModEntityTypes() {
        Dreaming.LOGGER.info("Registering EntityTypes for " + Dreaming.MOD_ID);
    }

    public static void registerAttributes() {
        FabricDefaultAttributeRegistry.register(WIZARD, WizardEntity.createWizardAttributes());
    }
}
