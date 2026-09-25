package com.wizardg.omnipipes;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

@Mod(OmniPipes.MODID)
public class OmniPipes {
    public static final String MODID = "omni_pipes";
    public static final Logger LOGGER = LogUtils.getLogger();

    public OmniPipes(IEventBus modEventBus, ModContainer modContainer) {
    }
}
