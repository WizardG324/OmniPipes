package com.wizardg.omnipipes;

import com.wizardg.omnipipes.config.ServerConfig;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.wizardg.omnipipes.block.ModBlockEntities;
import com.wizardg.omnipipes.block.ModBlocks;
import com.wizardg.omnipipes.gametest.ModGameTests;
import com.wizardg.omnipipes.item.ModCreativeTab;
import com.wizardg.omnipipes.item.ModDataComponents;
import com.wizardg.omnipipes.item.ModItems;
import com.wizardg.omnipipes.networking.SetFilterPayload;
import com.wizardg.omnipipes.networking.SetTagFilterPayload;
import com.wizardg.omnipipes.screen.ModMenuTypes;
import com.wizardg.omnipipes.screen.PipeScreen;
import net.minecraft.client.color.block.BlockTintSources;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import java.util.List;

@Mod(OmniPipes.MODID)
public class OmniPipes {
    public static final String MODID = "omni_pipes";
    public static final Logger LOGGER = LogUtils.getLogger();

    public OmniPipes(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModDataComponents.COMPONENTS.register(modEventBus);
        ModCreativeTab.CREATIVE_MODE_TABS.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        ModGameTests.TEST_FUNCTIONS.register(modEventBus);
        modEventBus.addListener(ModGameTests::registerTests);
        modEventBus.addListener(SetFilterPayload::register);
        modEventBus.addListener(SetTagFilterPayload::register);

        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
    }

    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {

        // Colored pipes tint the cable (tintindex 0) and the lighter core cube (tintindex 1).
        @SubscribeEvent
        public static void registerBlockColors(RegisterColorHandlersEvent.BlockTintSources event) {
            ModBlocks.COLORED_PIPES.forEach((color, pipe) ->
                    event.register(List.of(BlockTintSources.constant(color.getTextureDiffuseColor())), pipe.get()));
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenuTypes.PIPE_MENU.get(), PipeScreen::new);
        }
    }
}
