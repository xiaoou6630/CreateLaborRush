package com.xiaoou.rush;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Mod configuration. Loaded from the common config file.
 */
public class Config {
    static final ModConfigSpec SPEC;
    /** Probability (0.0–1.0) that an item is destroyed instead of produced. */
    public static final ModConfigSpec.DoubleValue DESTROY_CHANCE;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("work");
        DESTROY_CHANCE = builder
            .comment("Probability (0.0–1.0) that an item is destroyed during Work processing")
            .defineInRange("destroyChance", 0.15, 0.0, 1.0);
        builder.pop();
        SPEC = builder.build();
    }
}
