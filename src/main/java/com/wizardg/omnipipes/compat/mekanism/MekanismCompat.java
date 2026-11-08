package com.wizardg.omnipipes.compat.mekanism;

import com.mojang.datafixers.util.Function3;
import com.wizardg.omnipipes.block.entity.PipeBlockEntity;
import com.wizardg.omnipipes.block.entity.PipeFilter;
import mekanism.api.Action;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.heat.HeatAPI;
import mekanism.api.heat.IHeatHandler;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.util.ChemicalUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

// Mekanism's chemicals and heat, moved by the Chemical and Heat Upgrades. Only used when Mekanism is loaded.
public final class MekanismCompat {
    private MekanismCompat() {}

    public static boolean present(Level level, BlockPos pos, Direction side) {
        return level.getCapability(Capabilities.CHEMICAL.block(), pos, side) != null
                || level.getCapability(Capabilities.HEAT, pos, side) != null;
    }

    public static boolean hasChemical(PipeBlockEntity pipe, Direction dir, PipeFilter filter) {
        IChemicalHandler handler = pipe.neighbor(Capabilities.CHEMICAL.block(), dir);
        if (handler == null) return false;
        for (int i = 0; i < handler.getChemicalTanks(); i++) {
            ChemicalStack held = handler.getChemicalInTank(i);
            if (!held.isEmpty() && allows(filter, held)) return true;
        }
        return false;
    }

    public static int pushChemicals(PipeBlockEntity pipe, Direction dir, List<PipeBlockEntity.Target> targets, int amount,
                                    PipeFilter extract, Map<PipeFilter.Entry, Integer> batches) {
        return PipeBlockEntity.push(pipe, dir, Capabilities.CHEMICAL.block(), targets, amount,
                (from, to, n, t) -> moveChemicals(from, to, extract, t.filter(), n, batches));
    }

    // Same rules as fluids in PipeBlockEntity.moveFluids, one tank at a time.
    private static int moveChemicals(IChemicalHandler from, @Nullable IChemicalHandler to, PipeFilter extract, PipeFilter insert,
                                     int amount, Map<PipeFilter.Entry, Integer> batches) {
        if (to == null) return 0;
        int moved = 0;
        Map<PipeFilter.Entry, Integer> stocked = new HashMap<>();
        for (int tank = 0; tank < from.getChemicalTanks() && moved < amount; tank++) {
            ChemicalStack chemical = from.getChemicalInTank(tank);
            if (chemical.isEmpty() || !allows(extract, chemical) || !allows(insert, chemical)) continue;
            long limit = amount - moved;
            int batch = amount(extract, chemical);
            PipeFilter.Entry batchEntry = batch > 0 ? match(extract, chemical) : null;
            if (batchEntry != null) limit = Math.min(limit, batch - batches.getOrDefault(batchEntry, 0));
            int stock = amount(insert, chemical);
            PipeFilter.Entry stockEntry = stock > 0 ? match(insert, chemical) : null;
            if (stockEntry != null) limit = Math.min(limit, stock - stocked.computeIfAbsent(stockEntry, e -> count(to, e, insert)));
            if (limit <= 0) continue;
            ChemicalStack offered = from.extractChemical(tank, limit, Action.SIMULATE);
            if (offered.isEmpty()) continue;
            long fits = offered.getAmount() - to.insertChemical(offered, Action.SIMULATE).getAmount();
            if (fits <= 0) continue;
            ChemicalStack taken = from.extractChemical(tank, fits, Action.EXECUTE);
            ChemicalStack left = to.insertChemical(taken, Action.EXECUTE);
            if (!left.isEmpty()) from.insertChemical(tank, left, Action.EXECUTE); // put back what the target refused after all
            int done = (int) (taken.getAmount() - left.getAmount());
            if (batchEntry != null) batches.merge(batchEntry, done, Integer::sum);
            if (stockEntry != null) stocked.merge(stockEntry, done, Integer::sum);
            moved += done;
        }
        return moved;
    }

    private static int count(IChemicalHandler handler, PipeFilter.Entry entry, PipeFilter filter) {
        long total = 0;
        for (int i = 0; i < handler.getChemicalTanks(); i++) {
            ChemicalStack held = handler.getChemicalInTank(i);
            if (!held.isEmpty() && match(filter, held) == entry) total += held.getAmount();
        }
        return (int) Math.min(total, Integer.MAX_VALUE);
    }

    // Instant: every tick, each colder target gets heat until it sits at the source's temperature.
    public static void moveHeat(PipeBlockEntity pipe, Direction dir, List<PipeBlockEntity.Target> targets) {
        IHeatHandler from = pipe.neighbor(Capabilities.HEAT, dir);
        if (from == null) return;
        Level level = pipe.getLevel();
        if (level.dimension() != pendingLevel || level.getGameTime() != pendingTick) {
            pending.clear();
            pendingLevel = level.dimension();
            pendingTick = level.getGameTime();
        }
        BlockPos src = pipe.getBlockPos().relative(dir);
        double capacity = from.getTotalHeatCapacity();
        if (capacity <= 0) return;
        for (PipeBlockEntity.Target t : targets) {
            if (t.pos().equals(src)) continue;
            IHeatHandler to = t.pipe().neighbor(Capabilities.HEAT, t.dir());
            if (to == null) continue;
            double toCapacity = to.getTotalHeatCapacity();
            if (toCapacity <= 0) continue;
            double difference = temperature(src, from, capacity) - temperature(t.pos(), to, toCapacity);
            if (difference <= HeatAPI.EPSILON) continue;
            double heat = difference * capacity * toCapacity / (capacity + toCapacity);
            give(src, from, -heat);
            give(t.pos(), to, heat);
        }
    }

    // Heat handed out this tick by block. Mekanism only applies heat at the end of the tick, so without this a source
    // feeding several targets, or several sources feeding one target, would all see the old temperatures and overshoot.
    private static final Map<BlockPos, Double> pending = new HashMap<>();
    private static @Nullable ResourceKey<Level> pendingLevel;
    private static long pendingTick = -1;

    private static double temperature(BlockPos pos, IHeatHandler handler, double capacity) {
        return handler.getTotalTemperature() + pending.getOrDefault(pos, 0.0) / capacity;
    }

    private static void give(BlockPos pos, IHeatHandler handler, double heat) {
        handler.handleHeat(heat);
        pending.merge(pos, heat, Double::sum);
    }

    public static void addHeld(ItemStack stack, BiConsumer<Object, Object> add) {
        ChemicalStack held = held(stack);
        if (!held.isEmpty()) add.accept(held.getChemical(), held);
    }

    public static boolean holdsChemical(ItemStack stack) {
        return !held(stack).isEmpty();
    }

    public static ItemStack chemicalEntry(Object value) {
        Holder<Chemical> chemical = value instanceof ChemicalStack stack ? stack.getChemicalHolder()
                : value instanceof Chemical c ? c.getAsHolder() : null;
        return chemical == null ? ItemStack.EMPTY
                : PipeFilter.contentsOf(ChemicalUtil.getFilledVariant(new ItemStack(MekanismBlocks.BASIC_CHEMICAL_TANK), chemical));
    }

    public static <T> @Nullable T heldLook(ItemStack stack, Function3<Component, ResourceLocation, Integer, T> look) {
        ChemicalStack held = held(stack);
        return held.isEmpty() ? null : look.apply(held.getTextComponent(), held.getChemical().getIcon(), held.getChemicalTint());
    }

    private static ChemicalStack held(ItemStack stack) {
        IChemicalHandler handler = stack.getCapability(Capabilities.CHEMICAL.item());
        if (handler == null) return ChemicalStack.EMPTY;
        for (int i = 0; i < handler.getChemicalTanks(); i++)
            if (!handler.getChemicalInTank(i).isEmpty()) return handler.getChemicalInTank(i);
        return ChemicalStack.EMPTY;
    }

    private static @Nullable PipeFilter.Entry match(PipeFilter filter, ChemicalStack chemical) {
        return filter.match(chemical.getChemical(), sample -> sample instanceof ChemicalStack c && ChemicalStack.isSameChemical(c, chemical));
    }

    private static boolean allows(PipeFilter filter, ChemicalStack chemical) {
        return (match(filter, chemical) != null) == filter.isWhitelist();
    }

    private static int amount(PipeFilter filter, ChemicalStack chemical) {
        PipeFilter.Entry entry = filter.isWhitelist() ? match(filter, chemical) : null;
        return entry == null ? 0 : entry.amount();
    }
}
