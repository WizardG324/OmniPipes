package com.wizardg.aiop.block;

import com.wizardg.aiop.AIOPAllinOnePipe;
import com.wizardg.aiop.block.entity.PipeBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AIOPAllinOnePipe.MODID);

    public static final Supplier<BlockEntityType<PipeBlockEntity>> PIPE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("pipe_block_entity", () ->
                    new BlockEntityType<>(PipeBlockEntity::new, ModBlocks.ALL_PIPES.stream().map(DeferredBlock::get).collect(Collectors.toSet())));
}
