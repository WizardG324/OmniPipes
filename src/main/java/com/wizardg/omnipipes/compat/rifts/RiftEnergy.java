package com.wizardg.omnipipes.compat.rifts;

import com.benbenlaw.rifts.block.capability.RiftEnergyHandler;
import com.benbenlaw.rifts.block.capability.RiftsCapabilities;
import com.wizardg.omnipipes.block.entity.PipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.List;

// BBL Rifts' rift energy, moved by the Rift Upgrade. Only used when Rifts is loaded.
public final class RiftEnergy {
    private RiftEnergy() {}

    public static boolean present(Level level, BlockPos pos, Direction side) {
        return level.getCapability(RiftsCapabilities.RIFT_ENERGY, pos, side) != null;
    }

    public static int push(Level level, BlockPos src, Direction side, List<PipeBlockEntity.Target> targets, int amount) {
        return PipeBlockEntity.push(level, RiftsCapabilities.RIFT_ENERGY, src, side, targets, amount, (from, to, n, t) -> move(from, to, n));
    }

    // Same as EnergyHandlerUtil.move: simulate the extract, insert that, then extract what went in.
    private static int move(RiftEnergyHandler from, @Nullable RiftEnergyHandler to, int amount) {
        if (to == null || amount <= 0 || !from.canExtract() || !to.canInsert()) return 0;
        try (Transaction tx = Transaction.openRoot()) {
            int extractable;
            try (Transaction simulate = Transaction.open(tx)) {
                extractable = from.extract(amount, simulate);
            }
            if (extractable <= 0) return 0;
            int inserted = to.insert(extractable, tx);
            if (inserted != from.extract(inserted, tx)) return 0;
            tx.commit();
            return inserted;
        }
    }
}
