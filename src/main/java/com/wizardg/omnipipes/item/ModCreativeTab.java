package com.wizardg.omnipipes.item;

import com.wizardg.omnipipes.OmniPipes;
import com.wizardg.omnipipes.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OmniPipes.MODID);

    public static final Supplier<CreativeModeTab> OMNI_PIPES_TAB = CREATIVE_MODE_TABS.register("omni_pipes", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ModBlocks.PIPE.asItem().getDefaultInstance())
            .title(Component.translatable("itemGroup.omni_pipes"))
            .displayItems(ModItems.ITEMS.getEntries()).build());
}
