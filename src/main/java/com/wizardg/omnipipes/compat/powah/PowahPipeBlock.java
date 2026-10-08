package com.wizardg.omnipipes.compat.powah;

import com.wizardg.omnipipes.block.custom.PipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import owmii.powah.api.energy.IEnergyConnector;
import owmii.powah.block.cable.CableBlock;

// Powah's energizing rods stay on blocks marked as energy connectors, so pipes use this when Powah is loaded.
public class PowahPipeBlock extends PipeBlock implements IEnergyConnector {
    public PowahPipeBlock(Properties properties) {
        super(properties);
    }

    // Powah's cables offer energy on every side, pipes don't connect to them.
    @Override
    protected Side sideFor(Level level, BlockPos pos, Direction dir, Side current) {
        if (level.getBlockState(pos.relative(dir)).getBlock() instanceof CableBlock) return Side.NONE;
        return super.sideFor(level, pos, dir, current);
    }
}
