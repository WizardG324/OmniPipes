package com.wizardg.aiop.item;

import com.wizardg.aiop.AIOPAllinOnePipe;
import com.wizardg.aiop.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AIOPAllinOnePipe.MODID);

    public static final Supplier<CreativeModeTab> AIOP_TAB = CREATIVE_MODE_TABS.register("aiop", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ModBlocks.PIPE.asItem().getDefaultInstance())
            .title(Component.translatable("itemGroup.aiop"))
            .displayItems(ModItems.ITEMS.getEntries()).build());
}
