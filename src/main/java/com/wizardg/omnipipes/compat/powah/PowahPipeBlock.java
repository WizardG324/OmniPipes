package com.wizardg.omnipipes.compat.powah;

import com.wizardg.omnipipes.block.custom.PipeBlock;
import owmii.powah.api.energy.IEnergyConnector;

// Powah's energizing rods stay on blocks marked as energy connectors, so pipes use this when Powah is loaded.
public class PowahPipeBlock extends PipeBlock implements IEnergyConnector {
    public PowahPipeBlock(Properties properties) {
        super(properties);
    }
}
