package com.xiaoou.rush;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

@Mod(CreateLaborRush.MODID)
public class CreateLaborRush {
    public static final String MODID = "createlaborrush";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateLaborRush(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ModEffects.EFFECTS.register(modEventBus);
        // ✅ 只注册效果，不注册附魔（附魔留给 1.1.0）
    }
}