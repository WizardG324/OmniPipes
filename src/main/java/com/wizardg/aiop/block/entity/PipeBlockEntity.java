package com.wizardg.aiop.block.entity;

import com.wizardg.aiop.block.ModBlockEntities;
import com.wizardg.aiop.block.custom.PipeBlock;
import com.wizardg.aiop.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.wizardg.aiop.block.custom.PipeBlock.SIDES;

public class PipeBlockEntity extends BlockEntity {
    private long nextExtract; // game time, not saved since a reload just retries early

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PIPE_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PipeBlockEntity be) {
        long time = level.getGameTime();
        if (time < be.nextExtract) return;
        List<Target> targets = null;
        int moved = 0;
        for (Direction dir : Direction.values()) {
            PipeBlock.Side mode = state.getValue(SIDES.get(dir));
            if (!mode.extracts()) continue;
            if (targets == null) targets = findTargets(level, pos);
            BlockPos src = pos.relative(dir);
            Direction side = dir.getOpposite();
            // "both" sides act as buffers, they only feed plain inserts so two buffers can't ping-pong
            List<Target> dests = mode == PipeBlock.Side.BOTH ? targets.stream().filter(t -> !t.buffer).toList() : targets;
            moved += push(level, Capabilities.Item.BLOCK, src, side, dests, ServerConfig.base.itemTransferRate().get(), (a, b, n) -> ResourceHandlerUtil.move(a, b, r -> true, n, null));
            moved += push(level, Capabilities.Fluid.BLOCK, src, side, dests, ServerConfig.base.fluidTransferRate().get(), (a, b, n) -> ResourceHandlerUtil.move(a, b, r -> true, n, null));
            moved += push(level, Capabilities.Energy.BLOCK, src, side, dests, ServerConfig.base.energyTransferRate().get(), (a, b, n) -> EnergyHandlerUtil.move(a, b, n, null));
        }
        if (targets != null)
            be.nextExtract = time + (moved > 0 ? ServerConfig.base.transferRate().get() : ServerConfig.extractRecheckDelay.get());
    }

    interface Mover<H> {
        int move(H from, @Nullable H to, int amount);
    }

    private static <H> int push(Level level, BlockCapability<H, @Nullable Direction> cap, BlockPos src, Direction side,
                                List<Target> targets, int amount, Mover<H> mover) {
        H from = level.getCapability(cap, src, side);
        if (from == null) return 0;
        int left = amount;
        for (Target t : targets) {
            if (left <= 0) break;
            if (!t.pos.equals(src)) left -= mover.move(from, level.getCapability(cap, t.pos, t.side), left);
        }
        return amount - left;
    }

    record Target(BlockPos pos, Direction side, boolean buffer) {}

    // Cache the network per pipe if big networks lag
    private static List<Target> findTargets(Level level, BlockPos start) {
        List<Target> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>(List.of(start));
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(seen);
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            BlockState s = level.getBlockState(p);
            if (!(s.getBlock() instanceof PipeBlock)) continue;
            for (Direction dir : Direction.values()) {
                PipeBlock.Side side = s.getValue(SIDES.get(dir));
                BlockPos n = p.relative(dir);
                if (!level.isLoaded(n)) continue;
                if (side == PipeBlock.Side.PIPE) {
                    if (seen.add(n)) queue.add(n);
                } else if (side.inserts()) {
                    out.add(new Target(n, dir.getOpposite(), side == PipeBlock.Side.BOTH));
                }
            }
        }
        return out;
    }
}
