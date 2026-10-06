package flynnz.dreaming;

import flynnz.dreaming.items.WandItem;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public class DreamingItems {
    public static Item register(
            ResourceKey<Item> itemKey,
            Function<Item.Properties, Item> itemFactory,
            Item.Properties settings
    ) {
        final Item item = itemFactory.apply(settings.setId(itemKey));
        Registry.register(
                BuiltInRegistries.ITEM, itemKey, item
        );
        return item;
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(
                CreativeModeTabs.COMBAT)
                .register( creativeTab -> creativeTab.accept(DreamingItems.WAND_ITEM)
        );
    }

    public static final Item WAND_ITEM = register(
            DreamingItemIds.WAND_ITEM,
            WandItem::new,
            new Item.Properties().stacksTo(1)
    );
}
