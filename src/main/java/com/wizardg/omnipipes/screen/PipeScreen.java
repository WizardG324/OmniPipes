package com.wizardg.omnipipes.screen;

import com.wizardg.omnipipes.OmniPipes;
import com.wizardg.omnipipes.block.custom.PipeBlock;
import com.wizardg.omnipipes.block.entity.PipeBlockEntity;
import com.wizardg.omnipipes.block.entity.PipeFilter;
import com.wizardg.omnipipes.networking.SetFilterPayload;
import com.wizardg.omnipipes.util.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public class PipeScreen extends AbstractContainerScreen<PipeMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(OmniPipes.MODID, "textures/gui/pipe.png");
    private static final int STRIP_U = 180; // upgrade strip's spot in the texture
    private static final int STRIP_WIDTH = 26;
    private static final int STRIP_HEIGHT = 86;
    private static final int REDSTONE_X = -22; // redstone (and distribution below it) float just left of the panel
    private static final int REDSTONE_Y = 4;
    private static final int GRID_WIDTH = PipeMenu.GRID_COLS * 18;
    private static final int GRID_HEIGHT = PipeMenu.GRID_ROWS * 18;

    private final List<CycleButton> buttons = new ArrayList<>();
    private PipeBlock.Side shownMode;
    private boolean shownInsert;

    public PipeScreen(PipeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = PipeMenu.PANEL_HEIGHT;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        // Tooltips are raised above REI's drag highlight (z 500).
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 200);
        renderTooltip(graphics, mouseX, mouseY);
        for (CycleButton button : buttons)
            if (button.isHovered() && button.tip != null) graphics.renderTooltip(font, font.split(button.tip, 170), mouseX, mouseY);
        graphics.pose().popPose();
    }

    // The filter and channel buttons depend on the mode and the shown filter, so both rebuild the widgets.
    @Override
    protected void init() {
        super.init();
        shownMode = menu.mode();
        shownInsert = menu.showingInsert();
        buttons.clear();

        add(new CycleButton(PipeMenu.MODE, 8, 18, 78, null));
        // Only "both" has two filters to switch between.
        add(new CycleButton(PipeMenu.TAB, 90, 18, 78, null)).active = shownMode == PipeBlock.Side.BOTH;
        add(new CycleButton(PipeMenu.REDSTONE, REDSTONE_X, REDSTONE_Y, 20, () -> new ItemStack(switch (menu.redstone()) {
            case DISABLED -> Items.GUNPOWDER;
            case LOW_SIGNAL -> Items.REDSTONE;
            case HIGH_SIGNAL -> Items.REDSTONE_BLOCK;
            case OFF -> Items.BARRIER;
        })));
        // Distribution only matters when extracting.
        if (shownMode.extracts()) add(new CycleButton(PipeMenu.DISTRIBUTION, REDSTONE_X, REDSTONE_Y + 22, 20, () -> new ItemStack(switch (menu.distribution()) {
            case CLOSEST -> Items.COMPASS;
            case FURTHEST -> Items.ENDER_PEARL;
            case ROUND_ROBIN -> Items.CLOCK;
            case RANDOM -> Items.RABBIT_FOOT;
        })));
        if (shownMode.extracts()) add(new SpeedButton(REDSTONE_X, REDSTONE_Y + 44));

        add(new CycleButton(shownInsert ? PipeMenu.INSERT_CHANNEL : PipeMenu.EXTRACT_CHANNEL, 8, 40, 100, null));
        add(new CycleButton(PipeMenu.WHITELIST, 128, 40, 20,
                () -> new ItemStack(menu.whitelist() ? Items.WHITE_WOOL : Items.BLACK_WOOL)));
        add(new CycleButton(PipeMenu.COMPONENTS, 150, 40, 20,
                () -> new ItemStack(menu.matchComponents() ? Items.ENCHANTED_BOOK : Items.BOOK)));
        updateButtons();
    }

    private <T extends Button> T add(T button) {
        if (button instanceof CycleButton cycle) buttons.add(cycle);
        return addRenderableWidget(button);
    }

    private static boolean isChannel(int setting) {
        return setting == PipeMenu.INSERT_CHANNEL || setting == PipeMenu.EXTRACT_CHANNEL;
    }

    private DyeColor channel() {
        return shownInsert ? menu.insertChannel() : menu.extractChannel();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (menu.mode() != shownMode || menu.showingInsert() != shownInsert) rebuildWidgets();
        else updateButtons();
    }

    private void updateButtons() {
        for (CycleButton button : buttons) {
            String key = switch (button.setting) {
                case PipeMenu.MODE -> "screen.omni_pipes.mode." + menu.mode().getSerializedName();
                case PipeMenu.REDSTONE -> "screen.omni_pipes.redstone." + menu.redstone().name().toLowerCase(Locale.ROOT);
                case PipeMenu.TAB -> "screen.omni_pipes.tab." + (shownInsert ? "insert" : "extract");
                case PipeMenu.DISTRIBUTION -> "screen.omni_pipes.distribution." + menu.distribution().name().toLowerCase(Locale.ROOT);
                case PipeMenu.SPEED -> "screen.omni_pipes.speed";
                case PipeMenu.WHITELIST -> menu.whitelist() ? "screen.omni_pipes.whitelist" : "screen.omni_pipes.blacklist";
                case PipeMenu.COMPONENTS -> menu.matchComponents() ? "screen.omni_pipes.components.match" : "screen.omni_pipes.components.ignore";
                default -> "screen.omni_pipes." + (shownInsert ? "insert" : "extract") + "_channel";
            };
            Component message = switch (button.setting) {
                case PipeMenu.MODE -> Component.translatable("screen.omni_pipes.mode", Component.translatable(key));
                case PipeMenu.TAB -> Component.translatable("screen.omni_pipes.filter", Component.translatable(key));
                case PipeMenu.REDSTONE, PipeMenu.DISTRIBUTION, PipeMenu.SPEED, PipeMenu.WHITELIST, PipeMenu.COMPONENTS -> Component.empty(); // icon only
                default -> Component.translatable("screen.omni_pipes.channel", Component.translatable("color.minecraft." + channel().getName()));
            };
            button.setMessage(message);
            MutableComponent tooltip = switch (button.setting) {
                case PipeMenu.REDSTONE -> Component.translatable("screen.omni_pipes.redstone", Component.translatable(key))
                        .append("\n").append(Component.translatable(key + ".tooltip"));
                case PipeMenu.DISTRIBUTION -> Component.translatable("screen.omni_pipes.distribution", Component.translatable(key))
                        .append("\n").append(Component.translatable(key + ".tooltip"));
                case PipeMenu.SPEED -> Component.translatable(key, menu.speed(), String.format(Locale.ROOT, "%.2f", menu.speed() / 20.0))
                        .append("\n").append(Component.translatable(key + ".fastest", menu.fastestSpeed(), PipeBlockEntity.SLOWEST_SPEED))
                        .append("\n").append(Component.translatable(key + ".hint").withStyle(ChatFormatting.GRAY));
                case PipeMenu.WHITELIST, PipeMenu.COMPONENTS -> Component.translatable(key)
                        .append("\n").append(Component.translatable(key + ".tooltip"));
                default -> Component.translatable(key + ".tooltip");
            };
            if (button.setting == PipeMenu.MODE || button.setting == PipeMenu.REDSTONE || button.setting == PipeMenu.DISTRIBUTION || isChannel(button.setting))
                tooltip.append("\n").append(Component.translatable("screen.omni_pipes.cycle_hint").withStyle(ChatFormatting.GRAY));
            button.tip = tooltip;
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        graphics.blit(TEXTURE, leftPos + PipeMenu.STRIP_X, topPos, STRIP_U, 0, STRIP_WIDTH, STRIP_HEIGHT);

        // Channel color next to the channel button.
        int x = leftPos + 112;
        int y = topPos + 44;
        graphics.fill(x, y, x + 12, y + 12, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 11, y + 11, channel().getTextureDiffuseColor());
    }

    // Empty filter slots explain themselves, filled ones show the item's own tooltip.
    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        if (!menu.getCarried().isEmpty() || hoveredSlot == null || hoveredSlot.hasItem()) return;
        if (PipeMenu.isFilterSlot(hoveredSlot))
            graphics.renderTooltip(font, Component.translatable("screen.omni_pipes.filter_slot.tooltip"), mouseX, mouseY);
        TagKey<Item> accepts = PipeMenu.upgradeSlotAccepts(hoveredSlot);
        if (accepts != null) {
            String key = accepts == ModTags.Items.TIER_UPGRADES ? "screen.omni_pipes.slot.tier" : "screen.omni_pipes.slot.type";
            graphics.renderComponentTooltip(font, List.of(Component.translatable(key),
                    Component.translatable(key + ".tooltip").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }

    // Filled filter slots show their stock amount like a stack count.
    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        super.renderSlot(graphics, slot);
        // Drawn above the item, which sits at z 250.
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300);
        // Empty upgrade slots show a faded icon of what fits, cycling through every upgrade in the slot's tag.
        TagKey<Item> accepts = PipeMenu.upgradeSlotAccepts(slot);
        if (accepts != null && !slot.hasItem()) {
            List<Item> fits = BuiltInRegistries.ITEM.getTag(accepts).map(set -> set.stream().map(Holder::value).toList()).orElse(List.of());
            if (!fits.isEmpty()) {
                graphics.pose().translate(0, 0, -200);
                graphics.renderItem(new ItemStack(fits.get((int) (Util.getMillis() / 1000 % fits.size()))), slot.x, slot.y);
                graphics.pose().translate(0, 0, 200);
                graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0xB08B8B8B);
            }
        }
        int amount = PipeMenu.isFilterSlot(slot) && slot.hasItem() ? menu.amount(slot.getContainerSlot()) : 0;
        if (amount > 0) {
            String text = amount < 1000 ? String.valueOf(amount) : String.format(Locale.ROOT, "%.1fk", amount / 1000.0);
            graphics.drawString(font, text, slot.x + 17 - font.width(text), slot.y + 9, 0xFFFFFFFF, true);
        }
        graphics.pose().popPose();
    }

    // Filter slot tooltips add the amount and what it does, on top of the item's own tooltip.
    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        if (hoveredSlot == null || !PipeMenu.isFilterSlot(hoveredSlot)) return lines;
        int amount = menu.amount(hoveredSlot.getContainerSlot());
        String meaning = !menu.whitelist() ? "screen.omni_pipes.amount.blacklist"
                : amount == 0 ? "screen.omni_pipes.amount.none"
                : menu.showingInsert() ? "screen.omni_pipes.amount.insert" : "screen.omni_pipes.amount.extract";
        lines.add(Component.translatable(meaning, amount).withStyle(ChatFormatting.AQUA));
        if (PipeFilter.holdsFluid(stack))
            lines.add(Component.translatable("screen.omni_pipes.amount.fluid").withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("screen.omni_pipes.amount.hint").withStyle(ChatFormatting.GRAY));
        return lines;
    }

    // Recipe viewer drag and drop (compat/jei, compat/rei): the filter slots, where they are, and dropping into one.
    public List<Slot> filterSlots() {
        return menu.slots.stream().filter(PipeMenu::isFilterSlot).toList();
    }

    // Parts drawn outside the main panel (upgrade strip, buttons on the left), so recipe viewers keep clear of them.
    public List<Rect2i> extraAreas() {
        List<Rect2i> areas = new ArrayList<>(List.of(new Rect2i(leftPos + PipeMenu.STRIP_X, topPos, STRIP_WIDTH, STRIP_HEIGHT)));
        for (CycleButton button : buttons)
            if (button.getX() < leftPos) areas.add(new Rect2i(button.getX(), button.getY(), button.getWidth(), button.getHeight()));
        return areas;
    }

    public Rect2i slotArea(Slot slot) {
        return new Rect2i(leftPos + slot.x, topPos + slot.y, 16, 16);
    }

    public void dropIntoFilter(Slot slot, ItemStack stack) {
        if (!stack.isEmpty()) PacketDistributor.sendToServer(new SetFilterPayload(menu.containerId, slot.getContainerSlot(), stack.copyWithCount(1)));
    }

    // Fluids become their bucket, since a bucket entry filters its fluid.
    public static ItemStack bucketOf(Fluid fluid) {
        return new ItemStack(fluid.getBucket());
    }

    // Mouse wheel over a filled filter slot changes its amount (shift: by 10), anywhere else on the grid scrolls it.
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0 && hoveredSlot != null && PipeMenu.isFilterSlot(hoveredSlot) && hoveredSlot.hasItem()) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                    PipeMenu.amountButtonId(hoveredSlot.getContainerSlot(), scrollY > 0, Screen.hasShiftDown()));
            return true;
        }
        double x = mouseX - leftPos;
        double y = mouseY - topPos;
        if (scrollY != 0 && x >= PipeMenu.GRID_X && x < PipeMenu.GRID_X + GRID_WIDTH && y >= PipeMenu.GRID_Y && y < PipeMenu.GRID_Y + GRID_HEIGHT) {
            click(PipeMenu.SCROLL, scrollY < 0);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // Clicks on the upgrade strip and the buttons left of the panel count as inside, otherwise vanilla would drop the carried item.
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int mouseButton) {
        boolean onStrip = mouseX >= left + PipeMenu.STRIP_X && mouseX < left + PipeMenu.STRIP_X + STRIP_WIDTH
                && mouseY >= top && mouseY < top + STRIP_HEIGHT;
        boolean onButton = buttons.stream().anyMatch(b -> b.isMouseOver(mouseX, mouseY));
        return !onStrip && !onButton && super.hasClickedOutside(mouseX, mouseY, left, top, mouseButton);
    }

    private void click(int setting, boolean forward) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, PipeMenu.buttonId(setting, forward));
    }

    // Speed: left click slower, right click faster, shift by 10. Shows the ticks on the icon.
    private class SpeedButton extends CycleButton {
        SpeedButton(int x, int y) {
            super(PipeMenu.SPEED, x, y, 20, () -> new ItemStack(Items.SUGAR));
        }

        @Override
        public void onClick(double mouseX, double mouseY, int button) {
            click(Screen.hasShiftDown() ? PipeMenu.SPEED_BY_TEN : PipeMenu.SPEED, button == 1);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            String ticks = String.valueOf(menu.speed());
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200); // above the sugar icon
            graphics.drawString(font, ticks, getX() + 19 - font.width(ticks), getY() + 11, 0xFFFFFFFF, true);
            graphics.pose().popPose();
        }
    }

    // Left click steps forward, right click steps back. Can show an item icon, then the label sits after it.
    private class CycleButton extends Button {
        final int setting;
        private final @Nullable Supplier<ItemStack> icon;
        @Nullable Component tip; // drawn by the screen with the other tooltips

        CycleButton(int setting, int x, int y, int width, @Nullable Supplier<ItemStack> icon) {
            super(leftPos + x, topPos + y, width, 20, Component.empty(), b -> click(setting, true), DEFAULT_NARRATION);
            this.setting = setting;
            this.icon = icon;
        }

        @Override
        protected boolean isValidClickButton(int button) {
            return button == 0 || button == 1;
        }

        @Override
        public void onClick(double mouseX, double mouseY, int button) {
            click(setting, button == 0);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick); // icon buttons have no text, so this is just the sprite
            if (icon == null) return;
            ItemStack stack = icon.get();
            graphics.renderItem(stack, getX() + 2, getY() + (stack.is(Items.ENDER_PEARL) ? 1 : 2)); // the pearl sprite sits low
        }
    }
}
