package com.wizardg.aiop.data;

import com.wizardg.aiop.AIOPAllinOnePipe;
import com.wizardg.aiop.block.ModBlocks;
import com.wizardg.aiop.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

import java.util.concurrent.CompletableFuture;

public class ModItemTagsProvider extends ItemTagsProvider {

    ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, AIOPAllinOnePipe.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        //Colored Pipes
        ModBlocks.COLORED_PIPES.values().forEach(pipe -> tag(ModTags.Items.COLORED_PIPES).add(pipe.get().asItem()));

        //All Pipes
        tag(ModTags.Items.PIPES)
                .add(ModBlocks.PIPE.get().asItem())
                .addTag(ModTags.Items.COLORED_PIPES);
    }
}
