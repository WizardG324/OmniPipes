package com.wizardg.omnipipes.screen;

import com.wizardg.omnipipes.item.TagFilterItem;
import com.wizardg.omnipipes.networking.SetTagFilterPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

// Edits the tag list of the Tag Filter in hand: type a tag (matching tags are suggested), or click one of the tags of
// whatever is in the other hand. Every change is sent to the server right away.
public class TagFilterScreen extends Screen {
    private static final int ROW = 16;
    private static final int ROWS = TagFilterItem.MAX_TAGS; // the right column shows as many rows as the list can hold

    private final InteractionHand hand;
    private final List<Identifier> tags;
    private EditBox input;
    private String typed = "";
    private Component status = Component.empty();
    private final List<Identifier> allTags; // every item and fluid tag, for suggestions
    private final List<Button> offered = new ArrayList<>();
    private List<Identifier> offeredTags = List.of(); // all of them, the column shows ROWS starting at offerScroll
    private int offerScroll;

    private TagFilterScreen(InteractionHand hand, List<Identifier> tags) {
        super(Component.translatable("item.omni_pipes.tag_filter"));
        this.hand = hand;
        this.tags = new ArrayList<>(tags);
        this.allTags = Stream.concat(BuiltInRegistries.ITEM.getTags().map(named -> named.key().location()),
                BuiltInRegistries.FLUID.getTags().map(named -> named.key().location())).distinct().sorted().toList();
    }

    public static void open(InteractionHand hand) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new TagFilterScreen(hand, TagFilterItem.tags(minecraft.player.getItemInHand(hand))));
    }

    @Override
    protected void init() {
        int mid = width / 2;
        input = addRenderableWidget(new EditBox(font, mid - 150, 30, 250, 18, Component.translatable("screen.omni_pipes.tag_filter.input")));
        input.setMaxLength(128);
        input.setHint(Component.literal("c:ingots").withStyle(ChatFormatting.DARK_GRAY));
        input.setValue(typed);
        input.setResponder(value -> {
            typed = value;
            offerScroll = 0;
            status = check(parse(value));
            offerTags();
        });
        setInitialFocus(input);
        addRenderableWidget(Button.builder(Component.translatable("screen.omni_pipes.tag_filter.add"), b -> add(parse(input.getValue())))
                .bounds(mid + 104, 29, 46, 20).build());

        // Current tags on the left, click to remove.
        for (int i = 0; i < tags.size(); i++) {
            Identifier tag = tags.get(i);
            addRenderableWidget(Button.builder(Component.literal("#" + tag), b -> {
                tags.remove(tag);
                changed();
            }).bounds(mid - 150, 76 + i * ROW, 145, 14).tooltip(Tooltip.create(Component.translatable("screen.omni_pipes.tag_filter.remove"))).build());
        }
        offerTags();
    }

    // Right column, click to add: tags matching the typed text, or with nothing typed the tags of the other hand's
    // item (and the fluid it holds). Only these buttons are swapped while typing, so the text box keeps its focus.
    private void offerTags() {
        offered.forEach(this::removeWidget);
        offered.clear();
        String text = typed.trim().replaceFirst("^#", "").toLowerCase(Locale.ROOT);
        Stream<Identifier> source = text.isEmpty() ? otherHandTags() : allTags.stream().filter(id -> id.toString().contains(text))
                .sorted(Comparator.comparing((Identifier id) -> !id.getPath().startsWith(text) && !id.toString().startsWith(text)));
        offeredTags = source.filter(id -> !tags.contains(id)).distinct().toList();
        offerScroll = Math.clamp(offerScroll, 0, Math.max(0, offeredTags.size() - ROWS));
        int mid = width / 2;
        for (int i = 0; i < Math.min(ROWS, offeredTags.size() - offerScroll); i++) {
            Identifier tag = offeredTags.get(offerScroll + i);
            offered.add(addRenderableWidget(Button.builder(Component.literal("#" + tag), b -> add(tag))
                    .bounds(mid + 5, 76 + i * ROW, 145, 14).tooltip(Tooltip.create(Component.translatable("screen.omni_pipes.tag_filter.pick"))).build()));
        }
    }

    // The mouse wheel over the right column scrolls it when there are more tags than rows.
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= width / 2.0 && offeredTags.size() > ROWS && scrollY != 0) {
            offerScroll -= (int) Math.signum(scrollY);
            offerTags();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private Stream<Identifier> otherHandTags() {
        ItemStack other = minecraft.player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (other.isEmpty()) return Stream.empty();
        var fluid = FluidUtil.getFirstStackContained(other);
        Stream<TagKey<?>> itemTags = other.getItem().builtInRegistryHolder().tags().map(t -> t);
        Stream<TagKey<?>> fluidTags = fluid.isEmpty() ? Stream.empty() : fluid.getFluid().builtInRegistryHolder().tags().map(t -> t);
        return Stream.concat(fluidTags, itemTags).map(TagKey::location).sorted();
    }

    // "#c:ingots", "c:ingots" or "ingots" (minecraft namespace), null if it isn't a valid id.
    private static Identifier parse(String text) {
        String trimmed = text.trim();
        return Identifier.tryParse(trimmed.startsWith("#") ? trimmed.substring(1) : trimmed);
    }

    private Component check(Identifier tag) {
        if (typed.isBlank()) return Component.empty();
        if (tag == null) return Component.translatable("screen.omni_pipes.tag_filter.invalid").withStyle(ChatFormatting.RED);
        if (tags.contains(tag)) return Component.translatable("screen.omni_pipes.tag_filter.duplicate").withStyle(ChatFormatting.YELLOW);
        if (!TagFilterItem.tagExists(tag)) return Component.translatable("screen.omni_pipes.tag_filter.unknown").withStyle(ChatFormatting.RED);
        if (tags.size() >= TagFilterItem.MAX_TAGS) return Component.translatable("screen.omni_pipes.tag_filter.full").withStyle(ChatFormatting.RED);
        return Component.translatable("screen.omni_pipes.tag_filter.valid").withStyle(ChatFormatting.GREEN);
    }

    private void add(Identifier tag) {
        if (tag == null || tags.contains(tag) || tags.size() >= TagFilterItem.MAX_TAGS || !TagFilterItem.tagExists(tag)) return;
        tags.add(tag);
        if (tag.equals(parse(typed))) typed = "";
        changed();
    }

    private void changed() {
        ClientPacketDistributor.sendToServer(new SetTagFilterPayload(hand == InteractionHand.OFF_HAND, List.copyOf(tags)));
        status = Component.empty();
        rebuildWidgets();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (input.isFocused() && (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER)) {
            add(parse(input.getValue()));
            return true;
        }
        if (input.isFocused() && event.key() == GLFW.GLFW_KEY_TAB && !typed.isBlank() && !offeredTags.isEmpty()) {
            input.setValue(offeredTags.getFirst().toString());
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        int mid = width / 2;
        graphics.centeredText(font, title, mid, 12, 0xFFFFFFFF);
        graphics.text(font, status, mid - 150, 52, 0xFFFFFFFF);
        graphics.text(font, Component.translatable("screen.omni_pipes.tag_filter.list", tags.size(), TagFilterItem.MAX_TAGS), mid - 150, 64, 0xFFA0A0A0);
        graphics.text(font, Component.translatable(typed.isBlank() ? "screen.omni_pipes.tag_filter.other_hand" : "screen.omni_pipes.tag_filter.matching"),
                mid + 5, 64, 0xFFA0A0A0);
        if (offeredTags.size() > ROWS)
            graphics.text(font, Component.translatable("screen.omni_pipes.tag_filter.scroll", offerScroll + 1, offerScroll + ROWS, offeredTags.size()),
                    mid + 5, 78 + ROWS * ROW, 0xFFA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
