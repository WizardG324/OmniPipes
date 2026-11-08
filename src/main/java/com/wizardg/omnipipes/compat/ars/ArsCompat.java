package com.wizardg.omnipipes.compat.ars;

import com.hollingsworth.arsnouveau.api.source.ISourceCap;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.wizardg.omnipipes.block.entity.PipeBlockEntity;
import com.wizardg.omnipipes.block.entity.PipeFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

// Ars Nouveau's source, moved by the Source Upgrade. Only used when Ars Nouveau is loaded.
public final class ArsCompat {
    private ArsCompat() {}

    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("ars_nouveau", "block/mana_still"); // what fills a source jar
    private static final Object SOURCE = new Object(); // filter lookup key, source has no types

    public static boolean present(Level level, BlockPos pos, Direction side) {
        return level.getCapability(CapabilityRegistry.SOURCE_CAPABILITY, pos, side) != null;
    }

    public static boolean hasSource(PipeBlockEntity pipe, Direction dir, PipeFilter filter) {
        ISourceCap source = pipe.neighbor(CapabilityRegistry.SOURCE_CAPABILITY, dir);
        return source != null && source.getSource() > 0 && allows(filter);
    }

    public static int pushSource(PipeBlockEntity pipe, Direction dir, List<PipeBlockEntity.Target> targets, int amount,
                                 PipeFilter extract, Map<PipeFilter.Entry, Integer> batches) {
        return PipeBlockEntity.push(pipe, dir, CapabilityRegistry.SOURCE_CAPABILITY, targets, amount,
                (from, to, n, t) -> moveSource(from, to, extract, t.filter(), n, batches));
    }

    // Like energy (true means simulate), plus the filter rules fluids follow.
    private static int moveSource(ISourceCap from, @Nullable ISourceCap to, PipeFilter extract, PipeFilter insert, int amount,
                                  Map<PipeFilter.Entry, Integer> batches) {
        if (to == null || !from.canExtract() || !to.canReceive() || !allows(extract) || !allows(insert)) return 0;
        PipeFilter.Entry batchEntry = amount(extract) > 0 ? match(extract) : null;
        if (batchEntry != null) amount = Math.min(amount, amount(extract) - batches.getOrDefault(batchEntry, 0));
        if (amount(insert) > 0) amount = Math.min(amount, amount(insert) - to.getSource());
        if (amount <= 0) return 0;
        int fits = to.receiveSource(from.extractSource(amount, true), true);
        if (fits <= 0) return 0;
        int taken = from.extractSource(fits, false);
        int done = to.receiveSource(taken, false);
        if (done < taken) from.receiveSource(taken - done, false); // put back what the target refused after all
        if (batchEntry != null) batches.merge(batchEntry, done, Integer::sum);
        return done;
    }

    public static boolean isSourceJar(ItemStack stack) {
        return stack.is(BlockRegistry.SOURCE_JAR.get().asItem()) || stack.is(BlockRegistry.CREATIVE_SOURCE_JAR.get().asItem());
    }

    public static Object key() {
        return SOURCE;
    }

    private static @Nullable PipeFilter.Entry match(PipeFilter filter) {
        return filter.match(SOURCE, sample -> true);
    }

    private static boolean allows(PipeFilter filter) {
        return (match(filter) != null) == filter.isWhitelist();
    }

    private static int amount(PipeFilter filter) {
        PipeFilter.Entry entry = filter.isWhitelist() ? match(filter) : null;
        return entry == null ? 0 : entry.amount();
    }
}
