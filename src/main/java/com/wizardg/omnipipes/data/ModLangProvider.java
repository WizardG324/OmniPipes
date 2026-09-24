package com.wizardg.omnipipes.data;

import com.wizardg.omnipipes.OmniPipes;
import com.wizardg.omnipipes.block.ModBlocks;
import com.wizardg.omnipipes.item.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLangProvider extends LanguageProvider {

    public ModLangProvider(PackOutput output) {
        super(output, OmniPipes.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        //Creative Tab
        add("itemGroup.omni_pipes", "Omni Pipes");

        //Blocks
        add(ModBlocks.PIPE.get(), "Pipe");
        ModBlocks.COLORED_PIPES.forEach((color, pipe) -> add(pipe.get(), titleCase(color.getName()) + " Pipe"));

        //Upgrades
        for (int t = 0; t < ModItems.TIER_UPGRADES.size(); t++) add(ModItems.TIER_UPGRADES.get(t).get(), "Tier " + (t + 1) + " Upgrade");
        add(ModItems.CREATIVE_UPGRADE.get(), "Creative Upgrade");
        add(ModItems.FLUID_UPGRADE.get(), "Fluid Upgrade");
        add(ModItems.ENERGY_UPGRADE.get(), "Energy Upgrade");

        //Jade
        add("config.jade.plugin_omni_pipes.pipe", "Pipe Connection");
        add("jade.omni_pipes.mode", "Mode: %s");
        add("jade.omni_pipes.mode.insert", "Insert");
        add("jade.omni_pipes.mode.extract", "Extract");
        add("jade.omni_pipes.mode.both", "%s & %s");
        add("jade.omni_pipes.transferring", "Transferring: %s");
        add("jade.omni_pipes.type.items", "Items");
        add("jade.omni_pipes.type.fluids", "Fluids");
        add("jade.omni_pipes.type.energy", "Energy");
        add("jade.omni_pipes.everything", "Everything");
        add("jade.omni_pipes.stats", "Stats: %st / %s");
        add("jade.omni_pipes.fluid", "%sB");
        add("jade.omni_pipes.energy", "%s FE");
        add("jade.omni_pipes.unlimited", "Unlimited");

        //Messages
        add("message.omni_pipes.upgrade.same_tier", "A tier upgrade of this type already exists");
        add("message.omni_pipes.upgrade.same_type", "An upgrade of this type already exists");
        add("message.omni_pipes.upgrade.type_full", "All type upgrade slots on this side are full");

        //Pipe Screen
        add("screen.omni_pipes.pipe", "Pipe - %s");
        for (Direction dir : Direction.values()) add("screen.omni_pipes.side." + dir.getName(), titleCase(dir.getName()));
        add("screen.omni_pipes.mode", "Mode: %s");
        add("screen.omni_pipes.mode.insert", "Insert");
        add("screen.omni_pipes.mode.insert.tooltip", "Receives resources from the network into this block");
        add("screen.omni_pipes.mode.extract", "Extract");
        add("screen.omni_pipes.mode.extract.tooltip", "Pulls resources out of this block into the network");
        add("screen.omni_pipes.mode.both", "Both");
        add("screen.omni_pipes.mode.both.tooltip", "Receives and extracts, use different channels to control where things go");
        add("screen.omni_pipes.redstone", "Redstone: %s");
        add("screen.omni_pipes.redstone.disabled", "Disabled");
        add("screen.omni_pipes.redstone.disabled.tooltip", "Redstone is ignored, this side always runs");
        add("screen.omni_pipes.redstone.low_signal", "Run on Low Signal");
        add("screen.omni_pipes.redstone.low_signal.tooltip", "Runs while the pipe gets a signal strength of 0-7, including no signal");
        add("screen.omni_pipes.redstone.high_signal", "Run on High Signal");
        add("screen.omni_pipes.redstone.high_signal.tooltip", "Runs while the pipe gets a signal strength of 8-15");
        add("screen.omni_pipes.redstone.off", "Off");
        add("screen.omni_pipes.redstone.off.tooltip", "This side is switched off and never runs");
        add("screen.omni_pipes.insert_channel", "Insert Channel: %s");
        add("screen.omni_pipes.insert_channel.tooltip", "Only receives from extract sides on the same channel");
        add("screen.omni_pipes.extract_channel", "Extract Channel: %s");
        add("screen.omni_pipes.extract_channel.tooltip", "Only sends to insert sides on the same channel");
        add("screen.omni_pipes.cycle_hint", "Left click: next, right click: back");
        add("screen.omni_pipes.channel", "Channel: %s");
        add("screen.omni_pipes.filter", "Filter: %s");
        add("screen.omni_pipes.speed", "Speed: every %s ticks (%ss)");
        add("screen.omni_pipes.speed.fastest", "Range: %s (fastest for this tier) to %s ticks");
        add("screen.omni_pipes.speed.hint", "Left click: slower, right click: faster, shift: by 10");
        add("screen.omni_pipes.distribution", "Distribution: %s");
        add("screen.omni_pipes.distribution.closest", "Closest");
        add("screen.omni_pipes.distribution.closest.tooltip", "Fills the nearest insert sides first");
        add("screen.omni_pipes.distribution.furthest", "Furthest");
        add("screen.omni_pipes.distribution.furthest.tooltip", "Fills the furthest insert sides first");
        add("screen.omni_pipes.distribution.round_robin", "Round Robin");
        add("screen.omni_pipes.distribution.round_robin.tooltip", "Each transfer starts at the next insert side, spreading items evenly");
        add("screen.omni_pipes.distribution.random", "Random");
        add("screen.omni_pipes.distribution.random.tooltip", "Sends to insert sides in a random order each transfer");
        add("screen.omni_pipes.tab.extract", "Extract");
        add("screen.omni_pipes.tab.insert", "Insert");
        add("screen.omni_pipes.tab.extract.tooltip", "Extract Filter: controls what this side pulls out of the block");
        add("screen.omni_pipes.tab.insert.tooltip", "Insert Filter: controls what this side lets into the block");
        add("screen.omni_pipes.whitelist", "Whitelist");
        add("screen.omni_pipes.whitelist.tooltip", "Only what is in the list passes");
        add("screen.omni_pipes.blacklist", "Blacklist");
        add("screen.omni_pipes.blacklist.tooltip", "What is in the list is blocked, everything else passes");
        add("screen.omni_pipes.components.match", "Match Components");
        add("screen.omni_pipes.components.match.tooltip", "Items must match exactly, including enchantments, names and other data");
        add("screen.omni_pipes.components.ignore", "Ignore Components");
        add("screen.omni_pipes.components.ignore.tooltip", "Only the item type has to match");
        add("screen.omni_pipes.filter_slot.tooltip", "Click with an item to add it to the filter (a bucket or tank filters its fluid too), right click to remove");
        add("screen.omni_pipes.amount.none", "No amount set, moves any number");
        add("screen.omni_pipes.amount.extract", "Moves up to %s at a time");
        add("screen.omni_pipes.amount.insert", "Fills the target up to %s");
        add("screen.omni_pipes.amount.blacklist", "Amounts are ignored in blacklist mode");
        add("tooltip.omni_pipes.speed", "Speed: every %st");
        add("tooltip.omni_pipes.amounts", "Per transfer: %s items, %sB, %s FE");
        add("tooltip.omni_pipes.upgrade.creative_upgrade", "Every tick, unlimited amounts, moves every available type");
        add("tooltip.omni_pipes.upgrade.fluid_upgrade", "Lets a pipe connection extract fluids");
        add("tooltip.omni_pipes.upgrade.energy_upgrade", "Lets a pipe connection extract energy");
        add("tooltip.omni_pipes.install", "Shift right-click a pipe connection to install");
        add("screen.omni_pipes.amount.fluid", "Also filters the fluid inside, its amount counts in buckets");
        add("screen.omni_pipes.amount.hint", "Scroll to change the amount (shift: by 10), right click to remove");
    }

    // light_blue -> Light Blue
    private static String titleCase(String name) {
        StringBuilder out = new StringBuilder();
        for (String word : name.split("_")) out.append(out.isEmpty() ? "" : " ").append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        return out.toString();
    }
}
