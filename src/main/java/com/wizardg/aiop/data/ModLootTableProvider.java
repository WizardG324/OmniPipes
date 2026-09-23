package com.wizardg.aiop.data;

import com.wizardg.aiop.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.packs.VanillaBlockLoot;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.HashSet;
import java.util.Set;

public class ModLootTableProvider extends VanillaBlockLoot {
    private final Set<Block> knownBlocks = new HashSet<>();

    public ModLootTableProvider(HolderLookup.Provider provider) {
        super(provider);
    }

    @Override
    protected void generate() {
        ModBlocks.ALL_PIPES.forEach(pipe -> dropSelf(pipe.get()));
    }

    // Only validate our own blocks, not vanilla's.
    @Override
    protected void add(Block block, LootTable.Builder table) {
        super.add(block, table);
        knownBlocks.add(block);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return knownBlocks;
    }
}
