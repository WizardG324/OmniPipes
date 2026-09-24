package com.wizardg.omnipipes.block;

import com.wizardg.omnipipes.OmniPipes;
import com.wizardg.omnipipes.block.custom.PipeBlock;
import com.wizardg.omnipipes.item.ModItems;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(OmniPipes.MODID);

    private static final UnaryOperator<BlockBehaviour.Properties> PIPE_PROPERTIES = p -> p.strength(0.5f).sound(SoundType.METAL).noOcclusion();

    public static final DeferredBlock<PipeBlock> PIPE = registerBlock("pipe", PipeBlock::new, PIPE_PROPERTIES);
    public static final Map<DyeColor, DeferredBlock<PipeBlock>> COLORED_PIPES = new EnumMap<>(DyeColor.class);
    public static final List<DeferredBlock<PipeBlock>> ALL_PIPES = new ArrayList<>(List.of(PIPE));

    static {
        for (DyeColor color : DyeColor.values()) {
            var pipe = registerBlock(color.getName() + "_pipe", PipeBlock::new, PIPE_PROPERTIES);
            COLORED_PIPES.put(color, pipe);
            ALL_PIPES.add(pipe);
        }
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Function<BlockBehaviour.Properties, T> block,
                                                                    UnaryOperator<BlockBehaviour.Properties> properties) {
        DeferredBlock<T> toReturn = BLOCKS.registerBlock(name, block, properties);
        ModItems.ITEMS.registerSimpleBlockItem(toReturn);
        return toReturn;
    }
}
