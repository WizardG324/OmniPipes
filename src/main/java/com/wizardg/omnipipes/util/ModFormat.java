package com.wizardg.omnipipes.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ModFormat {
    // 1.25, 32, trailing zeros dropped.
    public static String decimal(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    // 999, 25k, 1.5M
    public static String compact(int value) {
        if (value >= 1_000_000) return decimal(value / 1_000_000.0) + "M";
        if (value >= 1_000) return decimal(value / 1_000.0) + "k";
        return String.valueOf(value);
    }
}
