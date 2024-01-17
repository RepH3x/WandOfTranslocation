package com.reph3x.wot.item;

import com.reph3x.wot.WandOfTranslocation;
import com.reph3x.wot.item.custom.WandOfTranslocationItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {

    public static final Item WAND_OF_TRANSLOCATION = registerItem("wandoftranslocation", new WandOfTranslocationItem(new FabricItemSettings().maxCount(1)));

    private static void addItemsToToolTabItemGroup(FabricItemGroupEntries entries) {
        entries.add(WAND_OF_TRANSLOCATION);
    }

    public static void registerModItems() {
        WandOfTranslocation.LOGGER.info("Registering Mod Items for " + WandOfTranslocation.MOD_ID);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(ModItems::addItemsToToolTabItemGroup);
    }

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(WandOfTranslocation.MOD_ID, name), item);
    }
}
