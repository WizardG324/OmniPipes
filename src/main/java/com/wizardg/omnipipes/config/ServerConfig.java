package com.wizardg.omnipipes.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class ServerConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<Boolean> enablePipeOptimizations;
    public static final ModConfigSpec.ConfigValue<Integer> retriesBeforePausing;
    public static final ModConfigSpec.ConfigValue<Integer> extractRecheckDelay;
    public static final Rates base;
    public static final List<Rates> upgradeTiers = new ArrayList<>(); // index 0 = tier 1

    public record Rates(ModConfigSpec.ConfigValue<Integer> transferRate,
                        ModConfigSpec.ConfigValue<Integer> itemTransferRate,
                        ModConfigSpec.ConfigValue<Integer> fluidTransferRate,
                        ModConfigSpec.ConfigValue<Integer> energyTransferRate) {}

    static {
        BUILDER.comment("Pipe settings").push("Pipes");

        enablePipeOptimizations = BUILDER
                .comment("Pause pipes that keep failing to extract, instead of retrying at full speed.")
                .define("enablePipeOptimizations", true);

        retriesBeforePausing = BUILDER
                .comment("How many failed extracts that can happen before pausing")
                .defineInRange("retriesBeforePausing", 2, 0, 100);

        extractRecheckDelay = BUILDER
                .comment("Ticks a pipe pauses for once it runs out of retries.")
                .defineInRange("extractRecheckDelay", 50, 0, 1200);

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
        SPEC = BUILDER.build();
    }

    private static Rates rates(int ticks, int items, int fluid, int energy) {
        return new Rates(
                BUILDER.comment("Ticks between transfers.").defineInRange("transferRate", ticks, 1, 1200),
                BUILDER.comment("Items moved per transfer.").defineInRange("itemTransferRate", items, 1, 1024),
                BUILDER.comment("Fluid in mB moved per transfer.").defineInRange("fluidTransferRate", fluid, 1, Integer.MAX_VALUE),
                BUILDER.comment("Energy moved per transfer.").defineInRange("energyTransferRate", energy, 1, Integer.MAX_VALUE));
    }
}
