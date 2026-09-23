package com.wizardg.aiop;

import com.wizardg.aiop.config.ServerConfig;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.wizardg.aiop.block.ModBlockEntities;
import com.wizardg.aiop.block.ModBlocks;
import com.wizardg.aiop.gametest.ModGameTests;
import com.wizardg.aiop.item.ModCreativeTab;
import com.wizardg.aiop.item.ModItems;
import net.minecraft.client.color.block.BlockTintSources;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

import java.util.List;

@Mod(AIOPAllinOnePipe.MODID)
public class AIOPAllinOnePipe {
    public static final String MODID = "aiop";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AIOPAllinOnePipe(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTab.CREATIVE_MODE_TABS.register(modEventBus);
        ModGameTests.TEST_FUNCTIONS.register(modEventBus);
        modEventBus.addListener(ModGameTests::registerTests);

        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
    }

    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {

        // Colored pipes tint the cable faces (tintindex 0) of the shared pipe models.
        @SubscribeEvent
        public static void registerBlockColors(RegisterColorHandlersEvent.BlockTintSources event) {
            ModBlocks.COLORED_PIPES.forEach((color, pipe) ->
                    event.register(List.of(BlockTintSources.constant(color.getTextureDiffuseColor())), pipe.get()));
        }
    }
}
