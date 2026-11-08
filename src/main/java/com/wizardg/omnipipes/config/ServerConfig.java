package com.wizardg.omnipipes.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class ServerConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<Boolean> enablePipeOptimizations;
    public static final ModConfigSpec.ConfigValue<Integer> maxSleepTicks;
    public static final Rates base;
    public static final List<Rates> upgradeTiers = new ArrayList<>(); // index 0 = tier 1
    public static final ModConfigSpec.ConfigValue<Integer> chemicalBase;
    public static final List<ModConfigSpec.ConfigValue<Integer>> chemicalTiers = new ArrayList<>(); // index 0 = tier 1
    public static final ModConfigSpec.ConfigValue<Integer> sourceBase;
    public static final List<ModConfigSpec.ConfigValue<Integer>> sourceTiers = new ArrayList<>(); // index 0 = tier 1

    public record Rates(ModConfigSpec.ConfigValue<Integer> transferRate,
                        ModConfigSpec.ConfigValue<Integer> itemTransferRate,
                        ModConfigSpec.ConfigValue<Integer> fluidTransferRate,
                        ModConfigSpec.ConfigValue<Integer> energyTransferRate) {}

    static {
        BUILDER.comment("Pipe settings").push("Pipes");

        enablePipeOptimizations = BUILDER
                .comment("Let sides that keep failing to extract sleep longer between tries, instead of retrying at full speed.")
                .define("enablePipeOptimizations", true);

        maxSleepTicks = BUILDER
                .comment("Longest a side sleeps. Each failed extract doubles its wait, a success puts it back to normal speed,",
                        "and a change next to the pipe wakes it.")
                .defineInRange("maxSleepTicks", 100, 1, 1200);

        BUILDER.pop();

        BUILDER.comment("Pipe transfer settings, sub sections override these for each upgrade").push("settings");
        base = rates(40, 8, 2000, 4000);

        // Might add higher tier upgrades to keep up with end game. {1, 128, 128_000, 464_000}, {1, 256, 256_000, 1_280_000}
        int[][] tierDefaults = {{30, 16, 5000, 12_000}, {15, 32, 16000, 36_000}, {5, 48, 32_000, 98_000}, {1, 64, 64_000, 232_000}};
        for (int tier = 1; tier <= tierDefaults.length; tier++) {
            int[] d = tierDefaults[tier - 1];
            BUILDER.push("upgrade_tier_" + tier);
            upgradeTiers.add(rates(d[0], d[1], d[2], d[3]));
            BUILDER.pop();
        }

        BUILDER.pop();

        BUILDER.comment("Mekanism settings, used by the Chemical Upgrade when Mekanism is installed. Sub sections override this for each upgrade.",
                "Heat has no rate, it moves instantly.").push("mekanism");
        chemicalBase = chemicalRate(2000);
        int[] chemicalDefaults = {5000, 16_000, 32_000, 64_000};
        for (int tier = 1; tier <= chemicalDefaults.length; tier++) {
            BUILDER.push("upgrade_tier_" + tier);
            chemicalTiers.add(chemicalRate(chemicalDefaults[tier - 1]));
            BUILDER.pop();
        }
        BUILDER.pop();

        // A source jar holds 10000, so tier 4 moves a full jar per transfer.
        BUILDER.comment("Ars Nouveau settings, used by the Source Upgrade when Ars Nouveau is installed. Sub sections override this for each upgrade.")
                .push("ars_nouveau");
        sourceBase = sourceRate(500);
        int[] sourceDefaults = {1000, 2500, 5000, 10_000};
        for (int tier = 1; tier <= sourceDefaults.length; tier++) {
            BUILDER.push("upgrade_tier_" + tier);
            sourceTiers.add(sourceRate(sourceDefaults[tier - 1]));
            BUILDER.pop();
        }
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private static ModConfigSpec.ConfigValue<Integer> chemicalRate(int amount) {
        return BUILDER.comment("Chemicals in mB moved per transfer.").defineInRange("chemicalTransferRate", amount, 1, Integer.MAX_VALUE);
    }

    private static ModConfigSpec.ConfigValue<Integer> sourceRate(int amount) {
        return BUILDER.comment("Source moved per transfer.").defineInRange("sourceTransferRate", amount, 1, Integer.MAX_VALUE);
    }

    private static Rates rates(int ticks, int items, int fluid, int energy) {
        return new Rates(
                BUILDER.comment("Ticks between transfers.").defineInRange("transferRate", ticks, 1, 1200),
                BUILDER.comment("Items moved per transfer.").defineInRange("itemTransferRate", items, 1, 1024),
                BUILDER.comment("Fluid in mB moved per transfer.").defineInRange("fluidTransferRate", fluid, 1, Integer.MAX_VALUE),
                BUILDER.comment("Energy moved per transfer.").defineInRange("energyTransferRate", energy, 1, Integer.MAX_VALUE));
    }
}
