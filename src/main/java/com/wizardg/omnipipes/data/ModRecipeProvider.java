package com.wizardg.omnipipes.data;

import com.wizardg.omnipipes.OmniPipes;
import com.wizardg.omnipipes.block.ModBlocks;
import com.wizardg.omnipipes.item.ModItems;
import com.wizardg.omnipipes.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
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
            return OmniPipes.MODID + " Recipes";
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
                .group(OmniPipes.MODID)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(output);

        //Tier upgrades, each built around the one before (tier 1 around a pipe)
        shaped(RecipeCategory.MISC, ModItems.TIER_UPGRADES.get(0).get())
                .pattern("IRI").pattern("RPR").pattern("IRI")
                .define('I', Tags.Items.INGOTS_IRON).define('R', Tags.Items.DUSTS_REDSTONE)
                .define('P', tag(ModTags.Items.PIPES))
                .group(OmniPipes.MODID + "_upgrade")
                .unlockedBy("has_pipe", has(ModTags.Items.PIPES))
                .save(output);
        tierUpgrade(1, tag(Tags.Items.INGOTS_GOLD), Tags.Items.DUSTS_REDSTONE);
        tierUpgrade(2, tag(Tags.Items.GEMS_DIAMOND), Tags.Items.STORAGE_BLOCKS_REDSTONE);
        shaped(RecipeCategory.MISC, ModItems.TIER_UPGRADES.get(3).get())
                .pattern("CGC").pattern("RUR").pattern("CGC")
                .define('C', Items.NETHERITE_SCRAP).define('G', Tags.Items.INGOTS_GOLD)
                .define('R', Tags.Items.STORAGE_BLOCKS_REDSTONE).define('U', ModItems.TIER_UPGRADES.get(2).get())
                .group(OmniPipes.MODID + "_upgrade")
                .unlockedBy("has_previous_tier", has(ModItems.TIER_UPGRADES.get(2).get()))
                .save(output);

        //Type upgrades, the unlocking item ringed by iron
        shaped(RecipeCategory.MISC, ModItems.FLUID_UPGRADE.get())
                .pattern("III")
                .pattern("IBI")
                .pattern("III")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('B', Items.BUCKET)
                .group(OmniPipes.MODID + "_upgrade")
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(output);

        shaped(RecipeCategory.MISC, ModItems.ENERGY_UPGRADE.get())
                .pattern("III")
                .pattern("IRI")
                .pattern("III")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('R', Tags.Items.STORAGE_BLOCKS_REDSTONE)
                .group(OmniPipes.MODID + "_upgrade")
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(output);

        //Pipe Configurator, a wrench with a redstone head
        shaped(RecipeCategory.TOOLS, ModItems.PIPE_CONFIGURATOR.get())
                .pattern("I I")
                .pattern(" R ")
                .pattern(" I ")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_pipe", has(ModTags.Items.PIPES))
                .save(output);

        //Tag Filter, paper ringed by 4 iron
        shaped(RecipeCategory.MISC, ModItems.TAG_FILTER.get())
                .pattern(" I ")
                .pattern("IPI")
                .pattern(" I ")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('P', Items.PAPER)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(output);

        //Dyeing, 8 pipes of any color + dye
        ModBlocks.COLORED_PIPES.forEach((color, result) ->
            shapeless(RecipeCategory.REDSTONE, result.get(), 8)
                    .requires(tag(ModTags.Items.PIPES), 8)
                    .requires(color.getTag())
                    .group(OmniPipes.MODID + "_dye")
                    .unlockedBy("has_pipe", has(ModBlocks.PIPE.get()))
                    .save(output, OmniPipes.MODID + ":dye_" + result.getId().getPath()));

        //Washing, 8 dyed pipes + water bucket back to the base pipe (vanilla returns the empty bucket)
        shapeless(RecipeCategory.REDSTONE, ModBlocks.PIPE.get(), 8)
                .requires(tag(ModTags.Items.COLORED_PIPES), 8)
                .requires(Items.WATER_BUCKET)
                .group(OmniPipes.MODID + "_dye")
                .unlockedBy("has_pipe", has(ModBlocks.PIPE.get()))
                .save(output, OmniPipes.MODID + ":wash_pipe");
    }

    // Corners of the tier's material, redstone on the edges, previous tier in the middle.
    private void tierUpgrade(int tier, Ingredient corner, TagKey<Item> redstone) {        Item previous = ModItems.TIER_UPGRADES.get(tier - 1).get();
        shaped(RecipeCategory.MISC, ModItems.TIER_UPGRADES.get(tier).get())
                .pattern("CRC").pattern("RUR").pattern("CRC")
                .define('C', corner).define('R', redstone).define('U', previous)
                .group(OmniPipes.MODID + "_upgrade")
                .unlockedBy("has_previous_tier", has(previous))
                .save(output);
    }
}
