package com.wizardg.aiop;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

@Mod(AIOPAllinOnePipe.MODID)
public class AIOPAllinOnePipe {
    public static final String MODID = "aiop";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AIOPAllinOnePipe(IEventBus modEventBus, ModContainer modContainer) {
    }
}
