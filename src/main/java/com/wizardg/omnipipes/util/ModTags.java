package com.wizardg.omnipipes.util;

import com.wizardg.omnipipes.OmniPipes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.List;

public class ModTags {

    public static class Items {
        public static final TagKey<Item> PIPES = tag("pipes");
        public static final TagKey<Item> COLORED_PIPES = tag("colored_pipes");
        public static final TagKey<Item> TIER_UPGRADES = tag("tier_upgrades");
        public static final TagKey<Item> TYPE_UPGRADES = tag("type_upgrades");
        // Items in these work as that type's upgrade, so packs can add their own.
        public static final TagKey<Item> FLUID_UPGRADE_COMPAT = tag("fluid_upgrade_compat");
        public static final TagKey<Item> ENERGY_UPGRADE_COMPAT = tag("energy_upgrade_compat");
        public static final List<TagKey<Item>> UPGRADE_COMPAT = List.of(FLUID_UPGRADE_COMPAT, ENERGY_UPGRADE_COMPAT);

        private static TagKey<Item> tag(String name) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(OmniPipes.MODID, name));
        }
    }
}
