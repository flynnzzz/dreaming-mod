package flynnz.dreaming;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class DreamingItemIds {

    public static ResourceKey<Item> create(String name) {
        return ResourceKey.create(Registries.ITEM, Dreaming.id(name));
    }

    public static final ResourceKey<Item> WAND_ITEM = create("wand");
}
