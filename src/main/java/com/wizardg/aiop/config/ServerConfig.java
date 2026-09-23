package com.wizardg.aiop.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ServerConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<Integer> extractRecheckDelay;
    public static final Rates base;
    public static final Rates upgradeBasic;

    public record Rates(ModConfigSpec.ConfigValue<Integer> transferRate,
                        ModConfigSpec.ConfigValue<Integer> itemTransferRate,
                        ModConfigSpec.ConfigValue<Integer> fluidTransferRate,
                        ModConfigSpec.ConfigValue<Integer> energyTransferRate) {}

    static {
        BUILDER.comment("Pipe settings").push("Pipes");

        extractRecheckDelay = BUILDER
                .comment("Ticks a pipe waits before trying again after an extract moves nothing.")
                .defineInRange("extractRecheckDelay", 50, 0, 1200);

        BUILDER.pop();

        BUILDER.comment("Pipe transfer settings, sub sections override these for each upgrade").push("settings");
        base = rates(40, 8, 1250, 4000);

        BUILDER.push("upgrade_basic");
        upgradeBasic = rates(30, 16, 2750, 10_000);

        BUILDER.pop(2);
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
