package com.wizardg.omnipipes.block;

import com.wizardg.omnipipes.OmniPipes;
import com.wizardg.omnipipes.block.entity.PipeBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, OmniPipes.MODID);

    public static final Supplier<BlockEntityType<PipeBlockEntity>> PIPE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("pipe_block_entity", () ->
                    BlockEntityType.Builder.of(PipeBlockEntity::new, ModBlocks.ALL_PIPES.stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));
}
