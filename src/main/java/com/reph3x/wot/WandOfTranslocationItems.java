package com.reph3x.wot;

import com.reph3x.wot.custom.WandOfTranslocationItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class WandOfTranslocationItems {

    public static final RegistryKey<Item> WAND_OF_TRANSLOCATION_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(WandOfTranslocation.MOD_ID, "wand_of_translocation"));
    public static final Item WAND_OF_TRANSLOCATION = register(
            new WandOfTranslocationItem(new Item.Settings().registryKey(WAND_OF_TRANSLOCATION_KEY).maxCount(1)),
            WAND_OF_TRANSLOCATION_KEY
    );

    public static Item register(Item item, RegistryKey<Item> registryKey) {
        Item registeredItem = Registry.register(Registries.ITEM, registryKey.getValue(), item);
        return registeredItem;
    }

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
                .register((itemGroup) -> itemGroup.add(WandOfTranslocationItems.WAND_OF_TRANSLOCATION));
    }
}
