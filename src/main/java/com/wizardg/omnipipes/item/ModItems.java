package com.wizardg.omnipipes.item;

import com.wizardg.omnipipes.OmniPipes;
import com.wizardg.omnipipes.config.ServerConfig;
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

    // Type upgrades, pipes only move items without any (none at all when requireItemUpgrade is on)
    public static final DeferredItem<Item> ITEM_UPGRADE = ITEMS.registerItem("item_upgrade", UpgradeItem::new, p -> p.stacksTo(16));
    public static final DeferredItem<Item> FLUID_UPGRADE = ITEMS.registerItem("fluid_upgrade", UpgradeItem::new, p -> p.stacksTo(16));
    public static final DeferredItem<Item> ENERGY_UPGRADE = ITEMS.registerItem("energy_upgrade", UpgradeItem::new, p -> p.stacksTo(16));
    public static final DeferredItem<Item> RIFT_UPGRADE = ITEMS.registerItem("rift_upgrade", UpgradeItem::new, p -> p.stacksTo(16)); // BBL Rifts

    public static final DeferredItem<PipeConfiguratorItem> PIPE_CONFIGURATOR = ITEMS.registerItem("pipe_configurator", PipeConfiguratorItem::new, p -> p.stacksTo(1));

    // Upgrades that do nothing in this game, hidden from the creative tab and recipe viewers. Reads the server config,
    // so it's checked on joining a world.
    public static List<DeferredItem<Item>> hidden() {
        List<DeferredItem<Item>> hidden = new ArrayList<>();
        if (!OmniPipes.RIFTS_LOADED) hidden.add(RIFT_UPGRADE);
        if (!ServerConfig.SPEC.isLoaded() || !ServerConfig.requireItemUpgrade.get()) hidden.add(ITEM_UPGRADE);
        return hidden;
    }

    public static final DeferredItem<TagFilterItem> TAG_FILTER = ITEMS.registerItem("tag_filter", TagFilterItem::new, p -> p.stacksTo(1));
}
