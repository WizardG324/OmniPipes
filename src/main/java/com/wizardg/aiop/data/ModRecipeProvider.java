package com.wizardg.aiop.data;

import com.wizardg.aiop.AIOPAllinOnePipe;
import com.wizardg.aiop.block.ModBlocks;
import com.wizardg.aiop.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(HolderLookup.Provider provider, RecipeOutput output) {
        super(provider, output);
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> provider) {
            super(packOutput, provider);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider provider, RecipeOutput recipeOutput) {
            return new ModRecipeProvider(provider, recipeOutput);
        }

        @Override
        public String getName() {
            return AIOPAllinOnePipe.MODID + " Recipes";
        }
    }

    @Override
    protected void buildRecipes() {
        //Pipe
        shaped(RecipeCategory.REDSTONE, ModBlocks.PIPE.get(), 16)
                .pattern("IDI")
                .pattern("BXB")
                .pattern("IDI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('D', Tags.Items.DUSTS_REDSTONE)
                .define('B', Tags.Items.STORAGE_BLOCKS_REDSTONE)
                .define('X', Tags.Items.STORAGE_BLOCKS_IRON)
                .group(AIOPAllinOnePipe.MODID)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(output);

        //Dyeing, 8 pipes of any color + dye
        ModBlocks.COLORED_PIPES.forEach((color, result) ->
            shapeless(RecipeCategory.REDSTONE, result.get(), 8)
                    .requires(tag(ModTags.Items.PIPES), 8)
                    .requires(color.getTag())
                    .group(AIOPAllinOnePipe.MODID + "_dye")
                    .unlockedBy("has_pipe", has(ModBlocks.PIPE.get()))
                    .save(output, AIOPAllinOnePipe.MODID + ":dye_" + result.getId().getPath()));

        //Washing, 8 dyed pipes + water bucket back to the base pipe (vanilla returns the empty bucket)
        shapeless(RecipeCategory.REDSTONE, ModBlocks.PIPE.get(), 8)
                .requires(tag(ModTags.Items.COLORED_PIPES), 8)
                .requires(Items.WATER_BUCKET)
                .group(AIOPAllinOnePipe.MODID + "_dye")
                .unlockedBy("has_pipe", has(ModBlocks.PIPE.get()))
                .save(output, AIOPAllinOnePipe.MODID + ":wash_pipe");
    }
}
