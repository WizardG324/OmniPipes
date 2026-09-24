package com.wizardg.omnipipes.screen;

import com.wizardg.omnipipes.OmniPipes;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, OmniPipes.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<PipeMenu>> PIPE_MENU =
            MENUS.register("pipe_menu", () -> IMenuTypeExtension.create(PipeMenu::new));
}
