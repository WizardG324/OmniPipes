package com.wizardg.omnipipes.item;

import com.wizardg.omnipipes.block.custom.PipeBlock;
import com.wizardg.omnipipes.config.ServerConfig;
import com.wizardg.omnipipes.util.ModFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.LevelReader;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.function.Consumer;

public class UpgradeItem extends Item {
    public UpgradeItem(Properties properties) {
        super(properties);
    }

    // Vanilla skips the block when sneaking with an item in hand, this lets shift right click reach pipes to install upgrades.
    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return level.getBlockState(pos).getBlock() instanceof PipeBlock;
    }

    // What the upgrade does, tiers show their rates from the server config (defaults when no world is loaded).
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        String key = "tooltip.omni_pipes.";
        int tier = ModItems.TIER_UPGRADES.stream().map(t -> t.get()).toList().indexOf(this);
        if (tier >= 0) {
            ServerConfig.Rates rates = ServerConfig.upgradeTiers.get(tier);
            builder.accept(Component.translatable(key + "speed", value(rates.transferRate())).withStyle(ChatFormatting.GRAY));
            builder.accept(Component.translatable(key + "amounts", value(rates.itemTransferRate()),
                    ModFormat.decimal(value(rates.fluidTransferRate()) / 1000.0), ModFormat.compact(value(rates.energyTransferRate()))).withStyle(ChatFormatting.GRAY));
        } else {
            builder.accept(Component.translatable(key + "upgrade." + stack.getItem().builtInRegistryHolder().key().identifier().getPath()).withStyle(ChatFormatting.GRAY));
        }
        builder.accept(Component.translatable(key + "install").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static int value(ModConfigSpec.ConfigValue<Integer> config) {
        return ServerConfig.SPEC.isLoaded() ? config.get() : config.getDefault();
    }
}
