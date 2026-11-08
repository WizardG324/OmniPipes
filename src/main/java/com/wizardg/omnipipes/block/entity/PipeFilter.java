package com.wizardg.omnipipes.block.entity;

import com.wizardg.omnipipes.OmniPipes;
import com.wizardg.omnipipes.compat.ars.ArsCompat;
import com.wizardg.omnipipes.compat.mekanism.MekanismCompat;
import com.wizardg.omnipipes.item.ModDataComponents;
import com.wizardg.omnipipes.item.TagFilterItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

// Filter for one direction of a pipe side. Starts as an empty blacklist, so everything passes.
// An entry marked with FILTER_CONTENTS (a bucket, a tank, a source jar, a battery) matches what's inside instead of the item.
// A Tag Filter entry matches every item and fluid in any of its tags.
public class PipeFilter {
    public static final int MAX_ENTRIES = 256; // hidden cap so a runaway list can't bloat the save
    public static final int MAX_AMOUNT = 999_999_999; // nine digits, what the amount box takes
    private static final Object ENERGY = new Object(); // lookup key, energy has no types

    // amount 0 = no limit. On a whitelist, extract moves at most this many per transfer and insert fills the
    // target up to this many. Blacklists ignore it.
    public record Entry(ItemStack stack, int amount) {}

    private final List<Entry> entries = new ArrayList<>();
    // Item or fluid -> entries with a sample to compare components against, so long lists stay fast.
    private record Key(Object sample, Entry entry) {}
    private final Map<Object, List<Key>> byValue = new HashMap<>();
    private record TagEntry(List<TagKey<Item>> items, List<TagKey<Fluid>> fluids, Entry entry) {}
    private final List<TagEntry> tagEntries = new ArrayList<>(); // checked after the exact entries
    private final Runnable onChange;
    private boolean whitelist;
    private boolean matchComponents;

    public PipeFilter(Runnable onChange) {
        this.onChange = onChange;
    }

    public @Nullable Entry match(ItemStack stack) {
        Entry exact = exact(stack.getItem(), sample -> sample instanceof ItemStack s && ItemStack.isSameItemSameComponents(s, stack));
        if (exact != null) return exact;
        for (TagEntry tag : tagEntries)
            if (tag.items().stream().anyMatch(stack::is)) return tag.entry();
        return null;
    }

    public @Nullable Entry match(FluidStack fluid) {
        Entry exact = exact(fluid.getFluid(), sample -> sample instanceof FluidStack f && FluidStack.isSameFluidSameComponents(f, fluid));
        if (exact != null) return exact;
        for (TagEntry tag : tagEntries)
            if (tag.fluids().stream().anyMatch(fluid::is)) return tag.entry();
        return null;
    }

    // For other mods' types (compat/mekanism), only entries holding one match.
    public @Nullable Entry match(Object value, Predicate<Object> sameComponents) {
        return exact(value, sameComponents);
    }

    private @Nullable Entry exact(Object value, Predicate<Object> sameComponents) {
        List<Key> same = byValue.get(value);
        if (same == null) return null;
        if (!matchComponents) return same.get(0).entry();
        return same.stream().filter(k -> sameComponents.test(k.sample())).map(Key::entry).findFirst().orElse(null);
    }

    public boolean allows(ItemStack stack) {
        return (match(stack) != null) == whitelist;
    }

    public boolean allows(FluidStack fluid) {
        return (match(fluid) != null) == whitelist;
    }

    // Amounts, 0 when there is none (or on a blacklist). Items count, fluids and chemicals in mB, source as is, energy in FE.
    public int amount(ItemStack stack) {
        Entry entry = whitelist ? match(stack) : null;
        return entry == null ? 0 : entry.amount();
    }

    public int amount(FluidStack fluid) {
        Entry entry = whitelist ? match(fluid) : null;
        return entry == null ? 0 : entry.amount();
    }

    // Energy ignores a whitelist that doesn't list it, so filters made before energy entries existed keep passing it.
    public boolean allowsEnergy() {
        return whitelist || exact(ENERGY, sample -> true) == null;
    }

    public @Nullable Entry matchEnergy() {
        return whitelist ? exact(ENERGY, sample -> true) : null;
    }

    public int energyAmount() {
        Entry entry = matchEnergy();
        return entry == null ? 0 : entry.amount();
    }

    // What one scroll step changes an entry's amount by: an item, a bucket of fluid or chemical, 100 source or 1000 FE.
    public static int amountStep(ItemStack stack) {
        return switch (contentsKind(stack)) {
            case "fluid", "chemical" -> FluidType.BUCKET_VOLUME;
            case "source" -> 100;
            case "energy" -> 1000;
            case null, default -> 1;
        };
    }

    public static @Nullable String contentsKind(ItemStack stack) {
        if (!filtersContents(stack)) return null;
        if (holdsFluid(stack)) return "fluid";
        if (OmniPipes.ARS_NOUVEAU_LOADED && ArsCompat.isSourceJar(stack)) return "source";
        if (OmniPipes.MEKANISM_LOADED && MekanismCompat.holdsChemical(stack)) return "chemical";
        if (holdsEnergy(stack)) return "energy";
        return null;
    }

    public static boolean holdsFluid(ItemStack stack) {
        return FluidUtil.getFluidContained(stack).isPresent();
    }

    public static boolean holdsEnergy(ItemStack stack) {
        return stack.getCapability(Capabilities.EnergyStorage.ITEM) != null;
    }

    public static boolean filtersContents(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.FILTER_CONTENTS, false);
    }

    // An entry for the fluid, chemical, source or energy inside stack, empty if it holds none.
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
            List<ResourceLocation> tags = TagFilterItem.tags(e.stack());
            if (!tags.isEmpty()) {
                tagEntries.add(new TagEntry(tags.stream().map(id -> TagKey.create(Registries.ITEM, id)).toList(),
                        tags.stream().map(id -> TagKey.create(Registries.FLUID, id)).toList(), e));
                continue;
            }
            if (!filtersContents(e.stack())) {
                add(e.stack().getItem(), e.stack(), e);
                continue;
            }
            switch (contentsKind(e.stack())) {
                case "fluid" -> FluidUtil.getFluidContained(e.stack()).ifPresent(fluid -> add(fluid.getFluid(), fluid, e));
                case "source" -> add(ArsCompat.key(), ArsCompat.key(), e);
                case "chemical" -> MekanismCompat.addHeld(e.stack(), (value, sample) -> add(value, sample, e));
                case "energy" -> add(ENERGY, ENERGY, e);
                case null, default -> {}
            }
        }
    }

    private void add(Object value, Object sample, Entry e) {
        byValue.computeIfAbsent(value, v -> new ArrayList<>()).add(new Key(sample, e));
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("whitelist", whitelist);
        tag.putBoolean("match_components", matchComponents);
        ListTag items = new ListTag();
        entries.forEach(e -> items.add(e.stack().save(registries)));
        tag.put("items", items);
        tag.putIntArray("amounts", entries.stream().mapToInt(Entry::amount).toArray());
        return tag;
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        whitelist = tag.getBoolean("whitelist");
        matchComponents = tag.getBoolean("match_components");
        int[] amounts = tag.getIntArray("amounts");
        entries.clear();
        ListTag items = tag.getList("items", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = ItemStack.parse(registries, items.get(i)).orElse(ItemStack.EMPTY);
            if (!stack.isEmpty()) entries.add(new Entry(stack, i < amounts.length ? amounts[i] : 0));
        }
        rebuildLookup();
    }
}
