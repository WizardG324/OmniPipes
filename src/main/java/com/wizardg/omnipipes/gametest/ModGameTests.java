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
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
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
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

public class ModGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, OmniPipes.MODID);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> EXTRACT_TO_INSERT =
            TEST_FUNCTIONS.register("extract_to_insert", () -> ModGameTests::extractToInsert);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CHANNELS_ROUTE =
            TEST_FUNCTIONS.register("channels_route", () -> ModGameTests::channelsRoute);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BOTH_RECEIVES =
            TEST_FUNCTIONS.register("both_receives", () -> ModGameTests::bothReceives);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> EXTRACT_RECHECK_DELAY =
            TEST_FUNCTIONS.register("extract_recheck_delay", () -> ModGameTests::extractRecheckDelay);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PIPE_SHAPE =
            TEST_FUNCTIONS.register("pipe_shape", () -> ModGameTests::pipeShape);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SAME_TYPE_CONNECTS =
            TEST_FUNCTIONS.register("same_type_connects", () -> ModGameTests::sameTypeConnects);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REDSTONE_HIGH_SIGNAL =
            TEST_FUNCTIONS.register("redstone_high_signal", () -> ModGameTests::redstoneHighSignal);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MENU_BUTTONS =
            TEST_FUNCTIONS.register("menu_buttons", () -> ModGameTests::menuButtons);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TIER_1_UPGRADE_RATE =
            TEST_FUNCTIONS.register("tier_1_upgrade_rate", () -> ModGameTests::tier1UpgradeRate);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CREATIVE_UPGRADE_RATE =
            TEST_FUNCTIONS.register("creative_upgrade_rate", () -> ModGameTests::creativeUpgradeRate);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INSTALL_UPGRADE =
            TEST_FUNCTIONS.register("install_upgrade", () -> ModGameTests::installUpgrade);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> EXTRACT_WHITELIST =
            TEST_FUNCTIONS.register("extract_whitelist", () -> ModGameTests::extractWhitelist);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INSERT_BLACKLIST =
            TEST_FUNCTIONS.register("insert_blacklist", () -> ModGameTests::insertBlacklist);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FILTER_SLOT_CLICKS =
            TEST_FUNCTIONS.register("filter_slot_clicks", () -> ModGameTests::filterSlotClicks);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INSERT_STOCK =
            TEST_FUNCTIONS.register("insert_stock", () -> ModGameTests::insertStock);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> EXTRACT_BATCH =
            TEST_FUNCTIONS.register("extract_batch", () -> ModGameTests::extractBatch);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DISTRIBUTION_FURTHEST =
            TEST_FUNCTIONS.register("distribution_furthest", () -> ModGameTests::distributionFurthest);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DISTRIBUTION_ROUND_ROBIN =
            TEST_FUNCTIONS.register("distribution_round_robin", () -> ModGameTests::distributionRoundRobin);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SPEED_SETTING =
            TEST_FUNCTIONS.register("speed_setting", () -> ModGameTests::speedSetting);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FLUID_FILTER =
            TEST_FUNCTIONS.register("fluid_filter", () -> ModGameTests::fluidFilter);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RELOAD_KEEPS_WORKING =
            TEST_FUNCTIONS.register("reload_keeps_working", () -> ModGameTests::reloadKeepsWorking);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TIMER_SURVIVES_RELOAD =
            TEST_FUNCTIONS.register("timer_survives_reload", () -> ModGameTests::timerSurvivesReload);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TAG_FILTER_ITEMS =
            TEST_FUNCTIONS.register("tag_filter_items", () -> ModGameTests::tagFilterItems);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TAG_FILTER_FLUIDS =
            TEST_FUNCTIONS.register("tag_filter_fluids", () -> ModGameTests::tagFilterFluids);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CONFIGURATOR_COPY_PASTE =
            TEST_FUNCTIONS.register("configurator_copy_paste", () -> ModGameTests::configuratorCopyPaste);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CONFIGURATOR_DISABLE =
            TEST_FUNCTIONS.register("configurator_disable", () -> ModGameTests::configuratorDisable);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CONFIGURATOR_DISMANTLE =
            TEST_FUNCTIONS.register("configurator_dismantle", () -> ModGameTests::configuratorDismantle);

    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("default"));
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 200, 1, true, Rotation.NONE, false, 1, 1, false, 8);

        event.registerTest(id("extract_to_insert"), new FunctionGameTestInstance(EXTRACT_TO_INSERT.getKey(), testData));
        event.registerTest(id("channels_route"), new FunctionGameTestInstance(CHANNELS_ROUTE.getKey(), testData));
        event.registerTest(id("both_receives"), new FunctionGameTestInstance(BOTH_RECEIVES.getKey(), testData));
        event.registerTest(id("same_type_connects"), new FunctionGameTestInstance(SAME_TYPE_CONNECTS.getKey(), testData));
        event.registerTest(id("redstone_high_signal"), new FunctionGameTestInstance(REDSTONE_HIGH_SIGNAL.getKey(), testData));
        event.registerTest(id("menu_buttons"), new FunctionGameTestInstance(MENU_BUTTONS.getKey(), testData));
        event.registerTest(id("tier_1_upgrade_rate"), new FunctionGameTestInstance(TIER_1_UPGRADE_RATE.getKey(), testData));
        event.registerTest(id("creative_upgrade_rate"), new FunctionGameTestInstance(CREATIVE_UPGRADE_RATE.getKey(), testData));
        event.registerTest(id("install_upgrade"), new FunctionGameTestInstance(INSTALL_UPGRADE.getKey(), testData));
        event.registerTest(id("extract_whitelist"), new FunctionGameTestInstance(EXTRACT_WHITELIST.getKey(), testData));
        event.registerTest(id("insert_blacklist"), new FunctionGameTestInstance(INSERT_BLACKLIST.getKey(), testData));
        event.registerTest(id("filter_slot_clicks"), new FunctionGameTestInstance(FILTER_SLOT_CLICKS.getKey(), testData));
        event.registerTest(id("insert_stock"), new FunctionGameTestInstance(INSERT_STOCK.getKey(), testData));
        event.registerTest(id("extract_batch"), new FunctionGameTestInstance(EXTRACT_BATCH.getKey(), testData));
        event.registerTest(id("distribution_furthest"), new FunctionGameTestInstance(DISTRIBUTION_FURTHEST.getKey(), testData));
        event.registerTest(id("distribution_round_robin"), new FunctionGameTestInstance(DISTRIBUTION_ROUND_ROBIN.getKey(), testData));
        event.registerTest(id("speed_setting"), new FunctionGameTestInstance(SPEED_SETTING.getKey(), testData));
        event.registerTest(id("fluid_filter"), new FunctionGameTestInstance(FLUID_FILTER.getKey(), testData));
        event.registerTest(id("reload_keeps_working"), new FunctionGameTestInstance(RELOAD_KEEPS_WORKING.getKey(), testData));
        event.registerTest(id("timer_survives_reload"), new FunctionGameTestInstance(TIMER_SURVIVES_RELOAD.getKey(), testData));
        event.registerTest(id("tag_filter_items"), new FunctionGameTestInstance(TAG_FILTER_ITEMS.getKey(), testData));
        event.registerTest(id("tag_filter_fluids"), new FunctionGameTestInstance(TAG_FILTER_FLUIDS.getKey(), testData));
        event.registerTest(id("configurator_copy_paste"), new FunctionGameTestInstance(CONFIGURATOR_COPY_PASTE.getKey(), testData));
        event.registerTest(id("configurator_disable"), new FunctionGameTestInstance(CONFIGURATOR_DISABLE.getKey(), testData));
        event.registerTest(id("configurator_dismantle"), new FunctionGameTestInstance(CONFIGURATOR_DISMANTLE.getKey(), testData));
        event.registerTest(id("pipe_shape"), new FunctionGameTestInstance(PIPE_SHAPE.getKey(), testData));
        event.registerTest(id("extract_recheck_delay"), new FunctionGameTestInstance(EXTRACT_RECHECK_DELAY.getKey(), testData));
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(OmniPipes.MODID, name);
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
        helper.getBlockEntity(new BlockPos(1, 1, 2), BarrelBlockEntity.class).setItem(0, new ItemStack(Items.DIAMOND, 5));
    }

    private static int diamonds(GameTestHelper helper, int x) {
        return helper.getBlockEntity(new BlockPos(x, 1, 2), BarrelBlockEntity.class).countItem(Items.DIAMOND);
    }

    private static void extractToInsert(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT);
        helper.succeedWhen(() -> helper.assertTrue(diamonds(helper, 2) == 5, "diamonds should reach the insert barrel"));
    }

    // Extracting on red skips the white insert barrel next to it and goes to the red one.
    private static void channelsRoute(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT, Side.INSERT);
        helper.getBlockEntity(new BlockPos(1, 1, 1), PipeBlockEntity.class).setExtractChannel(Direction.SOUTH, DyeColor.RED.getId());
        helper.getBlockEntity(new BlockPos(3, 1, 1), PipeBlockEntity.class).setInsertChannel(Direction.SOUTH, DyeColor.RED.getId());
        helper.onEachTick(() -> helper.assertTrue(diamonds(helper, 2) == 0, "white insert should not get red channel items"));
        helper.succeedWhen(() -> helper.assertTrue(diamonds(helper, 3) == 5, "red insert should get the items"));
    }

    // A "both" side receives from an extract side like any insert side.
    private static void bothReceives(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.BOTH);
        helper.succeedWhen(() -> helper.assertTrue(diamonds(helper, 2) == 5, "both side should receive"));
    }

    // Source starts empty, so the first extract fails and the pipe should wait extractRecheckDelay (50) ticks.
    // An empty source fails the first try and each retry at normal speed, then the side pauses for the recheck
    // delay: diamonds added after the last retry wait out the pause, not just one normal step.
    private static void extractRecheckDelay(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT);
        BarrelBlockEntity source = helper.getBlockEntity(new BlockPos(1, 1, 2), BarrelBlockEntity.class);
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
    private static void pipeShape(GameTestHelper helper) {
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
    private static void sameTypeConnects(GameTestHelper helper) {
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
    private static void redstoneHighSignal(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT);
        BlockPos pipe = new BlockPos(1, 1, 1);
        helper.getBlockEntity(pipe, PipeBlockEntity.class).setRedstone(Direction.SOUTH, RedstoneMode.HIGH_SIGNAL);
        helper.startSequence()
                .thenExecuteFor(20, () -> helper.assertTrue(diamonds(helper, 1) == 5, "unpowered pipe should not extract"))
                .thenExecute(() -> helper.setBlock(pipe.above(), Blocks.REDSTONE_BLOCK))
                .thenWaitUntil(() -> helper.assertTrue(diamonds(helper, 2) == 5, "powered pipe should extract"))
                .thenSucceed();
    }

    // The GUI buttons change the side's settings on the server, right click (back) wraps around.
    private static void menuButtons(GameTestHelper helper) {
        pipeRow(helper, Side.INSERT);
        BlockPos pipe = new BlockPos(1, 1, 1);
        PipeBlockEntity be = helper.getBlockEntity(pipe, PipeBlockEntity.class);
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
        helper.getBlockEntity(new BlockPos(1, 1, 2), BarrelBlockEntity.class).setItem(0, new ItemStack(Items.DIAMOND, 64));
        helper.getBlockEntity(new BlockPos(1, 1, 1), PipeBlockEntity.class).upgrades
                .setItem(Direction.SOUTH.ordinal() * PipeBlockEntity.UPGRADES_PER_SIDE, tier);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(diamonds(helper, 2) > 0, "nothing moved"))
                .thenExecute(() -> helper.assertTrue(diamonds(helper, 2) == expected,
                        "first transfer moved " + diamonds(helper, 2) + ", expected " + expected))
                .thenSucceed();
    }

    // Tier 1 uses the upgrade_tier_1 config amount instead of the base one.
    private static void tier1UpgradeRate(GameTestHelper helper) {
        firstTransferMoves(helper, new ItemStack(ModItems.TIER_UPGRADES.get(0).get()), ServerConfig.upgradeTiers.get(0).itemTransferRate().get());
    }

    // Creative upgrade moves everything at once.
    private static void creativeUpgradeRate(GameTestHelper helper) {
        firstTransferMoves(helper, new ItemStack(ModItems.CREATIVE_UPGRADE.get()), 64);
    }

    // Shift right click install: takes one item, swaps tiers, and says why when it refuses.
    private static void installUpgrade(GameTestHelper helper) {
        pipeRow(helper, Side.INSERT);
        PipeBlockEntity be = helper.getBlockEntity(new BlockPos(1, 1, 1), PipeBlockEntity.class);
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
        return helper.getBlockEntity(new BlockPos(x, 1, 2), BarrelBlockEntity.class).countItem(item);
    }

    // Extract whitelist with diamonds: the diamonds move, the dirt next to them stays.
    private static void extractWhitelist(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT);
        helper.getBlockEntity(new BlockPos(1, 1, 2), BarrelBlockEntity.class).setItem(1, new ItemStack(Items.DIRT, 5));
        var filter = helper.getBlockEntity(new BlockPos(1, 1, 1), PipeBlockEntity.class).getFilter(Direction.SOUTH, false);
        filter.toggleWhitelist();
        filter.set(0, new ItemStack(Items.DIAMOND));
        helper.onEachTick(() -> helper.assertTrue(count(helper, 2, Items.DIRT) == 0, "dirt is not whitelisted"));
        helper.succeedWhen(() -> helper.assertTrue(diamonds(helper, 2) == 5, "whitelisted diamonds should move"));
    }

    // Insert blacklist with diamonds on the first target: the diamonds skip it and go to the next one.
    private static void insertBlacklist(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT, Side.INSERT);
        helper.getBlockEntity(new BlockPos(2, 1, 1), PipeBlockEntity.class).getFilter(Direction.SOUTH, true)
                .set(0, new ItemStack(Items.DIAMOND));
        helper.onEachTick(() -> helper.assertTrue(diamonds(helper, 2) == 0, "blacklisted insert should get no diamonds"));
        helper.succeedWhen(() -> helper.assertTrue(diamonds(helper, 3) == 5, "diamonds should reach the next insert"));
    }

    // Clicking a filter slot with an item copies it in (nothing is taken), right click removes it.
    private static void filterSlotClicks(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT);
        PipeBlockEntity be = helper.getBlockEntity(new BlockPos(1, 1, 1), PipeBlockEntity.class);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        PipeMenu menu = new PipeMenu(0, player.getInventory(), be, Direction.SOUTH);
        int firstFilterSlot = menu.slots.size() - PipeMenu.GRID_COLS * PipeMenu.GRID_ROWS;
        var filter = be.getFilter(Direction.SOUTH, false);

        menu.setCarried(new ItemStack(Items.DIAMOND, 3));
        menu.clicked(firstFilterSlot + 5, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(filter.entries().size() == 1 && filter.entries().get(0).stack().is(Items.DIAMOND), "diamond should be added to the filter");
        helper.assertTrue(menu.getCarried().getCount() == 3, "the carried stack should not be used up");

        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(firstFilterSlot, 1, ContainerInput.PICKUP, player);
        helper.assertTrue(filter.entries().isEmpty(), "right click should remove the entry");
        helper.succeed();
    }

    // Whitelist with an amount on the given pipe's filter, 5 diamonds start in the first barrel.
    private static void stockSetup(GameTestHelper helper, boolean insert, int amount) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT);
        var filter = helper.getBlockEntity(new BlockPos(insert ? 2 : 1, 1, 1), PipeBlockEntity.class).getFilter(Direction.SOUTH, insert);
        filter.toggleWhitelist();
        filter.set(0, new ItemStack(Items.DIAMOND));
        filter.setAmount(0, amount);
    }

    // Insert amount 3: the target is filled up to 3 and then stays there.
    private static void insertStock(GameTestHelper helper) {
        stockSetup(helper, true, 3);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(diamonds(helper, 2) == 3, "target should be filled up to 3"))
                .thenExecuteFor(60, () -> helper.assertTrue(diamonds(helper, 2) == 3 && diamonds(helper, 1) == 2, "target should stay at 3"))
                .thenSucceed();
    }

    // Extract amount 2: each transfer moves 2 diamonds (the base rate would move all 5 at once).
    private static void extractBatch(GameTestHelper helper) {
        stockSetup(helper, false, 2);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(diamonds(helper, 2) > 0, "nothing moved"))
                .thenExecute(() -> helper.assertTrue(diamonds(helper, 2) == 2, "first transfer should move 2, moved " + diamonds(helper, 2)))
                .thenWaitUntil(() -> helper.assertTrue(diamonds(helper, 2) == 5, "the rest should follow in later transfers"))
                .thenSucceed();
    }

    // Furthest: the far insert barrel gets the diamonds, the near one stays empty.
    private static void distributionFurthest(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT, Side.INSERT);
        helper.getBlockEntity(new BlockPos(1, 1, 1), PipeBlockEntity.class).setDistribution(Direction.SOUTH, PipeBlockEntity.Distribution.FURTHEST);
        helper.onEachTick(() -> helper.assertTrue(diamonds(helper, 2) == 0, "near insert should get nothing"));
        helper.succeedWhen(() -> helper.assertTrue(diamonds(helper, 3) == 5, "far insert should get the diamonds"));
    }

    // Round-robin with 1 diamond per transfer: the first two transfers go to different barrels.
    private static void distributionRoundRobin(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT, Side.INSERT);
        PipeBlockEntity be = helper.getBlockEntity(new BlockPos(1, 1, 1), PipeBlockEntity.class);
        be.setDistribution(Direction.SOUTH, PipeBlockEntity.Distribution.ROUND_ROBIN);
        var filter = be.getFilter(Direction.SOUTH, false);
        filter.toggleWhitelist();
        filter.set(0, new ItemStack(Items.DIAMOND));
        filter.setAmount(0, 1);
        helper.succeedWhen(() -> helper.assertTrue(diamonds(helper, 2) == 1 && diamonds(helper, 3) == 1, "each insert should get one diamond"));
    }

    // Speed stays between the tier's fastest and 200 ticks, a slower speed spaces transfers out,
    // and going back to the fastest follows the tier.
    private static void speedSetting(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT);
        helper.getBlockEntity(new BlockPos(1, 1, 2), BarrelBlockEntity.class).setItem(0, new ItemStack(Items.DIAMOND, 64));
        PipeBlockEntity be = helper.getBlockEntity(new BlockPos(1, 1, 1), PipeBlockEntity.class);
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
    private static void fluidFilter(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT, Side.INSERT);
        helper.setBlock(new BlockPos(1, 1, 2), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.CAULDRON);
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.CAULDRON);
        helper.getBlockEntity(new BlockPos(1, 1, 1), PipeBlockEntity.class).upgrades
                .setItem(Direction.SOUTH.ordinal() * PipeBlockEntity.UPGRADES_PER_SIDE + 1, new ItemStack(ModItems.FLUID_UPGRADE.get()));
        helper.getBlockEntity(new BlockPos(2, 1, 1), PipeBlockEntity.class).getFilter(Direction.SOUTH, true)
                .set(0, new ItemStack(Items.WATER_BUCKET));
        helper.onEachTick(() -> helper.assertBlockPresent(Blocks.CAULDRON, new BlockPos(2, 1, 2)));
        helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.WATER_CAULDRON, new BlockPos(3, 1, 2)));
    }

    // Two "both" sides on different channels keep moving after their block entities are saved and loaded again,
    // like rejoining a world: first extracts on channel 1 into the second, the second extracts on 2 (nowhere).
    private static void reloadKeepsWorking(GameTestHelper helper) {
        pipeRow(helper, Side.BOTH, Side.BOTH);
        PipeBlockEntity first = helper.getBlockEntity(new BlockPos(1, 1, 1), PipeBlockEntity.class);
        PipeBlockEntity second = helper.getBlockEntity(new BlockPos(2, 1, 1), PipeBlockEntity.class);
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
    private static void timerSurvivesReload(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT);
        helper.getBlockEntity(new BlockPos(1, 1, 2), BarrelBlockEntity.class).setItem(0, new ItemStack(Items.DIAMOND, 64));
        helper.getBlockEntity(new BlockPos(1, 1, 1), PipeBlockEntity.class).changeSpeed(Direction.SOUTH, 1000);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(diamonds(helper, 2) > 0, "first transfer should happen right away"))
                .thenExecute(() -> reload(helper, new BlockPos(1, 1, 1)))
                .thenExecuteFor(100, () -> helper.assertTrue(diamonds(helper, 2) == 8, "reload should not reset the wait"))
                .thenSucceed();
    }

    private static ItemStack tagFilter(String... tags) {
        ItemStack stack = new ItemStack(ModItems.TAG_FILTER.get());
        stack.set(ModDataComponents.TAGS, java.util.Arrays.stream(tags).map(Identifier::parse).toList());
        return stack;
    }

    // Extract whitelist holding a Tag Filter for c:gems/diamond: the diamonds move, the emeralds stay.
    private static void tagFilterItems(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT);
        helper.getBlockEntity(new BlockPos(1, 1, 2), BarrelBlockEntity.class).setItem(1, new ItemStack(Items.EMERALD, 3));
        var filter = helper.getBlockEntity(new BlockPos(1, 1, 1), PipeBlockEntity.class).getFilter(Direction.SOUTH, false);
        filter.toggleWhitelist();
        filter.set(0, tagFilter("c:gems/diamond"));
        helper.onEachTick(() -> helper.assertTrue(count(helper, 2, Items.EMERALD) == 0, "emeralds are not in the tag"));
        helper.succeedWhen(() -> helper.assertTrue(diamonds(helper, 2) == 5, "diamonds should pass the tag filter"));
    }

    // A Tag Filter for the minecraft:water fluid tag in a blacklist blocks water, like a water bucket would.
    private static void tagFilterFluids(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT, Side.INSERT);
        helper.setBlock(new BlockPos(1, 1, 2), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.CAULDRON);
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.CAULDRON);
        helper.getBlockEntity(new BlockPos(1, 1, 1), PipeBlockEntity.class).upgrades
                .setItem(Direction.SOUTH.ordinal() * PipeBlockEntity.UPGRADES_PER_SIDE + 1, new ItemStack(ModItems.FLUID_UPGRADE.get()));
        helper.getBlockEntity(new BlockPos(2, 1, 1), PipeBlockEntity.class).getFilter(Direction.SOUTH, true).set(0, tagFilter("minecraft:water"));
        helper.onEachTick(() -> helper.assertBlockPresent(Blocks.CAULDRON, new BlockPos(2, 1, 2)));
        helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.WATER_CAULDRON, new BlockPos(3, 1, 2)));
    }

    // Copying an extract connection onto an insert one brings the mode, settings and filter along.
    private static void configuratorCopyPaste(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT);
        PipeBlockEntity from = helper.getBlockEntity(new BlockPos(1, 1, 1), PipeBlockEntity.class);
        PipeBlockEntity to = helper.getBlockEntity(new BlockPos(2, 1, 1), PipeBlockEntity.class);
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
    private static void configuratorDisable(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT);
        BlockPos first = new BlockPos(1, 1, 1), second = new BlockPos(2, 1, 1);
        PipeBlockEntity be = helper.getBlockEntity(first, PipeBlockEntity.class);
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
    private static void configuratorDismantle(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT);
        BlockPos pos = new BlockPos(1, 1, 1);
        PipeBlockEntity be = helper.getBlockEntity(pos, PipeBlockEntity.class);
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
        PipeBlock.dismantle(player, helper.getLevel(), helper.absolutePos(pos), helper.getBlockState(pos), helper.getBlockEntity(pos, PipeBlockEntity.class));
        helper.assertItemEntityPresent(ModBlocks.PIPE.get().asItem(), pos, 1);
        helper.succeed();
    }
}
