package com.wizardg.omnipipes.block.entity;

import com.wizardg.omnipipes.item.ModDataComponents;
import com.wizardg.omnipipes.item.TagFilterItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.RegisteredResource;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Filter for one direction of a pipe side. Starts as an empty blacklist, so everything passes.
// An entry marked with FILTER_CONTENTS (a bucket, a tank, a battery) matches what's inside instead of the item.
// A Tag Filter entry matches every item and fluid in any of its tags.
public class PipeFilter {
    public static final int MAX_ENTRIES = 256; // hidden cap so a runaway list can't bloat the save
    public static final int MAX_AMOUNT = 999_999_999; // nine digits, what the amount box takes
    private static final Object ENERGY = new Object(); // lookup key, energy has no types

    // amount 0 = no limit. On a whitelist, extract moves at most this many per transfer and insert fills the
    // target up to this many. Blacklists ignore it.
    public record Entry(ItemStack stack, int amount) {}

    private final List<Entry> entries = new ArrayList<>();
    private record Key(Object resource, Entry entry) {}
    private final Map<Object, List<Key>> byValue = new HashMap<>(); // item or fluid -> entries, so long lists stay fast
    private record TagEntry(List<TagKey<Item>> items, List<TagKey<Fluid>> fluids, Entry entry) {}
    private final List<TagEntry> tagEntries = new ArrayList<>(); // checked after the exact entries
    private final Runnable onChange;
    private boolean whitelist;
    private boolean matchComponents;

    public PipeFilter(Runnable onChange) {
        this.onChange = onChange;
    }

    public @Nullable Entry match(RegisteredResource<?> resource) {
        List<Key> same = byValue.get(resource.value());
        if (same != null) {
            if (!matchComponents) return same.get(0).entry();
            var exact = same.stream().filter(k -> k.resource().equals(resource)).findFirst();
            if (exact.isPresent()) return exact.get().entry();
        }
        for (TagEntry tag : tagEntries) {
            if (resource instanceof ItemResource item && tag.items().stream().anyMatch(item::is)) return tag.entry();
            if (resource instanceof FluidResource fluid && tag.fluids().stream().anyMatch(fluid::is)) return tag.entry();
        }
        return null;
    }

    public boolean allows(RegisteredResource<?> resource) {
        return (match(resource) != null) == whitelist;
    }

    // Amount for this resource (mB for fluids), 0 when there is none (or on a blacklist).
    public int amount(RegisteredResource<?> resource) {
        Entry entry = whitelist ? match(resource) : null;
        return entry == null ? 0 : entry.amount();
    }

    // Energy ignores a whitelist that doesn't list it, so filters made before energy entries existed keep passing it.
    public boolean allowsEnergy() {
        return whitelist || !byValue.containsKey(ENERGY);
    }

    public int energyAmount() {
        Entry entry = matchEnergy();
        return entry == null ? 0 : entry.amount();
    }

    public @Nullable Entry matchEnergy() {
        List<Key> same = whitelist ? byValue.get(ENERGY) : null;
        return same == null ? null : same.get(0).entry();
    }

    // What one scroll step changes an entry's amount by: an item, a bucket of fluid or 1000 FE.
    public static int amountStep(ItemStack stack) {
        return switch (contentsKind(stack)) {
            case "fluid" -> FluidType.BUCKET_VOLUME;
            case "energy" -> 1000;
            case null, default -> 1;
        };
    }

    public static @Nullable String contentsKind(ItemStack stack) {
        if (!stack.getOrDefault(ModDataComponents.FILTER_CONTENTS, false)) return null;
        if (!FluidUtil.getFirstStackContained(stack).isEmpty()) return "fluid";
        if (ItemAccess.forStack(stack).getCapability(Capabilities.Energy.ITEM) != null) return "energy";
        return null;
    }

    // An entry for the fluid or energy inside stack, empty if it holds neither.
    public static ItemStack contentsOf(ItemStack stack) {
        ItemStack copy = stack.copyWithCount(1);
        copy.set(ModDataComponents.FILTER_CONTENTS, true);
        return contentsKind(copy) != null ? copy : ItemStack.EMPTY;
    }

    public List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }

    public boolean isWhitelist() {
        return whitelist;
    }

    public boolean matchesComponents() {
        return matchComponents;
    }

    public void toggleWhitelist() {
        whitelist = !whitelist;
        onChange.run();
    }

    public void toggleMatchComponents() {
        matchComponents = !matchComponents;
        onChange.run();
    }

    // Sets the entry at index, or appends when index is past the end. Exact duplicates are ignored.
    public void set(int index, ItemStack stack) {
        ItemStack copy = stack.copyWithCount(1);
        if (entries.stream().anyMatch(e -> ItemStack.isSameItemSameComponents(e.stack(), copy))) return;
        if (index < entries.size()) entries.set(index, new Entry(copy, 0));
        else if (entries.size() < MAX_ENTRIES) entries.add(new Entry(copy, 0));
        else return;
        changed();
    }

    public void setAmount(int index, long amount) {
        if (index >= entries.size()) return;
        entries.set(index, new Entry(entries.get(index).stack(), Math.clamp(amount, 0, MAX_AMOUNT)));
        changed();
    }

    public void remove(int index) {
        if (index >= entries.size()) return;
        entries.remove(index);
        changed();
    }

    private void changed() {
        rebuildLookup();
        onChange.run();
    }

    private void rebuildLookup() {
        byValue.clear();
        tagEntries.clear();
        for (Entry e : entries) {
            List<Identifier> tags = TagFilterItem.tags(e.stack());
            if (!tags.isEmpty()) {
                tagEntries.add(new TagEntry(tags.stream().map(id -> TagKey.create(Registries.ITEM, id)).toList(),
                        tags.stream().map(id -> TagKey.create(Registries.FLUID, id)).toList(), e));
                continue;
            }
            switch (contentsKind(e.stack())) {
                case "fluid" -> {
                    FluidResource fluid = FluidResource.of(FluidUtil.getFirstStackContained(e.stack()));
                    add(fluid.value(), fluid, e);
                }
                case "energy" -> add(ENERGY, ENERGY, e);
                case null, default -> {
                    if (!e.stack().getOrDefault(ModDataComponents.FILTER_CONTENTS, false)) add(e.stack().getItem(), ItemResource.of(e.stack()), e);
                }
            }
        }
    }

    private void add(Object value, Object resource, Entry e) {
        byValue.computeIfAbsent(value, v -> new ArrayList<>()).add(new Key(resource, e));
    }

    public void save(ValueOutput output) {
        output.putBoolean("whitelist", whitelist);
        output.putBoolean("match_components", matchComponents);
        var list = output.list("items", ItemStack.CODEC);
        entries.forEach(e -> list.add(e.stack()));
        output.putIntArray("amounts", entries.stream().mapToInt(Entry::amount).toArray());
    }

    public void load(ValueInput input) {
        whitelist = input.getBooleanOr("whitelist", false);
        matchComponents = input.getBooleanOr("match_components", false);
        int[] amounts = input.getIntArray("amounts").orElse(new int[0]);
        entries.clear();
        input.listOrEmpty("items", ItemStack.CODEC).forEach(stack ->
                entries.add(new Entry(stack, entries.size() < amounts.length ? amounts[entries.size()] : 0)));
        rebuildLookup();
    }
}
