package com.wizardg.omnipipes.item;

import com.wizardg.omnipipes.screen.TagFilterScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.List;

// A list of item/fluid tags. Placed in a pipe filter slot it matches everything in any of its tags.
public class TagFilterItem extends Item {
    public static final int MAX_TAGS = 12;

    public TagFilterItem(Properties properties) {
        super(properties);
    }

    public static List<ResourceLocation> tags(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.TAGS, List.of());
    }

    public static boolean tagExists(ResourceLocation id) {
        return BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, id)).isPresent()
                || BuiltInRegistries.FLUID.getTag(TagKey.create(Registries.FLUID, id)).isPresent();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) TagFilterScreen.open(hand);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        List<ResourceLocation> tags = tags(stack);
        if (tags.isEmpty()) tooltip.add(Component.translatable("tooltip.omni_pipes.tag_filter.empty").withStyle(ChatFormatting.GRAY));
        for (ResourceLocation tag : tags) tooltip.add(Component.literal("#" + tag).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.omni_pipes.tag_filter.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
