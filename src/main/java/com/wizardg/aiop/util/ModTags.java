package com.wizardg.aiop.util;

import com.wizardg.aiop.AIOPAllinOnePipe;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModTags {

    public static class Items {
        public static final TagKey<Item> PIPES = tag("pipes");
        public static final TagKey<Item> COLORED_PIPES = tag("colored_pipes");

        private static TagKey<Item> tag(String name) {
            return ItemTags.create(Identifier.fromNamespaceAndPath(AIOPAllinOnePipe.MODID, name));
        }
    }
}
