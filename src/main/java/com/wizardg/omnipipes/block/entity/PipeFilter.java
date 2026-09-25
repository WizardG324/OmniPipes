package com.wizardg.omnipipes.block.entity;

import com.wizardg.omnipipes.item.TagFilterItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.RegisteredResource;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Filter for one direction of a pipe side. Starts as an empty blacklist, so everything passes.
// An entry that holds a fluid (a bucket, a tank) also matches that fluid, its amount then counts in buckets.
// A Tag Filter entry matches every item and fluid in any of its tags.
public class PipeFilter {
    public static final int MAX_ENTRIES = 256; // hidden cap so a runaway list can't bloat the save
    public static final int MAX_AMOUNT = 9999;

    // amount 0 = no limit. On a whitelist, extract moves at most this many per transfer and insert fills the
    // target up to this many. Blacklists ignore it.
    public record Entry(ItemStack stack, int amount) {}

    private final List<Entry> entries = new ArrayList<>();
    private record Key(RegisteredResource<?> resource, Entry entry) {}
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

    // Amount for this resource in its own units (mB for fluids), 0 when there is none (or on a blacklist).
    public int amount(RegisteredResource<?> resource) {
        Entry entry = whitelist ? match(resource) : null;
        if (entry == null) return 0;
        return resource instanceof FluidResource ? entry.amount() * FluidType.BUCKET_VOLUME : entry.amount();
    }

    public static boolean holdsFluid(ItemStack stack) {
        return !FluidUtil.getFirstStackContained(stack).isEmpty();
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

    public void setAmount(int index, int amount) {
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
            add(ItemResource.of(e.stack()), e);
            FluidResource fluid = FluidResource.of(FluidUtil.getFirstStackContained(e.stack()));
            if (!fluid.isEmpty()) add(fluid, e);
        }
    }

    private void add(RegisteredResource<?> resource, Entry e) {
        byValue.computeIfAbsent(resource.value(), v -> new ArrayList<>()).add(new Key(resource, e));
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
