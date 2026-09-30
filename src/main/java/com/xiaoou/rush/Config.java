package com.xiaoou.rush;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Mod configuration. Loaded from the common config file.
 */
public class Config {
    public static final ModConfigSpec SPEC;

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
        // translation 必须在 push 之前设置：push 时会消费掉挂起的键并绑定到该分节
        builder.translation("createlaborrush.configuration.work").push("work");

        DESTROY_CHANCE = builder
            .comment("加工过程中物品被销毁的概率 (0.0–1.0)")
            .translation("createlaborrush.config.destroyChance")
            .defineInRange("destroyChance", 0.15, 0.0, 1.0);

        DESTROY_RATIO_MIN = builder
            .comment("销毁触发时，一批物品被销毁的最小比例 (0.0–1.0)")
            .translation("createlaborrush.config.destroyRatioMin")
            .defineInRange("destroyRatioMin", 0.2, 0.0, 1.0);

        DESTROY_RATIO_MAX = builder
            .comment("销毁触发时，一批物品被销毁的最大比例 (0.0–1.0)")
            .translation("createlaborrush.config.destroyRatioMax")
            .defineInRange("destroyRatioMax", 0.5, 0.0, 1.0);

        builder.pop();
        builder.translation("createlaborrush.configuration.rebellion").push("rebellion");

        REBELLION_ENABLED = builder
            .comment("启用工人起义系统")
            .translation("createlaborrush.config.enableRebellion")
            .define("enableRebellion", false);

        REBELLION_TRIGGER_TIME = builder
            .comment("起义触发基础时间（秒）")
            .translation("createlaborrush.config.rebellionTriggerTime")
            .defineInRange("rebellionTriggerTime", 300, 10, 3600);

        REBELLION_CHANCE = builder
            .comment("每次检测起义触发的基础概率")
            .translation("createlaborrush.config.rebellionChance")
            .defineInRange("rebellionChance", 0.05, 0.0, 1.0);

        REBELLION_DURATION = builder
            .comment("起义持续时间（秒）")
            .translation("createlaborrush.config.rebellionDuration")
            .defineInRange("rebellionDuration", 300, 5, 600);

        REBELLION_RADIUS = builder
            .comment("起义检测半径（方块）")
            .translation("createlaborrush.config.rebellionRadius")
            .defineInRange("rebellionRadius", 5, 1, 32);

        REBELLION_CAN_DESTROY = builder
            .comment("起义工人是否可以拆除设备（仅在开启起义时生效）")
            .translation("createlaborrush.config.canDestroyDevices")
            .define("canDestroyDevices", true);

        REBELLION_DESTROY_COOLDOWN = builder
            .comment("拆家冷却时间（秒）")
            .translation("createlaborrush.config.destroyCooldown")
            .defineInRange("destroyCooldown", 10, 1, 60);

        REBELLION_DESTROY_INTENSITY = builder
            .comment("拆家强度（0.0-1.0）：越高拆得越勤，0 表示不拆")
            .translation("createlaborrush.config.destroyIntensity")
            .defineInRange("destroyIntensity", 0.5, 0.0, 1.0);

        builder.pop();
        SPEC = builder.build();
    }

    /**
     * 全量配置值快照，用于客户端与服务端之间同步。
     */
    public record Values(
        double destroyChance, double destroyRatioMin, double destroyRatioMax,
        boolean rebellionEnabled, int rebellionTriggerTime, double rebellionChance,
        int rebellionDuration, int rebellionRadius, boolean canDestroyDevices,
        int destroyCooldown, double destroyIntensity
    ) {}

    /** 读取当前全部配置值 */
    public static Values current() {
        return new Values(
            DESTROY_CHANCE.get(), DESTROY_RATIO_MIN.get(), DESTROY_RATIO_MAX.get(),
            REBELLION_ENABLED.get(), REBELLION_TRIGGER_TIME.get(), REBELLION_CHANCE.get(),
            REBELLION_DURATION.get(), REBELLION_RADIUS.get(), REBELLION_CAN_DESTROY.get(),
            REBELLION_DESTROY_COOLDOWN.get(), REBELLION_DESTROY_INTENSITY.get()
        );
    }

    /**
     * 把快照写入内存（不落盘），并返回实际写入的值——数值会被夹到 defineInRange
     * 声明的合法区间内，避免网络包里传来的越界值污染配置文件。
     */
    public static Values applyInMemory(Values v) {
        Values c = new Values(
            clamp01(v.destroyChance()), clamp01(v.destroyRatioMin()), clamp01(v.destroyRatioMax()),
            v.rebellionEnabled(), clamp(v.rebellionTriggerTime(), 10, 3600), clamp01(v.rebellionChance()),
            clamp(v.rebellionDuration(), 5, 600), clamp(v.rebellionRadius(), 1, 32), v.canDestroyDevices(),
            clamp(v.destroyCooldown(), 1, 60), clamp01(v.destroyIntensity())
        );
        DESTROY_CHANCE.set(c.destroyChance());
        DESTROY_RATIO_MIN.set(c.destroyRatioMin());
        DESTROY_RATIO_MAX.set(c.destroyRatioMax());
        REBELLION_ENABLED.set(c.rebellionEnabled());
        REBELLION_TRIGGER_TIME.set(c.rebellionTriggerTime());
        REBELLION_CHANCE.set(c.rebellionChance());
        REBELLION_DURATION.set(c.rebellionDuration());
        REBELLION_RADIUS.set(c.rebellionRadius());
        REBELLION_CAN_DESTROY.set(c.canDestroyDevices());
        REBELLION_DESTROY_COOLDOWN.set(c.destroyCooldown());
        REBELLION_DESTROY_INTENSITY.set(c.destroyIntensity());
        return c;
    }

    /** 把内存中的配置写回配置文件 */
    public static void save() {
        SPEC.save();
    }

    private static double clamp01(double value) {
        return Double.isFinite(value) ? Math.max(0.0, Math.min(1.0, value)) : 0.0;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}