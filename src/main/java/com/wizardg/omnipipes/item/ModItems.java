package com.wizardg.omnipipes.item;

import com.wizardg.omnipipes.OmniPipes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(OmniPipes.MODID);

    // Tier upgrades
    public static final List<DeferredItem<Item>> TIER_UPGRADES = new ArrayList<>(); // index 0 = tier 1
    static {
        for (int tier = 1; tier <= 4; tier++)
            TIER_UPGRADES.add(ITEMS.registerItem("tier_" + tier + "_upgrade", UpgradeItem::new, p -> p.stacksTo(16)));
    }
    public static final DeferredItem<Item> CREATIVE_UPGRADE = ITEMS.registerItem("creative_upgrade", UpgradeItem::new, p -> p.stacksTo(16).rarity(Rarity.EPIC));

    // Type upgrades, pipes only move items without any
    public static final DeferredItem<Item> FLUID_UPGRADE = ITEMS.registerItem("fluid_upgrade", UpgradeItem::new, p -> p.stacksTo(16));
    public static final DeferredItem<Item> ENERGY_UPGRADE = ITEMS.registerItem("energy_upgrade", UpgradeItem::new, p -> p.stacksTo(16));

    public static final DeferredItem<PipeConfiguratorItem> PIPE_CONFIGURATOR = ITEMS.registerItem("pipe_configurator", PipeConfiguratorItem::new, p -> p.stacksTo(1));

    public static final DeferredItem<TagFilterItem> TAG_FILTER = ITEMS.registerItem("tag_filter", TagFilterItem::new, p -> p.stacksTo(1));
}
