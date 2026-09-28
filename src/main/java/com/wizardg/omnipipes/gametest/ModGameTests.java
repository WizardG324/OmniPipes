package com.wizardg.omnipipes.gametest;

import com.wizardg.omnipipes.OmniPipes;
import com.wizardg.omnipipes.block.ModBlocks;
import com.wizardg.omnipipes.block.custom.PipeBlock;
import com.wizardg.omnipipes.block.custom.PipeBlock.Side;
import com.wizardg.omnipipes.block.entity.PipeBlockEntity;
import com.wizardg.omnipipes.block.entity.PipeBlockEntity.RedstoneMode;
import com.wizardg.omnipipes.config.ServerConfig;
import com.wizardg.omnipipes.item.ModDataComponents;
import com.wizardg.omnipipes.item.ModItems;
import com.wizardg.omnipipes.screen.PipeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;


@GameTestHolder(OmniPipes.MODID)
@PrefixGameTestTemplate(false)
public class ModGameTests {
    public static void registerTests(RegisterGameTestsEvent event) {
        event.register(ModGameTests.class);
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(OmniPipes.MODID, name);
    }

    // A row of pipes along x at z=1, each with a barrel to its south at z=2 using the given mode.
    private static void pipeRow(GameTestHelper helper, Side... modes) {
        for (int x = 0; x < modes.length; x++) helper.setBlock(new BlockPos(x + 1, 1, 2), Blocks.BARREL);
        for (int x = 0; x < modes.length; x++) {
            BlockState s = ModBlocks.PIPE.get().defaultBlockState().setValue(PipeBlock.SIDES.get(Direction.SOUTH), modes[x]);
            if (x > 0) s = s.setValue(PipeBlock.SIDES.get(Direction.WEST), Side.PIPE);
            if (x < modes.length - 1) s = s.setValue(PipeBlock.SIDES.get(Direction.EAST), Side.PIPE);
            helper.setBlock(new BlockPos(x + 1, 1, 1), s);
        }
        helper.<BarrelBlockEntity>getBlockEntity(new BlockPos(1, 1, 2)).setItem(0, new ItemStack(Items.DIAMOND, 5));
    }

    private static int diamonds(GameTestHelper helper, int x) {
        return helper.<BarrelBlockEntity>getBlockEntity(new BlockPos(x, 1, 2)).countItem(Items.DIAMOND);
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void extractToInsert(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT);
        helper.succeedWhen(() -> helper.assertTrue(diamonds(helper, 2) == 5, "diamonds should reach the insert barrel"));
    }

    // Extracting on red skips the white insert barrel next to it and goes to the red one.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void channelsRoute(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT, Side.INSERT);
        helper.<PipeBlockEntity>getBlockEntity(new BlockPos(1, 1, 1)).setExtractChannel(Direction.SOUTH, DyeColor.RED.getId());
        helper.<PipeBlockEntity>getBlockEntity(new BlockPos(3, 1, 1)).setInsertChannel(Direction.SOUTH, DyeColor.RED.getId());
        helper.onEachTick(() -> helper.assertTrue(diamonds(helper, 2) == 0, "white insert should not get red channel items"));
        helper.succeedWhen(() -> helper.assertTrue(diamonds(helper, 3) == 5, "red insert should get the items"));
    }

    // A "both" side receives from an extract side like any insert side.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void bothReceives(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.BOTH);
        helper.succeedWhen(() -> helper.assertTrue(diamonds(helper, 2) == 5, "both side should receive"));
    }

    // Source starts empty, so the first extract fails and the pipe should wait extractRecheckDelay (50) ticks.
    // An empty source fails the first try and each retry at normal speed, then the side pauses for the recheck
    // delay: diamonds added after the last retry wait out the pause, not just one normal step.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void extractRecheckDelay(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT);
        BarrelBlockEntity source = helper.<BarrelBlockEntity>getBlockEntity(new BlockPos(1, 1, 2));
        source.clearContent();
        int speed = ServerConfig.base.transferRate().get();
        int lastRetry = ServerConfig.retriesBeforePausing.get() * speed;
        int pause = ServerConfig.extractRecheckDelay.get();
        helper.startSequence()
                .thenIdle(lastRetry + 5)
                .thenExecute(() -> source.setItem(0, new ItemStack(Items.DIAMOND, 5)))
                .thenExecuteFor(pause - 10, () -> helper.assertTrue(diamonds(helper, 1) == 5, "pipe should be paused"))
                .thenWaitUntil(() -> helper.assertTrue(diamonds(helper, 2) == 5, "pipe should resume after the pause"))
                .thenSucceed();
    }

    // North has a plate (insert), up has a plain arm, the rest is just the 4px core.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void pipeShape(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.PIPE.get().defaultBlockState()
                .setValue(PipeBlock.SIDES.get(Direction.NORTH), Side.INSERT)
                .setValue(PipeBlock.SIDES.get(Direction.UP), Side.PIPE));
        var box = helper.getBlockState(pos).getShape(helper.getLevel(), helper.absolutePos(pos)).bounds();
        helper.assertTrue(box.minZ == 0 && box.maxZ == 10 / 16.0, "north arm should reach the block edge");
        helper.assertTrue(box.minX == 2.5 / 16.0 && box.maxX == 13.5 / 16.0, "north plate should be 11px wide");
        helper.assertTrue(box.minY == 2.5 / 16.0 && box.maxY == 1, "up arm should reach the top, plate sets the bottom");
        helper.succeed();
    }

    // A base pipe links to another base pipe but not to a red one next to it.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void sameTypeConnects(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.PIPE.get());
        helper.setBlock(pos.east(), ModBlocks.COLORED_PIPES.get(DyeColor.RED).get());
        helper.setBlock(pos.south(), ModBlocks.PIPE.get());
        BlockState state = helper.getBlockState(pos);
        helper.assertTrue(state.getValue(PipeBlock.SIDES.get(Direction.EAST)) == Side.NONE, "base pipe should not connect to red");
        helper.assertTrue(state.getValue(PipeBlock.SIDES.get(Direction.SOUTH)) == Side.PIPE, "base pipe should connect to base");
        helper.succeed();
    }

    // An extract side set to "Run on High Signal" waits until a redstone block powers the pipe.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void redstoneHighSignal(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT);
        BlockPos pipe = new BlockPos(1, 1, 1);
        helper.<PipeBlockEntity>getBlockEntity(pipe).setRedstone(Direction.SOUTH, RedstoneMode.HIGH_SIGNAL);
        helper.startSequence()
                .thenExecuteFor(20, () -> helper.assertTrue(diamonds(helper, 1) == 5, "unpowered pipe should not extract"))
                .thenExecute(() -> helper.setBlock(pipe.above(), Blocks.REDSTONE_BLOCK))
                .thenWaitUntil(() -> helper.assertTrue(diamonds(helper, 2) == 5, "powered pipe should extract"))
                .thenSucceed();
    }

    // The GUI buttons change the side's settings on the server, right click (back) wraps around.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void menuButtons(GameTestHelper helper) {
        pipeRow(helper, Side.INSERT);
        BlockPos pipe = new BlockPos(1, 1, 1);
        PipeBlockEntity be = helper.<PipeBlockEntity>getBlockEntity(pipe);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        PipeMenu menu = new PipeMenu(0, player.getInventory(), be, Direction.SOUTH);

        menu.clickMenuButton(player, PipeMenu.buttonId(PipeMenu.MODE, true));
        menu.clickMenuButton(player, PipeMenu.buttonId(PipeMenu.REDSTONE, false));
        menu.clickMenuButton(player, PipeMenu.buttonId(PipeMenu.EXTRACT_CHANNEL, false));
        helper.assertTrue(menu.mode() == Side.EXTRACT, "left click should step insert -> extract");
        helper.assertTrue(menu.redstone() == RedstoneMode.OFF, "right click should step disabled back to off");
        helper.assertTrue(menu.extractChannel() == DyeColor.BLACK, "right click should step white back to black");
        helper.succeed();
    }

    // Puts a tier upgrade on the first pipe's extract side, fills its barrel with 64 diamonds and
    // checks the first transfer moves exactly the expected amount.
    private static void firstTransferMoves(GameTestHelper helper, ItemStack tier, int expected) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT);
        helper.<BarrelBlockEntity>getBlockEntity(new BlockPos(1, 1, 2)).setItem(0, new ItemStack(Items.DIAMOND, 64));
        helper.<PipeBlockEntity>getBlockEntity(new BlockPos(1, 1, 1)).upgrades
                .setItem(Direction.SOUTH.ordinal() * PipeBlockEntity.UPGRADES_PER_SIDE, tier);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(diamonds(helper, 2) > 0, "nothing moved"))
                .thenExecute(() -> helper.assertTrue(diamonds(helper, 2) == expected,
                        "first transfer moved " + diamonds(helper, 2) + ", expected " + expected))
                .thenSucceed();
    }

    // Tier 1 uses the upgrade_tier_1 config amount instead of the base one.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void tier1UpgradeRate(GameTestHelper helper) {
        firstTransferMoves(helper, new ItemStack(ModItems.TIER_UPGRADES.get(0).get()), ServerConfig.upgradeTiers.get(0).itemTransferRate().get());
    }

    // Creative upgrade moves everything at once.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void creativeUpgradeRate(GameTestHelper helper) {
        firstTransferMoves(helper, new ItemStack(ModItems.CREATIVE_UPGRADE.get()), 64);
    }

    // Shift right click install: takes one item, swaps tiers, and says why when it refuses.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void installUpgrade(GameTestHelper helper) {
        pipeRow(helper, Side.INSERT);
        PipeBlockEntity be = helper.<PipeBlockEntity>getBlockEntity(new BlockPos(1, 1, 1));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        int first = Direction.SOUTH.ordinal() * PipeBlockEntity.UPGRADES_PER_SIDE;
        ItemStack tier1 = new ItemStack(ModItems.TIER_UPGRADES.get(0).get(), 3);
        ItemStack tier2 = new ItemStack(ModItems.TIER_UPGRADES.get(1).get());
        ItemStack fluid = new ItemStack(ModItems.FLUID_UPGRADE.get(), 2);

        helper.assertTrue(be.installUpgrade(Direction.SOUTH, tier1, player) == null && tier1.getCount() == 2, "tier 1 should install and use one item");
        helper.assertTrue("message.omni_pipes.upgrade.same_tier".equals(be.installUpgrade(Direction.SOUTH, tier1, player)), "same tier should say it already exists");
        helper.assertTrue(be.installUpgrade(Direction.SOUTH, tier2, player) == null && be.upgrades.getItem(first).is(ModItems.TIER_UPGRADES.get(1).get()), "tier 2 should swap in");
        helper.assertTrue(player.getInventory().countItem(ModItems.TIER_UPGRADES.get(0).get()) == 1, "swapped tier 1 should go back to the player");
        helper.assertTrue(be.installUpgrade(Direction.SOUTH, fluid, player) == null && be.upgrades.getItem(first + 1).is(ModItems.FLUID_UPGRADE.get()), "fluid should go in the first type slot");
        helper.assertTrue("message.omni_pipes.upgrade.same_type".equals(be.installUpgrade(Direction.SOUTH, fluid, player)) && fluid.getCount() == 1, "a second fluid upgrade should be refused");
        helper.succeed();
    }

    private static int count(GameTestHelper helper, int x, net.minecraft.world.item.Item item) {
        return helper.<BarrelBlockEntity>getBlockEntity(new BlockPos(x, 1, 2)).countItem(item);
    }

    // Extract whitelist with diamonds: the diamonds move, the dirt next to them stays.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void extractWhitelist(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT);
        helper.<BarrelBlockEntity>getBlockEntity(new BlockPos(1, 1, 2)).setItem(1, new ItemStack(Items.DIRT, 5));
        var filter = helper.<PipeBlockEntity>getBlockEntity(new BlockPos(1, 1, 1)).getFilter(Direction.SOUTH, false);
        filter.toggleWhitelist();
        filter.set(0, new ItemStack(Items.DIAMOND));
        helper.onEachTick(() -> helper.assertTrue(count(helper, 2, Items.DIRT) == 0, "dirt is not whitelisted"));
        helper.succeedWhen(() -> helper.assertTrue(diamonds(helper, 2) == 5, "whitelisted diamonds should move"));
    }

    // Insert blacklist with diamonds on the first target: the diamonds skip it and go to the next one.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void insertBlacklist(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT, Side.INSERT);
        helper.<PipeBlockEntity>getBlockEntity(new BlockPos(2, 1, 1)).getFilter(Direction.SOUTH, true)
                .set(0, new ItemStack(Items.DIAMOND));
        helper.onEachTick(() -> helper.assertTrue(diamonds(helper, 2) == 0, "blacklisted insert should get no diamonds"));
        helper.succeedWhen(() -> helper.assertTrue(diamonds(helper, 3) == 5, "diamonds should reach the next insert"));
    }

    // Clicking a filter slot with an item copies it in (nothing is taken), right click removes it.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void filterSlotClicks(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT);
        PipeBlockEntity be = helper.<PipeBlockEntity>getBlockEntity(new BlockPos(1, 1, 1));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        PipeMenu menu = new PipeMenu(0, player.getInventory(), be, Direction.SOUTH);
        int firstFilterSlot = menu.slots.size() - PipeMenu.GRID_COLS * PipeMenu.GRID_ROWS;
        var filter = be.getFilter(Direction.SOUTH, false);

        menu.setCarried(new ItemStack(Items.DIAMOND, 3));
        menu.clicked(firstFilterSlot + 5, 0, ClickType.PICKUP, player);
        helper.assertTrue(filter.entries().size() == 1 && filter.entries().get(0).stack().is(Items.DIAMOND), "diamond should be added to the filter");
        helper.assertTrue(menu.getCarried().getCount() == 3, "the carried stack should not be used up");

        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(firstFilterSlot, 1, ClickType.PICKUP, player);
        helper.assertTrue(filter.entries().isEmpty(), "right click should remove the entry");
        helper.succeed();
    }

    // Whitelist with an amount on the given pipe's filter, 5 diamonds start in the first barrel.
    private static void stockSetup(GameTestHelper helper, boolean insert, int amount) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT);
        var filter = helper.<PipeBlockEntity>getBlockEntity(new BlockPos(insert ? 2 : 1, 1, 1)).getFilter(Direction.SOUTH, insert);
        filter.toggleWhitelist();
        filter.set(0, new ItemStack(Items.DIAMOND));
        filter.setAmount(0, amount);
    }

    // Insert amount 3: the target is filled up to 3 and then stays there.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void insertStock(GameTestHelper helper) {
        stockSetup(helper, true, 3);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(diamonds(helper, 2) == 3, "target should be filled up to 3"))
                .thenExecuteFor(60, () -> helper.assertTrue(diamonds(helper, 2) == 3 && diamonds(helper, 1) == 2, "target should stay at 3"))
                .thenSucceed();
    }

    // Extract amount 2: each transfer moves 2 diamonds (the base rate would move all 5 at once).
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void extractBatch(GameTestHelper helper) {
        stockSetup(helper, false, 2);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(diamonds(helper, 2) > 0, "nothing moved"))
                .thenExecute(() -> helper.assertTrue(diamonds(helper, 2) == 2, "first transfer should move 2, moved " + diamonds(helper, 2)))
                .thenWaitUntil(() -> helper.assertTrue(diamonds(helper, 2) == 5, "the rest should follow in later transfers"))
                .thenSucceed();
    }

    // Furthest: the far insert barrel gets the diamonds, the near one stays empty.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void distributionFurthest(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT, Side.INSERT);
        helper.<PipeBlockEntity>getBlockEntity(new BlockPos(1, 1, 1)).setDistribution(Direction.SOUTH, PipeBlockEntity.Distribution.FURTHEST);
        helper.onEachTick(() -> helper.assertTrue(diamonds(helper, 2) == 0, "near insert should get nothing"));
        helper.succeedWhen(() -> helper.assertTrue(diamonds(helper, 3) == 5, "far insert should get the diamonds"));
    }

    // Round-robin with 1 diamond per transfer: the first two transfers go to different barrels.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void distributionRoundRobin(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT, Side.INSERT);
        PipeBlockEntity be = helper.<PipeBlockEntity>getBlockEntity(new BlockPos(1, 1, 1));
        be.setDistribution(Direction.SOUTH, PipeBlockEntity.Distribution.ROUND_ROBIN);
        var filter = be.getFilter(Direction.SOUTH, false);
        filter.toggleWhitelist();
        filter.set(0, new ItemStack(Items.DIAMOND));
        filter.setAmount(0, 1);
        helper.succeedWhen(() -> helper.assertTrue(diamonds(helper, 2) == 1 && diamonds(helper, 3) == 1, "each insert should get one diamond"));
    }

    // Speed stays between the tier's fastest and 200 ticks, a slower speed spaces transfers out,
    // and going back to the fastest follows the tier.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void speedSetting(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT);
        helper.<BarrelBlockEntity>getBlockEntity(new BlockPos(1, 1, 2)).setItem(0, new ItemStack(Items.DIAMOND, 64));
        PipeBlockEntity be = helper.<PipeBlockEntity>getBlockEntity(new BlockPos(1, 1, 1));
        be.changeSpeed(Direction.SOUTH, -1000);
        helper.assertTrue(be.getSpeed(Direction.SOUTH) == ServerConfig.base.transferRate().get(), "fastest without a tier should be the base rate");
        be.changeSpeed(Direction.SOUTH, 1000);
        helper.assertTrue(be.getSpeed(Direction.SOUTH) == PipeBlockEntity.SLOWEST_SPEED, "slowest should be 200 ticks");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(diamonds(helper, 2) == 8, "first transfer should happen right away"))
                .thenExecuteFor(150, () -> helper.assertTrue(diamonds(helper, 2) == 8, "next transfer should wait 200 ticks"))
                .thenExecute(() -> {
                    be.changeSpeed(Direction.SOUTH, -1000);
                    be.upgrades.setItem(Direction.SOUTH.ordinal() * PipeBlockEntity.UPGRADES_PER_SIDE, new ItemStack(ModItems.TIER_UPGRADES.get(3).get()));
                    helper.assertTrue(be.getSpeed(Direction.SOUTH) == ServerConfig.upgradeTiers.get(3).transferRate().get(), "fastest should follow the tier 4 upgrade");
                })
                .thenSucceed();
    }

    // A water bucket in a blacklist blocks water: the full cauldron skips the first insert and fills the second.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void fluidFilter(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT, Side.INSERT);
        helper.setBlock(new BlockPos(1, 1, 2), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.CAULDRON);
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.CAULDRON);
        helper.<PipeBlockEntity>getBlockEntity(new BlockPos(1, 1, 1)).upgrades
                .setItem(Direction.SOUTH.ordinal() * PipeBlockEntity.UPGRADES_PER_SIDE + 1, new ItemStack(ModItems.FLUID_UPGRADE.get()));
        helper.<PipeBlockEntity>getBlockEntity(new BlockPos(2, 1, 1)).getFilter(Direction.SOUTH, true)
                .set(0, new ItemStack(Items.WATER_BUCKET));
        helper.onEachTick(() -> helper.assertBlockPresent(Blocks.CAULDRON, new BlockPos(2, 1, 2)));
        helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.WATER_CAULDRON, new BlockPos(3, 1, 2)));
    }

    // Two "both" sides on different channels keep moving after their block entities are saved and loaded again,
    // like rejoining a world: first extracts on channel 1 into the second, the second extracts on 2 (nowhere).
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void reloadKeepsWorking(GameTestHelper helper) {
        pipeRow(helper, Side.BOTH, Side.BOTH);
        PipeBlockEntity first = helper.<PipeBlockEntity>getBlockEntity(new BlockPos(1, 1, 1));
        PipeBlockEntity second = helper.<PipeBlockEntity>getBlockEntity(new BlockPos(2, 1, 1));
        first.setExtractChannel(Direction.SOUTH, 1);
        second.setInsertChannel(Direction.SOUTH, 1);
        second.setExtractChannel(Direction.SOUTH, 2);
        reload(helper, new BlockPos(1, 1, 1));
        reload(helper, new BlockPos(2, 1, 1));
        helper.succeedWhen(() -> helper.assertTrue(diamonds(helper, 2) == 5, "diamonds should still reach the second barrel"));
    }

    private static void reload(GameTestHelper helper, BlockPos pos) {
        var level = helper.getLevel();
        BlockPos abs = helper.absolutePos(pos);
        var tag = level.getBlockEntity(abs).saveWithFullMetadata(level.registryAccess());
        level.removeBlockEntity(abs);
        level.setBlockEntity(net.minecraft.world.level.block.entity.BlockEntity.loadStatic(abs, level.getBlockState(abs), tag, level.registryAccess()));
    }

    // A pipe set to the slowest speed keeps waiting after a reload instead of transferring again right away.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void timerSurvivesReload(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT);
        helper.<BarrelBlockEntity>getBlockEntity(new BlockPos(1, 1, 2)).setItem(0, new ItemStack(Items.DIAMOND, 64));
        helper.<PipeBlockEntity>getBlockEntity(new BlockPos(1, 1, 1)).changeSpeed(Direction.SOUTH, 1000);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(diamonds(helper, 2) > 0, "first transfer should happen right away"))
                .thenExecute(() -> reload(helper, new BlockPos(1, 1, 1)))
                .thenExecuteFor(100, () -> helper.assertTrue(diamonds(helper, 2) == 8, "reload should not reset the wait"))
                .thenSucceed();
    }

    private static ItemStack tagFilter(String... tags) {
        ItemStack stack = new ItemStack(ModItems.TAG_FILTER.get());
        stack.set(ModDataComponents.TAGS, java.util.Arrays.stream(tags).map(ResourceLocation::parse).toList());
        return stack;
    }

    // Extract whitelist holding a Tag Filter for c:gems/diamond: the diamonds move, the emeralds stay.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void tagFilterItems(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT);
        helper.<BarrelBlockEntity>getBlockEntity(new BlockPos(1, 1, 2)).setItem(1, new ItemStack(Items.EMERALD, 3));
        var filter = helper.<PipeBlockEntity>getBlockEntity(new BlockPos(1, 1, 1)).getFilter(Direction.SOUTH, false);
        filter.toggleWhitelist();
        filter.set(0, tagFilter("c:gems/diamond"));
        helper.onEachTick(() -> helper.assertTrue(count(helper, 2, Items.EMERALD) == 0, "emeralds are not in the tag"));
        helper.succeedWhen(() -> helper.assertTrue(diamonds(helper, 2) == 5, "diamonds should pass the tag filter"));
    }

    // A Tag Filter for the minecraft:water fluid tag in a blacklist blocks water, like a water bucket would.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void tagFilterFluids(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT, Side.INSERT);
        helper.setBlock(new BlockPos(1, 1, 2), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.CAULDRON);
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.CAULDRON);
        helper.<PipeBlockEntity>getBlockEntity(new BlockPos(1, 1, 1)).upgrades
                .setItem(Direction.SOUTH.ordinal() * PipeBlockEntity.UPGRADES_PER_SIDE + 1, new ItemStack(ModItems.FLUID_UPGRADE.get()));
        helper.<PipeBlockEntity>getBlockEntity(new BlockPos(2, 1, 1)).getFilter(Direction.SOUTH, true).set(0, tagFilter("minecraft:water"));
        helper.onEachTick(() -> helper.assertBlockPresent(Blocks.CAULDRON, new BlockPos(2, 1, 2)));
        helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.WATER_CAULDRON, new BlockPos(3, 1, 2)));
    }

    // Copying an extract connection onto an insert one brings the mode, settings and filter along.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void configuratorCopyPaste(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT);
        PipeBlockEntity from = helper.<PipeBlockEntity>getBlockEntity(new BlockPos(1, 1, 1));
        PipeBlockEntity to = helper.<PipeBlockEntity>getBlockEntity(new BlockPos(2, 1, 1));
        from.setExtractChannel(Direction.SOUTH, 5);
        from.setDistribution(Direction.SOUTH, PipeBlockEntity.Distribution.FURTHEST);
        from.changeSpeed(Direction.SOUTH, 20);
        from.getFilter(Direction.SOUTH, false).toggleWhitelist();
        from.getFilter(Direction.SOUTH, false).set(0, new ItemStack(Items.DIAMOND));
        to.pasteSettings(Direction.SOUTH, from.copySettings(Direction.SOUTH));
        var filter = to.getFilter(Direction.SOUTH, false);
        helper.assertTrue(to.getBlockState().getValue(PipeBlock.SIDES.get(Direction.SOUTH)) == Side.EXTRACT, "mode should be pasted");
        helper.assertTrue(to.getExtractChannel(Direction.SOUTH) == 5, "channel should be pasted");
        helper.assertTrue(to.getDistribution(Direction.SOUTH) == PipeBlockEntity.Distribution.FURTHEST, "distribution should be pasted");
        helper.assertTrue(to.getSpeed(Direction.SOUTH) == from.getSpeed(Direction.SOUTH), "speed should be pasted");
        helper.assertTrue(filter.isWhitelist() && filter.entries().size() == 1 && filter.entries().getFirst().stack().is(Items.DIAMOND),
                "filter should be pasted");
        helper.succeed();
    }

    // Switching off the side between two pipes splits them on both ends, switching it back on reconnects them.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void configuratorDisable(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT);
        BlockPos first = new BlockPos(1, 1, 1), second = new BlockPos(2, 1, 1);
        PipeBlockEntity be = helper.<PipeBlockEntity>getBlockEntity(first);
        PipeBlock block = (PipeBlock) be.getBlockState().getBlock();
        block.toggleSide(helper.getLevel(), helper.absolutePos(first), Direction.EAST, be);
        helper.assertTrue(helper.getBlockState(first).getValue(PipeBlock.SIDES.get(Direction.EAST)) == Side.NONE, "disabled side should drop its arm");
        helper.assertTrue(helper.getBlockState(second).getValue(PipeBlock.SIDES.get(Direction.WEST)) == Side.NONE, "neighbor should disconnect too");
        helper.startSequence()
                .thenExecuteFor(60, () -> helper.assertTrue(diamonds(helper, 2) == 0, "split pipes should not transfer"))
                .thenExecute(() -> {
                    block.toggleSide(helper.getLevel(), helper.absolutePos(first), Direction.EAST, be);
                    // The extract connection keeps its mode through being switched off and on.
                    block.toggleSide(helper.getLevel(), helper.absolutePos(first), Direction.SOUTH, be);
                    block.toggleSide(helper.getLevel(), helper.absolutePos(first), Direction.SOUTH, be);
                    helper.assertTrue(helper.getBlockState(first).getValue(PipeBlock.SIDES.get(Direction.SOUTH)) == Side.EXTRACT,
                            "re-enabled connection should keep its mode");
                })
                .thenWaitUntil(() -> helper.assertTrue(diamonds(helper, 2) == 5, "re-enabled pipes should transfer again"))
                .thenSucceed();
    }

    // Dismantling removes the pipe and hands it back with its upgrade, the pipe joins the existing stack of pipes in the
    // main inventory before taking a free hotbar slot.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void configuratorDismantle(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT);
        BlockPos pos = new BlockPos(1, 1, 1);
        PipeBlockEntity be = helper.<PipeBlockEntity>getBlockEntity(pos);
        be.upgrades.setItem(Direction.SOUTH.ordinal() * PipeBlockEntity.UPGRADES_PER_SIDE, new ItemStack(ModItems.TIER_UPGRADES.getFirst().get()));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(20, new ItemStack(ModBlocks.PIPE.get(), 5));
        PipeBlock.dismantle(player, helper.getLevel(), helper.absolutePos(pos), helper.getBlockState(pos), be);
        helper.assertBlockPresent(Blocks.AIR, pos);
        helper.assertTrue(player.getInventory().getItem(20).getCount() == 6, "pipe should join the existing stack");
        helper.assertTrue(player.getInventory().getItem(0).is(ModItems.TIER_UPGRADES.getFirst().get()), "upgrade should go to the first hotbar slot");

        // With a full inventory the pipe drops where it was.
        helper.setBlock(pos, ModBlocks.PIPE.get());
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
        PipeBlock.dismantle(player, helper.getLevel(), helper.absolutePos(pos), helper.getBlockState(pos), helper.<PipeBlockEntity>getBlockEntity(pos));
        helper.assertItemEntityPresent(ModBlocks.PIPE.get().asItem(), pos, 1);
        helper.succeed();
    }

    // The part each side of the pipe model uses: stripes continue a straight run of plain pipe, everything else
    // without an arm gets the box joint, arms and connections use their own part.
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void modelParts(GameTestHelper helper) {
        BlockState lone = ModBlocks.PIPE.get().defaultBlockState();
        BlockState straight = lone.setValue(PipeBlock.SIDES.get(Direction.EAST), Side.PIPE).setValue(PipeBlock.SIDES.get(Direction.WEST), Side.PIPE);
        BlockState branch = straight.setValue(PipeBlock.SIDES.get(Direction.NORTH), Side.EXTRACT);
        helper.assertTrue(PipeBlock.modelPart(lone, Direction.UP).equals("core_box"), "a lone pipe shows the box");
        helper.assertTrue(PipeBlock.modelPart(straight, Direction.UP).equals("core_u"), "top of an east-west run runs along u");
        helper.assertTrue(PipeBlock.modelPart(straight, Direction.NORTH).equals("core_u"), "front of an east-west run runs along u");
        helper.assertTrue(PipeBlock.modelPart(straight.setValue(PipeBlock.SIDES.get(Direction.EAST), Side.NONE)
                .setValue(PipeBlock.SIDES.get(Direction.WEST), Side.NONE).setValue(PipeBlock.SIDES.get(Direction.UP), Side.PIPE)
                .setValue(PipeBlock.SIDES.get(Direction.DOWN), Side.PIPE), Direction.NORTH).equals("core_v"), "front of a vertical run runs along v");
        helper.assertTrue(PipeBlock.modelPart(branch, Direction.UP).equals("core_box"), "a connection breaks the straight run");
        helper.assertTrue(PipeBlock.modelPart(branch, Direction.NORTH).equals("arm_extract"), "connections use their mode's part");
        helper.assertTrue(PipeBlock.modelPart(branch, Direction.EAST).equals("arm"), "pipe sides use the arm");
        helper.succeed();
    }
}
