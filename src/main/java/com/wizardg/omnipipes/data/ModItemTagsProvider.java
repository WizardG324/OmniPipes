package com.wizardg.omnipipes.data;

import com.wizardg.omnipipes.OmniPipes;
import com.wizardg.omnipipes.block.ModBlocks;
import com.wizardg.omnipipes.item.ModItems;
import com.wizardg.omnipipes.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.neoforged.neoforge.common.Tags;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

import java.util.concurrent.CompletableFuture;

public class ModItemTagsProvider extends ItemTagsProvider {

    ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, OmniPipes.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        //Colored Pipes
        ModBlocks.COLORED_PIPES.values().forEach(pipe -> tag(ModTags.Items.COLORED_PIPES).add(pipe.get().asItem()));

        //All Pipes
        tag(ModTags.Items.PIPES)
                .add(ModBlocks.PIPE.get().asItem())
                .addTag(ModTags.Items.COLORED_PIPES);

        //Upgrades, which pipe upgrade slot accepts them
        ModItems.TIER_UPGRADES.forEach(tier -> tag(ModTags.Items.TIER_UPGRADES).add(tier.get()));
        tag(Tags.Items.TOOLS_WRENCH).add(ModItems.PIPE_CONFIGURATOR.get());
        tag(ModTags.Items.TIER_UPGRADES).add(ModItems.CREATIVE_UPGRADE.get());
        tag(ModTags.Items.FLUID_UPGRADE_COMPAT).add(ModItems.FLUID_UPGRADE.get());
        tag(ModTags.Items.ENERGY_UPGRADE_COMPAT).add(ModItems.ENERGY_UPGRADE.get());
        tag(ModTags.Items.RIFT_UPGRADE_COMPAT).add(ModItems.RIFT_UPGRADE.get());
        ModTags.Items.UPGRADE_COMPAT.forEach(type -> tag(ModTags.Items.TYPE_UPGRADES).addTag(type));
    }
}
