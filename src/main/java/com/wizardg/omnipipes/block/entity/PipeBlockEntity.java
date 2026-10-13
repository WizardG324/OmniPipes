package com.wizardg.omnipipes.block.entity;

import com.wizardg.omnipipes.OmniPipes;
import com.wizardg.omnipipes.block.ModBlockEntities;
import com.wizardg.omnipipes.block.custom.PipeBlock;
import com.wizardg.omnipipes.command.PipesCommand;
import com.wizardg.omnipipes.compat.rifts.RiftEnergy;
import com.wizardg.omnipipes.config.ServerConfig;
import com.wizardg.omnipipes.item.ModItems;
import com.wizardg.omnipipes.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.util.Util;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.resource.RegisteredResource;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.wizardg.omnipipes.block.custom.PipeBlock.SIDES;

public class PipeBlockEntity extends BlockEntity {
    private final long[] nextExtract = new long[6]; // game time per side
    // Sides switched off with the configurator (they never connect), holding what the side was so it can come back.
    private final PipeBlock.Side[] disabledFrom = new PipeBlock.Side[6];
    private final int[] sleep = new int[6];
    private final long[] lastExtract = new long[6];
    private final boolean[] blocked = new boolean[6]; // last failed with something to move, so the targets were full
    private int @Nullable [] loadedWait; // ticks each side still had to wait when saved, applied on the first tick
    private final RedstoneMode[] redstone = new RedstoneMode[6];
    private final Distribution[] distribution = new Distribution[6];
    private final int[] roundRobin = new int[6]; // next start target per side, not saved
    private final int[] speed = new int[6]; // ticks between transfers, 0 = as fast as the tier allows
    public static final int SLOWEST_SPEED = 200;
    private final int[] insertChannel = new int[6]; // dye color ids, 0 = white
    private final int[] extractChannel = new int[6];
    private final boolean[] canThisSideRun = new boolean[6]; // strict check if a side can run, only checks for sides that extract
    private @Nullable BlockState canRunState; // state canThisSideRun was worked out for
    private int activeSides = -1;
    private long lastTickTime = -1; // prevent tick acceleration, since it's just lag waiting to happen
    private final PipeFilter[] insertFilters = new PipeFilter[6];
    private final PipeFilter[] extractFilters = new PipeFilter[6];

    // Per side: slot 0 is the tier upgrade, 1-3 are type upgrades. Down uses 0-3, up 4-7, and so on.
    public static final int UPGRADES_PER_SIDE = 4;
    public final SimpleContainer upgrades = new SimpleContainer(6 * UPGRADES_PER_SIDE) {
        @Override
        public void setChanged() {
            super.setChanged();
            PipeBlockEntity.this.setChanged();
        }
    };

    public record Rates(int ticks, int items, int fluid, int energy, int rift) {}
    private static final Rates CREATIVE = new Rates(1, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

    /** Continue enums after the last one, to not mess up pre-existing worlds */
    public enum RedstoneMode {
        DISABLED, LOW_SIGNAL, HIGH_SIGNAL, OFF;

        public boolean allows(int signal) {
            return switch (this) {
                case DISABLED -> true;
                case LOW_SIGNAL -> signal <= 7;
                case HIGH_SIGNAL -> signal >= 8;
                case OFF -> false;
            };
        }
    }

    // Order an extracting side hands items to its insert sides in.
    public enum Distribution {
        CLOSEST, FURTHEST, ROUND_ROBIN, RANDOM
    }

    // Connected pipes. A change marks it invalid, then the next pipe to look walks the network into a fresh one.
    private static final class Network {
        boolean valid = true;
        final Set<PipeBlockEntity> members = Collections.newSetFromMap(new IdentityHashMap<>());
        long wokenAt = -1;

        // A target made room, so only sides blocked by full targets wake. Empty sources keep sleeping.
        void wake(long time) {
            if (wokenAt == time) return;
            wokenAt = time;
            for (PipeBlockEntity pipe : members) pipe.wake(true);
        }
    }

    private @Nullable Network network;
    private final Map<BlockCapability<?, @Nullable Direction>, BlockCapabilityCache<?, @Nullable Direction>[]> capabilityCaches = new HashMap<>();
    // Per side, what the block there had no room for. Cleared when it changes, or every REFUSED_TICKS for blocks that don't say.
    @SuppressWarnings("unchecked")
    private final Set<Object>[] refused = new Set[6];
    private final long[] refusedSince = new long[6];
    private @Nullable Network cachedFor;
    private List<Target> cachedTargets = List.of();

    private List<Target> targets() {
        if (network == null || !network.valid || cachedFor != network) {
            cachedTargets = findTargets();
            cachedFor = network;
        }
        return cachedTargets;
    }

    private void invalidateNetwork() {
        if (network != null) network.valid = false;
    }

    // Connections may have changed, so neighboring networks could merge or split.
    private void invalidateAround() {
        invalidateNetwork();
        if (level == null) return;
        for (Direction dir : Direction.values()) {
            BlockPos n = worldPosition.relative(dir);
            if (level.isLoaded(n) && level.getBlockEntity(n) instanceof PipeBlockEntity pipe) pipe.invalidateNetwork();
        }
    }

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PIPE_BLOCK_ENTITY.get(), pos, state);
        Arrays.fill(redstone, RedstoneMode.DISABLED);
        Arrays.fill(distribution, Distribution.CLOSEST);
        for (int i = 0; i < 6; i++) {
            insertFilters[i] = new PipeFilter(this::setChanged);
            extractFilters[i] = new PipeFilter(this::setChanged);
        }
    }

    public PipeFilter getFilter(Direction dir, boolean insert) {
        return (insert ? insertFilters : extractFilters)[dir.ordinal()];
    }

    public RedstoneMode getRedstone(Direction dir) {
        return redstone[dir.ordinal()];
    }

    public void setRedstone(Direction dir, RedstoneMode mode) {
        redstone[dir.ordinal()] = mode;
        setChanged();
        if (level != null) updateCanRun(getBlockState());
    }

    public void cycleRedstone(Direction dir, int step) {
        setRedstone(dir, RedstoneMode.values()[Math.floorMod(getRedstone(dir).ordinal() + step, RedstoneMode.values().length)]);
    }

    public Distribution getDistribution(Direction dir) {
        return distribution[dir.ordinal()];
    }

    public void setDistribution(Direction dir, Distribution mode) {
        distribution[dir.ordinal()] = mode;
        setChanged();
    }

    public void cycleDistribution(Direction dir, int step) {
        setDistribution(dir, Distribution.values()[Math.floorMod(getDistribution(dir).ordinal() + step, Distribution.values().length)]);
    }

    // Targets come in network order (closest first), this reorders them for the side's distribution mode.
    private List<Target> distribute(Direction dir, List<Target> targets) {
        if (targets.size() < 2) return targets;
        return switch (getDistribution(dir)) {
            case CLOSEST -> targets;
            case FURTHEST -> targets.reversed();
            case ROUND_ROBIN -> {
                int start = roundRobin[dir.ordinal()]++ % targets.size();
                List<Target> rotated = new ArrayList<>(targets.subList(start, targets.size()));
                rotated.addAll(targets.subList(0, start));
                yield rotated;
            }
            case RANDOM -> {
                List<Target> shuffled = new ArrayList<>(targets);
                Util.shuffle(shuffled, level.getRandom());
                yield shuffled;
            }
        };
    }

    // Fastest the side's tier allows, in ticks between transfers.
    public int fastestSpeed(Direction dir) {
        return rates(dir).ticks;
    }

    // Ticks between transfers actually used, the setting clamped between the tier's fastest and SLOWEST_SPEED.
    public int getSpeed(Direction dir) {
        int fastest = fastestSpeed(dir);
        int set = speed[dir.ordinal()];
        return set == 0 ? fastest : Math.max(fastest, Math.min(set, SLOWEST_SPEED));
    }

    // Changes the speed by ticks (negative = faster). Reaching the tier's fastest stores 0, so better tiers speed it up.
    public void changeSpeed(Direction dir, int ticks) {
        int fastest = fastestSpeed(dir);
        int next = Math.clamp(getSpeed(dir) + ticks, fastest, Math.max(fastest, SLOWEST_SPEED));
        speed[dir.ordinal()] = next == fastest ? 0 : next;
        setChanged();
    }

    public boolean isDisabled(Direction dir) {
        return disabledFrom[dir.ordinal()] != null;
    }

    public void disable(Direction dir, PipeBlock.Side was) {
        disabledFrom[dir.ordinal()] = was;
        setChanged();
    }

    // Returns what the side was before it got disabled.
    public PipeBlock.@Nullable Side enable(Direction dir) {
        PipeBlock.Side was = disabledFrom[dir.ordinal()];
        disabledFrom[dir.ordinal()] = null;
        setChanged();
        return was;
    }

    // A connection's settings and both its filters, for the configurator to copy onto another connection.
    public CompoundTag copySettings(Direction dir) {
        int i = dir.ordinal();
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        output.putString("mode", getBlockState().getValue(SIDES.get(dir)).getSerializedName());
        output.putInt("redstone", redstone[i].ordinal());
        output.putInt("distribution", distribution[i].ordinal());
        output.putInt("speed", speed[i]);
        output.putInt("insert_channel", insertChannel[i]);
        output.putInt("extract_channel", extractChannel[i]);
        insertFilters[i].save(output.child("insert_filter"));
        extractFilters[i].save(output.child("extract_filter"));
        return output.buildResult();
    }

    // Only onto block connections, the mode is changed too.
    public void pasteSettings(Direction dir, CompoundTag settings) {
        int i = dir.ordinal();
        ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), settings);
        redstone[i] = RedstoneMode.values()[Math.clamp(input.getIntOr("redstone", 0), 0, RedstoneMode.values().length - 1)];
        distribution[i] = Distribution.values()[Math.clamp(input.getIntOr("distribution", 0), 0, Distribution.values().length - 1)];
        speed[i] = Math.clamp(input.getIntOr("speed", 0), 0, SLOWEST_SPEED);
        insertChannel[i] = Math.clamp(input.getIntOr("insert_channel", 0), 0, 15);
        extractChannel[i] = Math.clamp(input.getIntOr("extract_channel", 0), 0, 15);
        invalidateNetwork();
        input.child("insert_filter").ifPresent(insertFilters[i]::load);
        input.child("extract_filter").ifPresent(extractFilters[i]::load);
        setChanged();
        String mode = input.getStringOr("mode", "");
        for (PipeBlock.Side side : PipeBlock.Side.values())
            if (PipeBlock.isPort(side) && side.getSerializedName().equals(mode))
                level.setBlockAndUpdate(worldPosition, getBlockState().setValue(SIDES.get(dir), side));
        updateCanRun(getBlockState());
    }

    public int getInsertChannel(Direction dir) {
        return insertChannel[dir.ordinal()];
    }

    public int getExtractChannel(Direction dir) {
        return extractChannel[dir.ordinal()];
    }

    public void setInsertChannel(Direction dir, int channel) {
        insertChannel[dir.ordinal()] = channel;
        setChanged();
        invalidateNetwork();
    }

    public void setExtractChannel(Direction dir, int channel) {
        extractChannel[dir.ordinal()] = channel;
        setChanged();
        invalidateNetwork();
    }

    // Called when the mode, redstone setting or incoming signal changes. Not saved, onLoad works it out again.
    public void updateCanRun(BlockState state) {
        int signal = level.getBestNeighborSignal(worldPosition);
        int active = 0;
        for (Direction dir : Direction.values()) {
            boolean allows = getRedstone(dir).allows(signal);
            canThisSideRun[dir.ordinal()] = state.getValue(SIDES.get(dir)).extracts() && allows;
            if (allows) active |= 1 << dir.ordinal();
        }
        canRunState = state;
        if (active != activeSides) invalidateNetwork(); // redstone decides which insert sides are targets
        activeSides = active;
    }

    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        invalidateAround();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        invalidateNetwork();
        if (network != null) network.members.remove(this);
        PipesCommand.LOADED.remove(this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        invalidateAround();
        if (!level.isClientSide()) PipesCommand.LOADED.add(this);
        if (!level.isClientSide()) updateCanRun(getBlockState());
    }

    public boolean getCanThisSideRun(Direction dir) {
        return canThisSideRun[dir.ordinal()];
    }

    private ItemStack upgrade(Direction dir, int slot) {
        return upgrades.getItem(dir.ordinal() * UPGRADES_PER_SIDE + slot);
    }

    // Puts one upgrade from the stack into a side's slots. A different tier swaps with the installed one (the old one
    // goes back to the player), the same tier or a type that is already installed is refused.
    // Returns null on success, otherwise the translation key of the reason.
    public @Nullable String installUpgrade(Direction dir, ItemStack stack, Player player) {
        int first = dir.ordinal() * UPGRADES_PER_SIDE;
        int slot = -1;
        if (stack.is(ModTags.Items.TIER_UPGRADES)) {
            ItemStack tier = upgrades.getItem(first);
            if (ItemStack.isSameItem(tier, stack)) return "message.omni_pipes.upgrade.same_tier";
            slot = first;
        } else if (stack.is(ModTags.Items.TYPE_UPGRADES)) {
            if (hasTypeUpgrade(upgrades, first, stack, -1)) return "message.omni_pipes.upgrade.same_type";
            for (int i = UPGRADES_PER_SIDE - 1; i >= 1; i--)
                if (upgrades.getItem(first + i).isEmpty()) slot = first + i; // ends on the first free slot
            if (slot < 0) return "message.omni_pipes.upgrade.type_full";
        }
        ItemStack old = upgrades.getItem(slot);
        upgrades.setItem(slot, stack.copyWithCount(1));
        stack.consume(1, player);
        if (!old.isEmpty()) player.getInventory().placeItemBackInInventory(old);
        return null;
    }

    // True if one of the side's type slots (starting at first) already holds this upgrade or one of the same type,
    // skipping ignoreSlot.
    public static boolean hasTypeUpgrade(Container container, int first, ItemStack stack, int ignoreSlot) {
        for (int i = first + 1; i < first + UPGRADES_PER_SIDE; i++) {
            ItemStack held = container.getItem(i);
            if (i == ignoreSlot || held.isEmpty()) continue;
            if (ItemStack.isSameItem(held, stack)) return true;
            for (TagKey<Item> type : ModTags.Items.UPGRADE_COMPAT) if (held.is(type) && stack.is(type)) return true;
        }
        return false;
    }

    public boolean isCreative(Direction dir) {
        return upgrade(dir, 0).is(ModItems.CREATIVE_UPGRADE.get());
    }

    public Rates rates(Direction dir) {
        if (isCreative(dir)) return CREATIVE;
        ServerConfig.Rates c = ServerConfig.base;
        var rift = ServerConfig.riftBase;
        for (int t = 0; t < ModItems.TIER_UPGRADES.size(); t++) {
            if (!upgrade(dir, 0).is(ModItems.TIER_UPGRADES.get(t).get())) continue;
            c = ServerConfig.upgradeTiers.get(t);
            rift = ServerConfig.riftTiers.get(t);
        }
        return new Rates(c.transferRate().get(),
                c.itemTransferRate().get(), c.fluidTransferRate().get(), c.energyTransferRate().get(), rift.get());
    }

    // Items always move, every other type needs its type upgrade (or creative).
    public boolean movesType(Direction dir, TagKey<Item> type) {
        if (isCreative(dir)) return true;
        for (int i = 1; i < UPGRADES_PER_SIDE; i++) if (upgrade(dir, i).is(type)) return true;
        return false;
    }

    private boolean isActive(Direction dir) {
        return getRedstone(dir).allows(level.getBestNeighborSignal(worldPosition));
    }

    // Live values for the side's GUI: 0 = mode, 1 = redstone, 2 = insert channel, 3 = extract channel, 4 = distribution,
    // 5 = speed in ticks, 6 = fastest speed the tier allows.
    public ContainerData sideData(Direction dir) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> getBlockState().getValue(SIDES.get(dir)).ordinal();
                    case 1 -> getRedstone(dir).ordinal();
                    case 2 -> getInsertChannel(dir);
                    case 3 -> getExtractChannel(dir);
                    case 4 -> getDistribution(dir).ordinal();
                    case 5 -> getSpeed(dir);
                    default -> fastestSpeed(dir);
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 7;
            }
        };
    }

    // Each extracting side runs on its own timer, since upgrades give sides different rates.
    public static void serverTick(Level level, BlockPos pos, BlockState state, PipeBlockEntity be) {
        long start = System.nanoTime();
        tick(level, pos, state, be);
        PipesCommand.nanos += System.nanoTime() - start;
    }

    private static void tick(Level level, BlockPos pos, BlockState state, PipeBlockEntity be) {
        long time = level.getGameTime();
        if (time == be.lastTickTime) return;
        be.lastTickTime = time;
        if (be.loadedWait != null) {
            for (int i = 0; i < Math.min(be.loadedWait.length, 6); i++) {
                be.nextExtract[i] = time + be.loadedWait[i];
                if (be.loadedWait[i] > be.getSpeed(Direction.values()[i])) {
                    be.sleep[i] = be.loadedWait[i];
                    be.blocked[i] = true; // the reason wasn't saved, so either kind of wake may end it
                }
            }
            be.loadedWait = null;
        }
        if (state != be.canRunState) be.updateCanRun(state); // a side's mode changed
        for (Direction dir : Direction.values()) {
            int i = dir.ordinal();
            if (!be.getCanThisSideRun(dir) || time < be.nextExtract[i]) continue;
            List<Target> targets = be.targets();
            int channel = be.getExtractChannel(dir);
            List<Target> dests = be.distribute(dir, targets.stream().filter(t -> t.channel == channel).toList());
            Rates rates = be.rates(dir);
            PipeFilter extractFilter = be.getFilter(dir, false);
            Map<PipeFilter.Entry, Integer> batches = new HashMap<>(); // per transfer, shared by every target
            int moved = push(be, dir, Capabilities.Item.BLOCK, dests, rates.items,
                    (a, b, n, t) -> moveFiltered(a, b, extractFilter, t.filter, t.pipe.refused(t.dir), n, batches));
            if (be.movesType(dir, ModTags.Items.FLUID_UPGRADE_COMPAT))
                moved += push(be, dir, Capabilities.Fluid.BLOCK, dests, rates.fluid,
                        (a, b, n, t) -> moveFiltered(a, b, extractFilter, t.filter, t.pipe.refused(t.dir), n, batches));
            if (be.movesType(dir, ModTags.Items.ENERGY_UPGRADE_COMPAT))
                moved += push(be, dir, Capabilities.Energy.BLOCK, dests, rates.energy, (a, b, n, t) -> EnergyHandlerUtil.move(a, b, n, null));
            if (OmniPipes.RIFTS_LOADED && be.movesType(dir, ModTags.Items.RIFT_UPGRADE_COMPAT))
                moved += RiftEnergy.push(be, dir, dests, rates.rift);
            be.lastExtract[i] = time;
            PipesCommand.tries++;
            if (moved > 0) PipesCommand.moves++;
            be.nextExtract[i] = time + be.backoff(i, moved > 0, be.getSpeed(dir));
            be.blocked[i] = be.sleep[i] > 0 && be.hasWork(dir, extractFilter);
        }
    }

    // Each failure doubles the wait (up to maxSleepTicks), a success goes straight back to normal speed.
    private long backoff(int i, boolean moved, int speed) {
        if (moved || !ServerConfig.enablePipeOptimizations.get()) sleep[i] = 0;
        else sleep[i] = Math.min(ServerConfig.maxSleepTicks.get(), Math.max(sleep[i], speed) * 2);
        return Math.max(speed, sleep[i]);
    }

    public int sleepTicks(Direction dir) {
        return sleep[dir.ordinal()];
    }

    // Whether the source holds anything movable, so a failure was down to full targets.
    private boolean hasWork(Direction dir, PipeFilter filter) {
        var items = neighbor(Capabilities.Item.BLOCK, dir);
        if (items != null && hasAny(items, filter)) return true;
        if (movesType(dir, ModTags.Items.FLUID_UPGRADE_COMPAT)) {
            var fluids = neighbor(Capabilities.Fluid.BLOCK, dir);
            if (fluids != null && hasAny(fluids, filter)) return true;
        }
        if (movesType(dir, ModTags.Items.ENERGY_UPGRADE_COMPAT)) {
            var energy = neighbor(Capabilities.Energy.BLOCK, dir);
            if (energy != null && energy.getAmountAsLong() > 0) return true;
        }
        return OmniPipes.RIFTS_LOADED && movesType(dir, ModTags.Items.RIFT_UPGRADE_COMPAT) && RiftEnergy.hasAny(this, dir);
    }

    private static <R extends RegisteredResource<?>> boolean hasAny(ResourceHandler<R> handler, PipeFilter filter) {
        for (int i = 0; i < handler.size(); i++) {
            R r = handler.getResource(i);
            if (!r.isEmpty() && filter.allows(r)) return true;
        }
        return false;
    }

    public void wake() {
        wake(false);
    }

    private void wake(boolean onlyBlocked) {
        if (isRemoved()) return;
        for (Direction dir : Direction.values()) {
            int i = dir.ordinal();
            if (sleep[i] == 0 || onlyBlocked && !blocked[i]) continue;
            sleep[i] = 0;
            nextExtract[i] = Math.min(nextExtract[i], lastExtract[i] + getSpeed(dir));
        }
    }

    private static final int REFUSED_TICKS = 20;

    Set<Object> refused(Direction dir) {
        int i = dir.ordinal();
        if (refused[i] == null) refused[i] = new HashSet<>();
        long now = level.getGameTime();
        if (!ServerConfig.enablePipeOptimizations.get() || now - refusedSince[i] >= REFUSED_TICKS) {
            refused[i].clear();
            refusedSince[i] = now;
        }
        return refused[i];
    }

    public void forgetRefused(@Nullable Direction dir) {
        for (Direction d : Direction.values())
            if ((dir == null || d == dir) && refused[d.ordinal()] != null) refused[d.ordinal()].clear();
    }

    public void wakeNetwork() {
        if (network != null && level != null) network.wake(level.getGameTime());
        else wake();
    }

    public interface Mover<H> {
        int move(H from, @Nullable H to, int amount, Target target);
    }

    public static <H> int push(PipeBlockEntity pipe, Direction dir, BlockCapability<H, @Nullable Direction> cap,
                               List<Target> targets, int amount, Mover<H> mover) {
        H from = pipe.neighbor(cap, dir);
        if (from == null) return 0;
        BlockPos src = pipe.worldPosition.relative(dir);
        int left = amount;
        for (Target t : targets) {
            if (left <= 0) break;
            if (!t.pos.equals(src)) left -= mover.move(from, t.pipe.neighbor(cap, t.dir), left, t);
        }
        return amount - left;
    }

    // Cached per side and type, NeoForge clears it when that block changes.
    @SuppressWarnings("unchecked")
    public <H> @Nullable H neighbor(BlockCapability<H, @Nullable Direction> cap, Direction dir) {
        if (isRemoved() || !(level instanceof ServerLevel server)) return null;
        var caches = (BlockCapabilityCache<H, @Nullable Direction>[]) capabilityCaches.computeIfAbsent(cap, c -> new BlockCapabilityCache[6]);
        var cache = caches[dir.ordinal()];
        if (cache == null) {
            cache = BlockCapabilityCache.create(cap, server, worldPosition.relative(dir), dir.getOpposite(), () -> !isRemoved(), () -> {});
            caches[dir.ordinal()] = cache;
        }
        return cache.getCapability();
    }

    // Moves up to amount items or fluids that pass this side's extract filter and the target's insert filter, one resource at
    // a time so filter amounts apply: an extract entry moves at most that many per transfer (tracked in batches
    // across all targets), an insert entry fills the target only up to that many.
    private static <R extends RegisteredResource<?>> int moveFiltered(ResourceHandler<R> from, @Nullable ResourceHandler<R> to,
                                 PipeFilter extract, PipeFilter insert, Set<Object> refused, int amount, Map<PipeFilter.Entry, Integer> batches) {
        if (to == null) return 0;
        int moved = 0;
        Map<PipeFilter.Entry, Integer> stocked = new HashMap<>();
        @Nullable R skipped = null; // cheap check before hashing
        for (int i = 0; i < from.size() && moved < amount; i++) {
            R r = from.getResource(i);
            if (r.isEmpty() || r.equals(skipped)) continue;
            if (refused.contains(r)) {
                skipped = r;
                continue;
            }
            if (!extract.allows(r) || !insert.allows(r)) continue;
            int limit = amount - moved;
            int batch = extract.amount(r);
            PipeFilter.Entry batchEntry = batch > 0 ? extract.match(r) : null;
            if (batchEntry != null) limit = Math.min(limit, batch - batches.getOrDefault(batchEntry, 0));
            int stock = insert.amount(r);
            PipeFilter.Entry stockEntry = stock > 0 ? insert.match(r) : null;
            if (stockEntry != null) limit = Math.min(limit, stock - stocked.computeIfAbsent(stockEntry, e -> count(to, e, insert)));
            if (limit <= 0) continue;
            int done = moveSlot(from, i, r, to, limit, refused);
            if (batchEntry != null) batches.merge(batchEntry, done, Integer::sum);
            if (stockEntry != null) stocked.merge(stockEntry, done, Integer::sum);
            moved += done;
        }
        return moved;
    }

    // ResourceHandlerUtil.move for one slot. A target taking less than offered is out of room, so it's refused.
    private static <R extends RegisteredResource<?>> int moveSlot(ResourceHandler<R> from, int index, R resource, ResourceHandler<R> to,
                                                                  int amount, Set<Object> refused) {
        try (Transaction transaction = Transaction.openRoot()) {
            int available;
            try (Transaction simulate = Transaction.open(transaction)) {
                available = from.extract(index, resource, amount, simulate);
            }
            if (available == 0) return 0;
            int inserted = to.insert(resource, available, transaction);
            if (inserted < available) refused.add(resource);
            if (inserted == 0 || inserted != from.extract(index, resource, inserted, transaction)) return 0;
            transaction.commit();
            return inserted;
        }
    }

    // How much the handler holds of everything that matches this filter entry.
    private static <R extends RegisteredResource<?>> int count(ResourceHandler<R> handler, PipeFilter.Entry entry, PipeFilter filter) {
        int total = 0;
        for (int i = 0; i < handler.size(); i++)
            if (!handler.getResource(i).isEmpty() && filter.match(handler.getResource(i)) == entry) total += handler.getAmountAsInt(i);
        return total;
    }

    public Object networkId() {
        if (network == null || !network.valid) targets();
        return network;
    }

    public record NetworkInfo(Object id, String dimension, BlockPos at, int pipes, int targets, int extractSides, int sleeping, int blocked) {}

    public NetworkInfo networkInfo() {
        Object id = networkId();
        int targets = 0, extracting = 0, sleeping = 0, blockedSides = 0;
        for (PipeBlockEntity pipe : network.members) {
            for (Direction dir : Direction.values()) {
                PipeBlock.Side side = pipe.getBlockState().getValue(SIDES.get(dir));
                if (side.inserts()) targets++;
                if (!side.extracts()) continue;
                extracting++;
                if (pipe.sleep[dir.ordinal()] > 0) sleeping++;
                if (pipe.sleep[dir.ordinal()] > 0 && pipe.blocked[dir.ordinal()]) blockedSides++;
            }
        }
        return new NetworkInfo(id, level.dimension().identifier().toString(), worldPosition, network.members.size(), targets, extracting, sleeping, blockedSides);
    }

    public record Target(PipeBlockEntity pipe, Direction dir, BlockPos pos, int channel, PipeFilter filter) {}

    // Every insert side in the network, closest first.
    private List<Target> findTargets() {
        boolean fresh = network == null || !network.valid;
        if (fresh) {
            network = new Network();
            PipesCommand.rebuilds++;
        }
        List<Target> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>(List.of(worldPosition));
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(seen);
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            BlockState s = level.getBlockState(p);
            if (!(s.getBlock() instanceof PipeBlock) || !(level.getBlockEntity(p) instanceof PipeBlockEntity be)) continue;
            if (fresh) {
                be.network = network;
                network.members.add(be);
            }
            for (Direction dir : Direction.values()) {
                PipeBlock.Side side = s.getValue(SIDES.get(dir));
                BlockPos n = p.relative(dir);
                if (side == PipeBlock.Side.PIPE) {
                    if (level.isLoaded(n) && seen.add(n)) queue.add(n);
                } else if (side.inserts() && be.isActive(dir)) {
                    out.add(new Target(be, dir, n, be.getInsertChannel(dir), be.getFilter(dir, true)));
                }
            }
        }
        return out;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putIntArray("redstone", Arrays.stream(redstone).mapToInt(Enum::ordinal).toArray());
        output.putIntArray("distribution", Arrays.stream(distribution).mapToInt(Enum::ordinal).toArray());
        output.putIntArray("speed", speed);
        output.putIntArray("disabled_from", Arrays.stream(disabledFrom).mapToInt(side -> side == null ? -1 : side.ordinal()).toArray());
        // Saved as ticks left, since game time keeps running while the chunk is unloaded. Without a level there's
        // no clock to compare against, so no wait.
        if (level != null) {
            long now = level.getGameTime();
            output.putIntArray("extract_wait", Arrays.stream(nextExtract).mapToInt(t -> (int) Math.clamp(t - now, 0, Integer.MAX_VALUE)).toArray());
        }
        output.putIntArray("insert_channel", insertChannel);
        output.putIntArray("extract_channel", extractChannel);
        ContainerHelper.saveAllItems(output, upgrades.getItems());
        for (Direction dir : Direction.values()) {
            insertFilters[dir.ordinal()].save(output.child("insert_filter_" + dir.getName()));
            extractFilters[dir.ordinal()].save(output.child("extract_filter_" + dir.getName()));
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getIntArray("redstone").ifPresent(modes -> {
            for (int i = 0; i < Math.min(modes.length, 6); i++) redstone[i] = RedstoneMode.values()[modes[i]];
        });
        input.getIntArray("speed").ifPresent(s -> System.arraycopy(s, 0, speed, 0, Math.min(s.length, 6)));
        loadedWait = input.getIntArray("extract_wait").orElse(null);
        input.getIntArray("disabled_from").ifPresent(d -> {
            for (int i = 0; i < Math.min(d.length, 6); i++)
                disabledFrom[i] = d[i] >= 0 && d[i] < PipeBlock.Side.values().length ? PipeBlock.Side.values()[d[i]] : null;
        });
        input.getIntArray("distribution").ifPresent(modes -> {
            for (int i = 0; i < Math.min(modes.length, 6); i++) distribution[i] = Distribution.values()[modes[i]];
        });
        input.getIntArray("insert_channel").ifPresent(c -> System.arraycopy(c, 0, insertChannel, 0, Math.min(c.length, 6)));
        input.getIntArray("extract_channel").ifPresent(c -> System.arraycopy(c, 0, extractChannel, 0, Math.min(c.length, 6)));
        ContainerHelper.loadAllItems(input, upgrades.getItems());
        for (Direction dir : Direction.values()) {
            input.child("insert_filter_" + dir.getName()).ifPresent(insertFilters[dir.ordinal()]::load);
            input.child("extract_filter_" + dir.getName()).ifPresent(extractFilters[dir.ordinal()]::load);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) Containers.dropContents(level, pos, upgrades);
    }
}
