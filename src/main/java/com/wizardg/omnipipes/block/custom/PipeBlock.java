package com.wizardg.omnipipes.block.custom;

import com.mojang.serialization.MapCodec;
import com.wizardg.omnipipes.block.ModBlockEntities;
import com.wizardg.omnipipes.block.entity.PipeBlockEntity;
import com.wizardg.omnipipes.item.ModDataComponents;
import com.wizardg.omnipipes.item.ModItems;
import com.wizardg.omnipipes.item.PipeConfiguratorItem;
import net.minecraft.nbt.CompoundTag;
import com.wizardg.omnipipes.screen.PipeMenu;
import com.wizardg.omnipipes.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.EnumMap;
import java.util.Map;

public class PipeBlock extends Block implements EntityBlock {
    public static final MapCodec<PipeBlock> CODEC = simpleCodec(PipeBlock::new);

    public enum Side implements StringRepresentable {
        NONE, PIPE, INSERT, EXTRACT, BOTH;

        public boolean inserts() {
            return this == INSERT || this == BOTH;
        }

        public boolean extracts() {
            return this == EXTRACT || this == BOTH;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }

    public static final Map<Direction, EnumProperty<Side>> SIDES = new EnumMap<>(Direction.class);
    static {
        for (Direction dir : Direction.values()) SIDES.put(dir, EnumProperty.create(dir.getName(), Side.class));
    }

    // Shape only depends on none/arm/port per side, so 3^6 = 729 shapes shared by every pipe block.
    private static final VoxelShape[] SHAPES = new VoxelShape[729];
    static {
        Map<Direction, VoxelShape> arms = Shapes.rotateAll(Block.boxZ(4, 0, 8));
        Map<Direction, VoxelShape> ports = Shapes.rotateAll(Block.boxZ(11, 0, 1));
        for (int key = 0; key < SHAPES.length; key++) {
            VoxelShape shape = Block.cube(4);
            int k = key;
            for (Direction dir : Direction.values()) {
                int kind = k % 3;
                k /= 3;
                if (kind > 0) shape = Shapes.or(shape, arms.get(dir));
                if (kind > 1) shape = Shapes.or(shape, ports.get(dir));
            }
            SHAPES[key] = shape.optimize();
        }
    }

    private static int shapeKey(BlockState state) {
        int key = 0;
        for (int i = 5; i >= 0; i--) {
            Side side = state.getValue(SIDES.get(Direction.from3DDataValue(i)));
            key = key * 3 + (side == Side.NONE ? 0 : side == Side.PIPE ? 1 : 2);
        }
        return key;
    }

    public PipeBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (var prop : SIDES.values()) state = state.setValue(prop, Side.NONE);
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<PipeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        SIDES.values().forEach(builder::add);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[shapeKey(state)];
    }

    // New block connections start as INSERT, existing modes are kept.
    // Pipes only connect to the exact same pipe block, so each color is its own network.
    // A side disabled with the configurator, on this pipe or on the neighboring pipe, never connects.
    private Side sideFor(Level level, BlockPos pos, Direction dir, Side current) {
        if (level.getBlockEntity(pos) instanceof PipeBlockEntity be && be.isDisabled(dir)) return Side.NONE;
        BlockPos n = pos.relative(dir);
        Block neighbor = level.getBlockState(n).getBlock();
        if (neighbor == this)
            return level.getBlockEntity(n) instanceof PipeBlockEntity other && other.isDisabled(dir.getOpposite()) ? Side.NONE : Side.PIPE;
        if (neighbor instanceof PipeBlock) return Side.NONE;
        Direction side = dir.getOpposite();
        boolean handler = level.getCapability(Capabilities.Item.BLOCK, n, side) != null
                || level.getCapability(Capabilities.Fluid.BLOCK, n, side) != null
                || level.getCapability(Capabilities.Energy.BLOCK, n, side) != null;
        if (!handler) return Side.NONE;
        return current == Side.NONE || current == Side.PIPE ? Side.INSERT : current;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = defaultBlockState();
        for (Direction dir : Direction.values())
            state = state.setValue(SIDES.get(dir), sideFor(ctx.getLevel(), ctx.getClickedPos(), dir, Side.NONE));
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction dir, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        var prop = SIDES.get(dir);
        return state.setValue(prop, level instanceof Level l ? sideFor(l, pos, dir, state.getValue(prop)) : Side.NONE);
    }

    public static boolean isPort(Side side) {
        return side.inserts() || side.extracts();
    }

    private static final Side[] PORT_MODES = {Side.INSERT, Side.EXTRACT, Side.BOTH};

    // Steps a block connection through insert -> extract -> both.
    public static void cycleMode(Level level, BlockPos pos, Direction dir, int step) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof PipeBlock)) return;
        Side current = state.getValue(SIDES.get(dir));
        if (!isPort(current)) return;
        Side next = PORT_MODES[Math.floorMod(Arrays.asList(PORT_MODES).indexOf(current) + step, PORT_MODES.length)];
        level.setBlockAndUpdate(pos, state.setValue(SIDES.get(dir), next));
    }

    // Switches a side off or back on and reconnects it (and the neighboring pipe's side) to match. A block connection
    // that comes back gets its old mode again instead of the default insert.
    public void toggleSide(Level level, BlockPos pos, Direction dir, PipeBlockEntity be) {
        var prop = SIDES.get(dir);
        if (be.isDisabled(dir)) {
            Side was = be.enable(dir);
            refreshSide(level, pos, dir);
            BlockState now = level.getBlockState(pos);
            if (was != null && isPort(was) && isPort(now.getValue(prop))) level.setBlockAndUpdate(pos, now.setValue(prop, was));
        } else {
            be.disable(dir, level.getBlockState(pos).getValue(prop));
            refreshSide(level, pos, dir);
        }
        BlockPos n = pos.relative(dir);
        if (level.getBlockState(n).getBlock() instanceof PipeBlock other) other.refreshSide(level, n, dir.getOpposite());
    }

    private void refreshSide(Level level, BlockPos pos, Direction dir) {
        BlockState state = level.getBlockState(pos);
        var prop = SIDES.get(dir);
        level.setBlockAndUpdate(pos, state.setValue(prop, sideFor(level, pos, dir, state.getValue(prop))));
    }

    // Picks the pipe up with its upgrades. Inventory.add fills matching stacks first, then free hotbar slots, then the
    // rest of the inventory, whatever doesn't fit drops where the pipe was.
    public static void dismantle(Player player, Level level, BlockPos pos, BlockState state, PipeBlockEntity be) {
        List<ItemStack> items = new ArrayList<>(List.of(new ItemStack(state.getBlock())));
        for (int i = 0; i < be.upgrades.getContainerSize(); i++)
            if (!be.upgrades.getItem(i).isEmpty()) items.add(be.upgrades.removeItemNoUpdate(i));
        level.removeBlock(pos, false);
        level.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1, 1);
        for (ItemStack stack : items)
            if (!player.getInventory().add(stack)) popResource(level, pos, stack);
    }

    // Pipe Configurator: configuration mode toggles the clicked side or picks the pipe up (sneaking), copy mode copies
    // a connection (sneaking) or pastes onto one. Returns the action bar message, if any.
    private static @Nullable Component configure(ItemStack configurator, Player player, PipeBlock block, Level level, BlockPos pos,
                                       BlockState state, Direction dir, PipeBlockEntity be) {
        Component side = Component.translatable("screen.omni_pipes.side." + dir.getName());
        if (!PipeConfiguratorItem.copyMode(configurator)) {
            if (player.isShiftKeyDown()) {
                dismantle(player, level, pos, state, be);
                return null;
            }
            block.toggleSide(level, pos, dir, be);
            return Component.translatable(be.isDisabled(dir) ? "message.omni_pipes.configurator.disabled" : "message.omni_pipes.configurator.enabled", side);
        }
        if (!isPort(state.getValue(SIDES.get(dir)))) return Component.translatable("message.omni_pipes.configurator.not_connection");
        if (player.isShiftKeyDown()) {
            configurator.set(ModDataComponents.COPIED_SETTINGS, be.copySettings(dir));
            return Component.translatable("message.omni_pipes.configurator.copied", side);
        }
        CompoundTag copied = configurator.get(ModDataComponents.COPIED_SETTINGS);
        if (copied == null) return Component.translatable("message.omni_pipes.configurator.nothing_copied");
        be.pasteSettings(dir, copied);
        return Component.translatable("message.omni_pipes.configurator.pasted", side);
    }

    private static boolean isUpgradeItem(ItemStack stack) {
        return stack.is(ModTags.Items.TIER_UPGRADES) || stack.is(ModTags.Items.TYPE_UPGRADES);
    }

    // The arm that was clicked, the hit point's biggest offset from the center points along it.
    public static Direction clickedSide(BlockPos pos, BlockHitResult hit) {
        Vec3 d = hit.getLocation().subtract(Vec3.atCenterOf(pos));
        return Direction.getApproximateNearest(d.x, d.y, d.z);
    }

    // Shift right-clicking with an upgrade installs it on the clicked block connection.
    @Override
    protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        Direction dir = clickedSide(pos, hitResult);
        if (itemStack.getItem() instanceof PipeConfiguratorItem) {
            if (!player.mayBuild()) return InteractionResult.PASS; // adventure mode can't rewire or pick up pipes
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PipeBlockEntity be) {
                Component message = configure(itemStack, player, this, level, pos, state, dir, be);
                if (message != null) player.sendOverlayMessage(message);
            }
            return InteractionResult.SUCCESS;
        }
        if (!player.isShiftKeyDown() || !isUpgradeItem(itemStack) || !isPort(state.getValue(SIDES.get(dir))))
            return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);

        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PipeBlockEntity be) {
            String refused = be.installUpgrade(dir, itemStack, player);
            if (refused != null) player.sendOverlayMessage(Component.translatable(refused));
        }
        return InteractionResult.SUCCESS;
    }

    // Right-clicking on a block connection opens its settings.
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        Direction dir = clickedSide(pos, hit);
        if (!isPort(state.getValue(SIDES.get(dir)))) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PipeBlockEntity be) {
            Component title = Component.translatable("screen.omni_pipes.pipe", Component.translatable("screen.omni_pipes.side." + dir.getName()));
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new PipeMenu(id, inventory, be, dir), title),
                    buf -> PipeMenu.writeOpenData(buf, be, dir));
        }
        return InteractionResult.SUCCESS;
    }

    // Redstone signal changes arrive here, so the pipe can re-check which sides may run.
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PipeBlockEntity be) be.updateCanRun(state);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || type != ModBlockEntities.PIPE_BLOCK_ENTITY.get() ? null
                : (BlockEntityTicker<T>) (BlockEntityTicker<PipeBlockEntity>) PipeBlockEntity::serverTick;
    }
}
