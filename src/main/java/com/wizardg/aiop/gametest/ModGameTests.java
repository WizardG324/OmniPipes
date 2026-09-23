package com.wizardg.aiop.gametest;

import com.wizardg.aiop.AIOPAllinOnePipe;
import com.wizardg.aiop.block.ModBlocks;
import com.wizardg.aiop.block.custom.PipeBlock;
import com.wizardg.aiop.block.custom.PipeBlock.Side;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

public class ModGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, AIOPAllinOnePipe.MODID);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> EXTRACT_TO_INSERT =
            TEST_FUNCTIONS.register("extract_to_insert", () -> ModGameTests::extractToInsert);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BOTH_SKIPS_BOTH =
            TEST_FUNCTIONS.register("both_skips_both", () -> ModGameTests::bothSkipsBoth);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> EXTRACT_RECHECK_DELAY =
            TEST_FUNCTIONS.register("extract_recheck_delay", () -> ModGameTests::extractRecheckDelay);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PIPE_SHAPE =
            TEST_FUNCTIONS.register("pipe_shape", () -> ModGameTests::pipeShape);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SAME_TYPE_CONNECTS =
            TEST_FUNCTIONS.register("same_type_connects", () -> ModGameTests::sameTypeConnects);

    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("default"));
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 200, 1, true, Rotation.NONE, false, 1, 1, false, 8);

        event.registerTest(id("extract_to_insert"), new FunctionGameTestInstance(EXTRACT_TO_INSERT.getKey(), testData));
        event.registerTest(id("both_skips_both"), new FunctionGameTestInstance(BOTH_SKIPS_BOTH.getKey(), testData));
        event.registerTest(id("same_type_connects"), new FunctionGameTestInstance(SAME_TYPE_CONNECTS.getKey(), testData));
        event.registerTest(id("pipe_shape"), new FunctionGameTestInstance(PIPE_SHAPE.getKey(), testData));
        event.registerTest(id("extract_recheck_delay"), new FunctionGameTestInstance(EXTRACT_RECHECK_DELAY.getKey(), testData));
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(AIOPAllinOnePipe.MODID, name);
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

    // Two "both" barrels used to ping-pong items forever.
    private static void bothSkipsBoth(GameTestHelper helper) {
        pipeRow(helper, Side.BOTH, Side.BOTH, Side.INSERT);
        helper.onEachTick(() -> helper.assertTrue(diamonds(helper, 2) == 0, "both should never feed another both"));
        helper.succeedWhen(() -> helper.assertTrue(diamonds(helper, 3) == 5, "diamonds should reach the insert barrel"));
    }

    // Source starts empty, so the first extract fails and the pipe should wait extractRecheckDelay (50) ticks.
    private static void extractRecheckDelay(GameTestHelper helper) {
        pipeRow(helper, Side.EXTRACT, Side.INSERT);
        BarrelBlockEntity source = helper.getBlockEntity(new BlockPos(1, 1, 2), BarrelBlockEntity.class);
        source.clearContent();
        helper.startSequence()
                .thenIdle(11)
                .thenExecute(() -> source.setItem(0, new ItemStack(Items.DIAMOND, 5)))
                .thenExecuteFor(20, () -> helper.assertTrue(diamonds(helper, 1) == 5, "pipe should still be waiting"))
                .thenWaitUntil(() -> helper.assertTrue(diamonds(helper, 2) == 5, "pipe should resume after the delay"))
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
        helper.assertTrue(box.minX == 3 / 16.0 && box.maxX == 13 / 16.0, "north plate should be 10px wide");
        helper.assertTrue(box.minY == 3 / 16.0 && box.maxY == 1, "up arm should reach the top, plate sets the bottom");
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
}
