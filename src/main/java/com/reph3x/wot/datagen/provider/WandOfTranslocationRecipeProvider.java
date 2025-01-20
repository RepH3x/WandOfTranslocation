package com.reph3x.wot.datagen.provider;

import com.reph3x.wot.WandOfTranslocationItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.recipe.RecipeExporter;
import net.minecraft.data.recipe.RecipeGenerator;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

public class WandOfTranslocationRecipeProvider extends FabricRecipeProvider {
    public WandOfTranslocationRecipeProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected RecipeGenerator getRecipeGenerator(RegistryWrapper.WrapperLookup registryLookup, RecipeExporter recipeExporter) {
        return new RecipeGenerator(registryLookup, recipeExporter) {
            @Override
            public void generate() {
                RegistryWrapper.Impl<Item> itemLookup = registries.getOrThrow(RegistryKeys.ITEM);

                createShaped(RecipeCategory.TOOLS, WandOfTranslocationItems.WAND_OF_TRANSLOCATION)
                        .pattern("  1")
                        .pattern(" 2 ")
                        .pattern("2  ")
                        .input('1', Items.ENDER_PEARL)
                        .input('2', Items.IRON_INGOT)
                        .group("tools")
                        .criterion(hasItem(Items.CRAFTING_TABLE), conditionsFromItem(Items.CRAFTING_TABLE))
                        .offerTo(recipeExporter);
            }
        };
    }

    @Override
    public String getName() {
        return "WandOfTranslocationRecipeProvider";
    }
}
