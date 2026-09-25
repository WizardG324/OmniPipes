package com.wizardg.omnipipes.item;

import com.wizardg.omnipipes.screen.TagFilterScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

// A list of item/fluid tags. Placed in a pipe filter slot it matches everything in any of its tags.
public class TagFilterItem extends Item {
    public static final int MAX_TAGS = 12;

    public TagFilterItem(Properties properties) {
        super(properties);
    }

    public static List<Identifier> tags(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.TAGS, List.of());
    }

    public static boolean tagExists(Identifier id) {
        return BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM, id)).isPresent()
                || BuiltInRegistries.FLUID.get(TagKey.create(Registries.FLUID, id)).isPresent();
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) TagFilterScreen.open(hand);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        List<Identifier> tags = tags(stack);
        if (tags.isEmpty()) builder.accept(Component.translatable("tooltip.omni_pipes.tag_filter.empty").withStyle(ChatFormatting.GRAY));
        for (Identifier tag : tags) builder.accept(Component.literal("#" + tag).withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable("tooltip.omni_pipes.tag_filter.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
