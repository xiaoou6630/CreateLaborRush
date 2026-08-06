package com.xiaoou.rush;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue DESTROY_CHANCE;

    public static final ForgeConfigSpec.DoubleValue DESTROY_RATIO_MIN;

    public static final ForgeConfigSpec.DoubleValue DESTROY_RATIO_MAX;

    public static final ForgeConfigSpec.BooleanValue REBELLION_ENABLED;
    public static final ForgeConfigSpec.IntValue REBELLION_TRIGGER_TIME;
    public static final ForgeConfigSpec.DoubleValue REBELLION_CHANCE;
    public static final ForgeConfigSpec.IntValue REBELLION_DURATION;
    public static final ForgeConfigSpec.IntValue REBELLION_RADIUS;
    public static final ForgeConfigSpec.BooleanValue REBELLION_CAN_DESTROY;
    public static final ForgeConfigSpec.IntValue REBELLION_DESTROY_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue REBELLION_ATTACK_RATIO;

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
        builder.push("rebellion");

        REBELLION_ENABLED = builder
            .comment("Enable worker rebellion system")
            .define("enableRebellion", false);

        REBELLION_TRIGGER_TIME = builder
            .comment("Base time (in seconds) before rebellion can trigger")
            .defineInRange("rebellionTriggerTime", 300, 10, 3600);

        REBELLION_CHANCE = builder
            .comment("Base probability of rebellion triggering per tick check")
            .defineInRange("rebellionChance", 0.05, 0.0, 1.0);

        REBELLION_DURATION = builder
            .comment("Duration of rebellion in seconds")
            .defineInRange("rebellionDuration", 30, 5, 600);

        REBELLION_RADIUS = builder
            .comment("Radius for rebellion detection (blocks)")
            .defineInRange("rebellionRadius", 5, 1, 32);

        REBELLION_CAN_DESTROY = builder
            .comment("Whether rebels can destroy devices")
            .define("canDestroyDevices", false);

        REBELLION_DESTROY_COOLDOWN = builder
            .comment("Cooldown between device destructions (seconds)")
            .defineInRange("destroyCooldown", 10, 1, 60);

        REBELLION_ATTACK_RATIO = builder
            .comment("Ratio of rebels that attack player (0.0-1.0), rest destroy devices")
            .defineInRange("attackPlayerRatio", 0.5, 0.0, 1.0);

        builder.pop();
        SPEC = builder.build();
    }
}