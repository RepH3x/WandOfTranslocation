package com.reph3x.wot.component;

import com.mojang.serialization.Codec;
import com.reph3x.wot.WandOfTranslocation;
import net.minecraft.component.ComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemStackSet;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

import java.util.List;

public class WandOfTranslocationComponents {

    public static final ComponentType<Boolean> WAND_IS_FULL_COMPONENT = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(WandOfTranslocation.MOD_ID, "wand_is_full"),
            ComponentType.<Boolean>builder().codec(Codec.BOOL).build()
    );

    public static final ComponentType<Boolean> WAND_INVENTORY_IS_BARREL_COMPONENT = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(WandOfTranslocation.MOD_ID, "wand_inventory_is_barrel"),
            ComponentType.<Boolean>builder().codec(Codec.BOOL).build()
    );

    public static final ComponentType<List<ItemStack>> WAND_INVENTORY_COMPONENT = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(WandOfTranslocation.MOD_ID, "wand_inventory"),
            ComponentType.<List<ItemStack>>builder().codec(ItemStack.CODEC.listOf()).build()
    );

    public static void initialize() {};
}
