package com.wizardg.omnipipes.item;

import com.wizardg.omnipipes.block.custom.PipeBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

import java.util.function.Consumer;

// Copies and pastes connection settings, or switches pipe sides off and on. The pipe block does the work,
// this item only holds the mode and the copied settings.
public class PipeConfiguratorItem extends Item {
    public PipeConfiguratorItem(Properties properties) {
        super(properties);
    }

    public static boolean copyMode(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.COPY_MODE, false);
    }

    private static Component modeName(boolean copy) {
        return Component.translatable("tooltip.omni_pipes.configurator.mode." + (copy ? "copy" : "configure"));
    }

    // Shift + right-click in the air swaps the mode.
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            boolean copy = !copyMode(stack);
            stack.set(ModDataComponents.COPY_MODE, copy);
            player.sendOverlayMessage(Component.translatable("tooltip.omni_pipes.configurator.mode", modeName(copy)));
        }
        return InteractionResult.SUCCESS;
    }

    // Lets shift + right-click reach pipes, for copying.
    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return level.getBlockState(pos).getBlock() instanceof PipeBlock;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        boolean copy = copyMode(stack);
        builder.accept(Component.translatable("tooltip.omni_pipes.configurator.mode", modeName(copy)).withStyle(ChatFormatting.AQUA));
        if (!copy) {
            builder.accept(Component.translatable("tooltip.omni_pipes.configurator.configure_hint").withStyle(ChatFormatting.GRAY));
            builder.accept(Component.translatable("tooltip.omni_pipes.configurator.dismantle_hint").withStyle(ChatFormatting.GRAY));
        } else {
            CompoundTag copied = stack.get(ModDataComponents.COPIED_SETTINGS);
            builder.accept(copied == null ? Component.translatable("tooltip.omni_pipes.configurator.empty").withStyle(ChatFormatting.GRAY)
                    : Component.translatable("tooltip.omni_pipes.configurator.saved").withStyle(ChatFormatting.WHITE));
            builder.accept(Component.translatable("tooltip.omni_pipes.configurator.copy_hint").withStyle(ChatFormatting.GRAY));
        }
        builder.accept(Component.translatable("tooltip.omni_pipes.configurator.switch_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
