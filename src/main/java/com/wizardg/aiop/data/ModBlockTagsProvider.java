package com.wizardg.aiop.data;

import com.wizardg.aiop.AIOPAllinOnePipe;
import com.wizardg.aiop.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagsProvider extends BlockTagsProvider {

    ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, AIOPAllinOnePipe.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        ModBlocks.ALL_PIPES.forEach(pipe -> tag(BlockTags.MINEABLE_WITH_PICKAXE).add(pipe.get()));
    }
}
