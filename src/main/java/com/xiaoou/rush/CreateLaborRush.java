package com.xiaoou.rush;

import com.mojang.logging.LogUtils;
import com.xiaoou.rush.client.RushConfigScreen;
import com.xiaoou.rush.network.LaborNetworking;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.ModLoadingContext;
import org.slf4j.Logger;

@Mod(CreateLaborRush.MODID)
public class CreateLaborRush {
    public static final String MODID = "createlaborrush";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateLaborRush() {
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ModEffects.EFFECTS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModPaintings.PAINTING_VARIANTS.register(modEventBus);
        modEventBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(LaborNetworking::register));

        // 把配置界面挂到"模组列表 → Config"按钮上（仅客户端）
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new RushConfigScreen(parent))
            );
        }
    }
}