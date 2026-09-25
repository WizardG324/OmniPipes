package com.wizardg.omnipipes.item;

import com.wizardg.omnipipes.OmniPipes;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, OmniPipes.MODID);

    // Tag ids on a Tag Filter, each one matches the item tag and the fluid tag with that id.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<Identifier>>> TAGS = COMPONENTS.registerComponentType("tags",
            builder -> builder.persistent(Identifier.CODEC.listOf()).networkSynchronized(Identifier.STREAM_CODEC.apply(ByteBufCodecs.list())));

    // Pipe Configurator: true while in copy/paste mode (configuration is the default), and the settings it copied.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> COPY_MODE = COMPONENTS.registerComponentType("copy_mode",
            builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> COPIED_SETTINGS = COMPONENTS.registerComponentType("copied_settings",
            builder -> builder.persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG));
}
