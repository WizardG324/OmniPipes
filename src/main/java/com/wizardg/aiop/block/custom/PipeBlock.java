package com.wizardg.aiop.block.custom;

import com.mojang.serialization.MapCodec;
import com.wizardg.aiop.block.ModBlockEntities;
import com.wizardg.aiop.block.entity.PipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jspecify.annotations.Nullable;

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

    // Shape only depends on none/arm/arm+plate per side, so 3^6 = 729 shapes shared by every pipe block.
    private static final VoxelShape[] SHAPES = new VoxelShape[729];
    static {
        Map<Direction, VoxelShape> arms = Shapes.rotateAll(Block.boxZ(4, 0, 8));
        Map<Direction, VoxelShape> plates = Shapes.rotateAll(Block.boxZ(10, 0, 3));
        for (int key = 0; key < SHAPES.length; key++) {
            VoxelShape shape = Block.cube(4);
            int k = key;
            for (Direction dir : Direction.values()) {
                int kind = k % 3;
                k /= 3;
                if (kind > 0) shape = Shapes.or(shape, arms.get(dir));
                if (kind > 1) shape = Shapes.or(shape, plates.get(dir));
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
    private Side sideFor(Level level, BlockPos pos, Direction dir, Side current) {
        BlockPos n = pos.relative(dir);
        Block neighbor = level.getBlockState(n).getBlock();
        if (neighbor == this) return Side.PIPE;
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

    // Empty-hand right click on an arm cycles insert -> extract -> both.
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        Vec3 d = hit.getLocation().subtract(Vec3.atCenterOf(pos));
        Direction dir = Direction.getApproximateNearest(d.x, d.y, d.z);
        Side next = switch (state.getValue(SIDES.get(dir))) {
            case INSERT -> Side.EXTRACT;
            case EXTRACT -> Side.BOTH;
            case BOTH -> Side.INSERT;
            default -> null;
        };
        if (next == null) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            level.setBlockAndUpdate(pos, state.setValue(SIDES.get(dir), next));
            player.sendOverlayMessage(Component.literal(dir.getName() + ": " + next.getSerializedName()));
        }
        return InteractionResult.SUCCESS;
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
