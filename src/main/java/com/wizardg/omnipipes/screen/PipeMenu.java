package com.wizardg.omnipipes.screen;

import com.wizardg.omnipipes.block.custom.PipeBlock;
import com.wizardg.omnipipes.block.entity.PipeBlockEntity;
import com.wizardg.omnipipes.block.entity.PipeBlockEntity.RedstoneMode;
import com.wizardg.omnipipes.block.entity.PipeFilter;
import com.wizardg.omnipipes.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

// Settings for one side of a pipe. Buttons go through vanilla's clickMenuButton, filter slots through vanilla's
// slot clicks, and values sync through ContainerData, so no custom packets are needed.
public class PipeMenu extends AbstractContainerMenu {
    // Settings, each has two button ids: setting * 2 steps forward, setting * 2 + 1 steps back.
    public static final int MODE = 0;
    public static final int REDSTONE = 1;
    public static final int INSERT_CHANNEL = 2;
    public static final int EXTRACT_CHANNEL = 3;
    public static final int TAB = 4;
    public static final int WHITELIST = 5;
    public static final int COMPONENTS = 6;
    public static final int SCROLL = 7; // forward scrolls down
    public static final int DISTRIBUTION = 8;
    public static final int SPEED = 9; // forward = faster (fewer ticks)
    public static final int SPEED_BY_TEN = 10;
    // Amount buttons carry the filter slot too: AMOUNT_BASE + slot * 4 + (down ? 1 : 0) + (by ten ? 2 : 0).
    public static final int AMOUNT_BASE = 100;

    // Layout shared with PipeScreen, slot positions are relative to the panel's top left.
    public static final int PANEL_HEIGHT = 200;
    public static final int STRIP_X = 178; // upgrade strip sits just right of the 176 wide panel
    public static final int INVENTORY_Y = PANEL_HEIGHT - 82;
    public static final int GRID_X = 8;
    public static final int GRID_Y = 64;
    public static final int GRID_COLS = 9;
    public static final int GRID_ROWS = 2;

    private static final int UPGRADE_SLOTS = PipeBlockEntity.UPGRADES_PER_SIDE;
    private static final int PLAYER_END = UPGRADE_SLOTS + 36;
    private static final int GHOST_START = PLAYER_END;

    public final BlockPos pos;
    public final Direction side;
    private final @Nullable PipeBlockEntity be; // server only
    private final ContainerData sideData;
    private final ContainerData viewData;

    // Server side view state, synced to the client through viewData.
    private boolean insertTab;
    private int scroll;

    public PipeMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, inventory, extraData.readBlockPos(), extraData.readEnum(Direction.class), null,
                readSideData(extraData), new SimpleContainerData(5 + GRID_COLS * GRID_ROWS), new SimpleContainer(6 * UPGRADE_SLOTS),
                new SimpleContainer(GRID_COLS * GRID_ROWS));
    }

    // Written when the menu opens so the screen starts with the real mode and settings instead of zeros for a tick.
    public static void writeOpenData(RegistryFriendlyByteBuf buf, PipeBlockEntity be, Direction side) {
        buf.writeBlockPos(be.getBlockPos());
        buf.writeEnum(side);
        ContainerData data = be.sideData(side);
        for (int i = 0; i < data.getCount(); i++) buf.writeVarInt(data.get(i));
    }

    private static SimpleContainerData readSideData(RegistryFriendlyByteBuf buf) {
        SimpleContainerData data = new SimpleContainerData(7);
        for (int i = 0; i < data.getCount(); i++) data.set(i, buf.readVarInt());
        return data;
    }

    public PipeMenu(int containerId, Inventory inventory, PipeBlockEntity be, Direction side) {
        this(containerId, inventory, be.getBlockPos(), side, be, be.sideData(side), null, be.upgrades, null);
    }

    private PipeMenu(int containerId, Inventory inventory, BlockPos pos, Direction side, @Nullable PipeBlockEntity be,
                     ContainerData sideData, @Nullable ContainerData viewData, Container upgrades, @Nullable Container ghosts) {
        super(ModMenuTypes.PIPE_MENU.get(), containerId);
        this.pos = pos;
        this.side = side;
        this.be = be;
        this.sideData = sideData;
        this.viewData = viewData != null ? viewData : new ViewData();
        addDataSlots(sideData);
        addDataSlots(this.viewData);

        int first = side.ordinal() * UPGRADE_SLOTS;
        addSlot(new UpgradeSlot(upgrades, first, STRIP_X + 5, 5, ModTags.Items.TIER_UPGRADES));
        for (int i = 1; i < UPGRADE_SLOTS; i++)
            addSlot(new UpgradeSlot(upgrades, first + i, STRIP_X + 5, 11 + i * 18, ModTags.Items.TYPE_UPGRADES));

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, INVENTORY_Y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_Y + 58));

        Container ghostContainer = ghosts != null ? ghosts : new FilterView();
        for (int row = 0; row < GRID_ROWS; row++)
            for (int col = 0; col < GRID_COLS; col++)
                addSlot(new GhostSlot(ghostContainer, row * GRID_COLS + col, GRID_X + col * 18, GRID_Y + row * 18));
    }

    // Which upgrades a slot of the upgrade strip takes, null for any other slot.
    public static @Nullable TagKey<Item> upgradeSlotAccepts(Slot slot) {
        return slot instanceof UpgradeSlot upgrade ? upgrade.accepts : null;
    }

    public static boolean isFilterSlot(Slot slot) {
        return slot instanceof GhostSlot;
    }

    public static int buttonId(int setting, boolean forward) {
        return setting * 2 + (forward ? 0 : 1);
    }

    public static int amountButtonId(int filterSlot, boolean up, boolean byTen) {
        return AMOUNT_BASE + filterSlot * 4 + (up ? 0 : 1) + (byTen ? 2 : 0);
    }

    // Amount set on a visible filter slot (0-17), 0 if none.
    public int amount(int filterSlot) {
        return viewData.get(5 + filterSlot);
    }

    public PipeBlock.Side mode() {
        return PipeBlock.Side.values()[sideData.get(MODE)];
    }

    public RedstoneMode redstone() {
        return RedstoneMode.values()[sideData.get(REDSTONE)];
    }

    public PipeBlockEntity.Distribution distribution() {
        return PipeBlockEntity.Distribution.values()[sideData.get(4)];
    }

    // Ticks between transfers, and the fastest the installed tier allows.
    public int speed() {
        return sideData.get(5);
    }

    public int fastestSpeed() {
        return sideData.get(6);
    }

    public DyeColor insertChannel() {
        return DyeColor.byId(sideData.get(INSERT_CHANNEL));
    }

    public DyeColor extractChannel() {
        return DyeColor.byId(sideData.get(EXTRACT_CHANNEL));
    }

    // Which filter the grid shows. Single direction modes only have one, "both" follows the tab.
    public boolean showingInsert() {
        PipeBlock.Side mode = mode();
        return mode == PipeBlock.Side.INSERT || (mode == PipeBlock.Side.BOTH && viewData.get(0) == 1);
    }

    public int scroll() {
        return viewData.get(1);
    }

    public int maxScroll() {
        return Math.max(0, viewData.get(2) / GRID_COLS + 1 - GRID_ROWS); // always leaves room for one more entry
    }

    public boolean whitelist() {
        return viewData.get(3) == 1;
    }

    public boolean matchComponents() {
        return viewData.get(4) == 1;
    }

    private PipeFilter filter() {
        return be.getFilter(side, showingInsert());
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (be == null) return false;
        if (buttonId >= AMOUNT_BASE) {
            int id = buttonId - AMOUNT_BASE;
            int index = scroll * GRID_COLS + id / 4;
            if (index >= filter().entries().size()) return false;
            int change = (id % 2 == 0 ? 1 : -1) * ((id & 2) != 0 ? 10 : 1);
            filter().setAmount(index, filter().entries().get(index).amount() + change);
            return true;
        }
        int step = buttonId % 2 == 0 ? 1 : -1;
        switch (buttonId / 2) {
            case MODE -> PipeBlock.cycleMode(player.level(), pos, side, step);
            case REDSTONE -> be.cycleRedstone(side, step);
            case INSERT_CHANNEL -> be.setInsertChannel(side, Math.floorMod(be.getInsertChannel(side) + step, 16));
            case EXTRACT_CHANNEL -> be.setExtractChannel(side, Math.floorMod(be.getExtractChannel(side) + step, 16));
            case TAB -> {
                insertTab = !insertTab;
                scroll = 0;
            }
            case WHITELIST -> filter().toggleWhitelist();
            case COMPONENTS -> filter().toggleMatchComponents();
            case SCROLL -> scroll = Mth.clamp(scroll + step, 0, maxScroll());
            case DISTRIBUTION -> be.cycleDistribution(side, step);
            case SPEED -> be.changeSpeed(side, -step);
            case SPEED_BY_TEN -> be.changeSpeed(side, -step * 10);
            default -> {
                return false;
            }
        }
        return true;
    }

    // Filter slots: clicking with an item sets a copy of it, right click clears, nothing is ever taken or placed.
    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
        if (slotIndex < GHOST_START || slotIndex >= slots.size()) {
            super.clicked(slotIndex, buttonNum, input, player);
            return;
        }
        if (be == null || input != ContainerInput.PICKUP) return;
        int slot = slotIndex - GHOST_START;
        if (buttonNum == 1) {
            filter().remove(scroll * GRID_COLS + slot);
            scroll = Mth.clamp(scroll, 0, maxScroll());
        } else setFilterSlot(slot, getCarried());
    }

    // Puts a ghost copy of stack into a visible filter slot (0-17), from a click or a recipe viewer drag.
    public void setFilterSlot(int slot, ItemStack stack) {
        if (be == null || stack.isEmpty() || slot < 0 || slot >= GRID_COLS * GRID_ROWS) return;
        filter().set(scroll * GRID_COLS + slot, stack);
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    // Shift click moves upgrades between the player inventory and the side's upgrade slots.
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (slotIndex >= PLAYER_END || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = slotIndex < UPGRADE_SLOTS
                ? moveItemStackTo(stack, UPGRADE_SLOTS, PLAYER_END, true)
                : moveItemStackTo(stack, 0, UPGRADE_SLOTS, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    // Close if the pipe is gone or this side no longer connects to a block.
    @Override
    public boolean stillValid(Player player) {
        BlockState state = player.level().getBlockState(pos);
        return player.isWithinBlockInteractionRange(pos, 4.0)
                && state.getBlock() instanceof PipeBlock
                && PipeBlock.isPort(state.getValue(PipeBlock.SIDES.get(side)));
    }

    // Server side values for the filter view: 0 = insert tab, 1 = scroll, 2 = entry count, 3 = whitelist,
    // 4 = match components, 5 = amounts of the shown entries.
    private class ViewData implements ContainerData {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> insertTab ? 1 : 0;
                case 1 -> scroll;
                case 2 -> filter().entries().size();
                case 3 -> filter().isWhitelist() ? 1 : 0;
                case 4 -> filter().matchesComponents() ? 1 : 0;
                default -> {
                    int entry = scroll * GRID_COLS + index - 5;
                    var entries = filter().entries();
                    yield entry < entries.size() ? entries.get(entry).amount() : 0;
                }
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return 5 + GRID_COLS * GRID_ROWS;
        }
    }

    // Server side window into the shown filter's entries, the grid slots read through it so vanilla syncs them.
    private class FilterView extends SimpleContainer {
        FilterView() {
            super(GRID_COLS * GRID_ROWS);
        }

        @Override
        public ItemStack getItem(int slot) {
            int index = scroll * GRID_COLS + slot;
            var entries = filter().entries();
            return index < entries.size() ? entries.get(index).stack() : ItemStack.EMPTY;
        }
    }

    // Holds one upgrade from the given tag.
    private static class UpgradeSlot extends Slot {
        private final TagKey<Item> accepts;

        UpgradeSlot(Container container, int index, int x, int y, TagKey<Item> accepts) {
            super(container, index, x, y);
            this.accepts = accepts;
        }

        // Type slots also refuse an upgrade the side already has in another type slot.
        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!stack.is(accepts)) return false;
            int first = getContainerSlot() - getContainerSlot() % UPGRADE_SLOTS;
            return getContainerSlot() == first || !PipeBlockEntity.hasTypeUpgrade(container, first, stack, getContainerSlot());
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    // Shows a filter entry, clicks are handled in clicked() so items never really go in or out.
    private static class GhostSlot extends Slot {
        GhostSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
