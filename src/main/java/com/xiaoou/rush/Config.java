package com.xiaoou.rush;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Mod configuration. Loaded from the common config file.
 */
public class Config {
    static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue DESTROY_CHANCE;

    public static final ModConfigSpec.DoubleValue DESTROY_RATIO_MIN;

    public static final ModConfigSpec.DoubleValue DESTROY_RATIO_MAX;

    public static final ModConfigSpec.BooleanValue REBELLION_ENABLED;
    public static final ModConfigSpec.IntValue REBELLION_TRIGGER_TIME;
    public static final ModConfigSpec.DoubleValue REBELLION_CHANCE;
    public static final ModConfigSpec.IntValue REBELLION_DURATION;
    public static final ModConfigSpec.IntValue REBELLION_RADIUS;
    public static final ModConfigSpec.BooleanValue REBELLION_CAN_DESTROY;
    public static final ModConfigSpec.IntValue REBELLION_DESTROY_COOLDOWN;
    public static final ModConfigSpec.DoubleValue REBELLION_DESTROY_INTENSITY;
    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("work");

        DESTROY_CHANCE = builder
            .comment("加工过程中物品被销毁的概率 (0.0–1.0)")
            .defineInRange("destroyChance", 0.15, 0.0, 1.0);

        DESTROY_RATIO_MIN = builder
            .comment("销毁触发时，一批物品被销毁的最小比例 (0.0–1.0)")
            .defineInRange("destroyRatioMin", 0.2, 0.0, 1.0);

        DESTROY_RATIO_MAX = builder
            .comment("销毁触发时，一批物品被销毁的最大比例 (0.0–1.0)")
            .defineInRange("destroyRatioMax", 0.5, 0.0, 1.0);

        builder.pop();
        builder.push("rebellion");

        REBELLION_ENABLED = builder
            .comment("启用工人起义系统")
            .define("enableRebellion", false);

        REBELLION_TRIGGER_TIME = builder
            .comment("起义触发基础时间（秒）")
            .defineInRange("rebellionTriggerTime", 300, 10, 3600);

        REBELLION_CHANCE = builder
            .comment("每次检测起义触发的基础概率")
            .defineInRange("rebellionChance", 0.05, 0.0, 1.0);

        REBELLION_DURATION = builder
            .comment("起义持续时间（秒）")
            .defineInRange("rebellionDuration", 300, 5, 600);

        REBELLION_RADIUS = builder
            .comment("起义检测半径（方块）")
            .defineInRange("rebellionRadius", 5, 1, 32);

        REBELLION_CAN_DESTROY = builder
            .comment("起义工人是否可以拆除设备（仅在开启起义时生效）")
            .define("canDestroyDevices", true);

        REBELLION_DESTROY_COOLDOWN = builder
            .comment("拆家冷却时间（秒）")
            .defineInRange("destroyCooldown", 10, 1, 60);

        REBELLION_DESTROY_INTENSITY = builder
            .comment("拆家强度（0.0-1.0）：越高拆得越勤，0 表示不拆")
            .defineInRange("destroyIntensity", 0.5, 0.0, 1.0);

        builder.pop();
        SPEC = builder.build();
    }
}