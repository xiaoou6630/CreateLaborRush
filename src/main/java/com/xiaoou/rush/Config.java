package com.xiaoou.rush;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue DESTROY_CHANCE;

    public static final ForgeConfigSpec.DoubleValue DESTROY_RATIO_MIN;

    public static final ForgeConfigSpec.DoubleValue DESTROY_RATIO_MAX;

    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.push("work");

        DESTROY_CHANCE = builder
            .comment("Probability (0.0–1.0) that an item is destroyed during Work processing")
            .defineInRange("destroyChance", 0.15, 0.0, 1.0);

        DESTROY_RATIO_MIN = builder
            .comment("When destroy triggers, minimum percentage (0.0–1.0) of the batch that will be destroyed")
            .defineInRange("destroyRatioMin", 0.2, 0.0, 1.0);

        DESTROY_RATIO_MAX = builder
            .comment("When destroy triggers, maximum percentage (0.0–1.0) of the batch that will be destroyed")
            .defineInRange("destroyRatioMax", 0.5, 0.0, 1.0);

        builder.pop();
        SPEC = builder.build();
    }
}