package com.wizardg.omnipipes.util;

import com.wizardg.omnipipes.OmniPipes;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModTags {

    public static class Items {
        public static final TagKey<Item> PIPES = tag("pipes");
        public static final TagKey<Item> COLORED_PIPES = tag("colored_pipes");
        public static final TagKey<Item> TIER_UPGRADES = tag("tier_upgrades");
        public static final TagKey<Item> TYPE_UPGRADES = tag("type_upgrades");

        private static TagKey<Item> tag(String name) {
            return ItemTags.create(Identifier.fromNamespaceAndPath(OmniPipes.MODID, name));
        }
    }
}
