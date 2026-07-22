package com.xiaoou.rush;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Mod configuration. Loaded from the common config file.
 */
public class Config {
    static final ModConfigSpec SPEC;
    
    /** Probability (0.0–1.0) that an item is destroyed instead of produced. */
    public static final ModConfigSpec.DoubleValue DESTROY_CHANCE;
    
    /** 触发销毁时，销毁比例的下限 (0.0–1.0) */
    public static final ModConfigSpec.DoubleValue DESTROY_RATIO_MIN;
    
    /** 触发销毁时，销毁比例的上限 (0.0–1.0) */
    public static final ModConfigSpec.DoubleValue DESTROY_RATIO_MAX;

    static {
        var builder = new ModConfigSpec.Builder();
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