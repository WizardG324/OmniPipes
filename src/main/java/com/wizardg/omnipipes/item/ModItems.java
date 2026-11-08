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
            TIER_UPGRADES.add(ITEMS.registerItem("tier_" + tier + "_upgrade", UpgradeItem::new, new Item.Properties().stacksTo(16)));
    }
    public static final DeferredItem<Item> CREATIVE_UPGRADE = ITEMS.registerItem("creative_upgrade", UpgradeItem::new, new Item.Properties().stacksTo(16).rarity(Rarity.EPIC));

    // Type upgrades, pipes only move items without any
    public static final DeferredItem<Item> FLUID_UPGRADE = ITEMS.registerItem("fluid_upgrade", UpgradeItem::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> ENERGY_UPGRADE = ITEMS.registerItem("energy_upgrade", UpgradeItem::new, new Item.Properties().stacksTo(16));
    // Mekanism
    public static final DeferredItem<Item> CHEMICAL_UPGRADE = ITEMS.registerItem("chemical_upgrade", UpgradeItem::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> HEAT_UPGRADE = ITEMS.registerItem("heat_upgrade", UpgradeItem::new, new Item.Properties().stacksTo(16));
    public static final List<DeferredItem<Item>> MEKANISM_UPGRADES = List.of(CHEMICAL_UPGRADE, HEAT_UPGRADE);
    // Ars Nouveau
    public static final DeferredItem<Item> SOURCE_UPGRADE = ITEMS.registerItem("source_upgrade", UpgradeItem::new, new Item.Properties().stacksTo(16));

    // Compat upgrades whose mod isn't installed, hidden from the creative tab and recipe viewers since they do nothing.
    public static final List<DeferredItem<Item>> UNAVAILABLE = unavailable();

    private static List<DeferredItem<Item>> unavailable() {
        List<DeferredItem<Item>> hidden = new ArrayList<>();
        if (!OmniPipes.MEKANISM_LOADED) hidden.addAll(MEKANISM_UPGRADES);
        if (!OmniPipes.ARS_NOUVEAU_LOADED) hidden.add(SOURCE_UPGRADE);
        return List.copyOf(hidden);
    }

    public static final DeferredItem<PipeConfiguratorItem> PIPE_CONFIGURATOR = ITEMS.registerItem("pipe_configurator", PipeConfiguratorItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredItem<TagFilterItem> TAG_FILTER = ITEMS.registerItem("tag_filter", TagFilterItem::new, new Item.Properties().stacksTo(1));
}
