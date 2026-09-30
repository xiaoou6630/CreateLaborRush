package com.xiaoou.rush;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import com.xiaoou.rush.network.BellSyncPayload;
import com.xiaoou.rush.network.ConfigSyncPayload;
import com.xiaoou.rush.network.HangingSignEditPayload;
import com.xiaoou.rush.network.SignBoardSyncPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(CreateLaborRush.MODID)
public class CreateLaborRush {
    public static final String MODID = "createlaborrush";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateLaborRush(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ModEffects.EFFECTS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModArmorMaterials.ARMOR_MATERIALS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        modEventBus.addListener(this::registerPayloads);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(BellSyncPayload.TYPE, BellSyncPayload.STREAM_CODEC, BellSyncPayload::handle);
        // 只有"客户端 → 服务端"一个方向：服务端不会把配置值下发给客户端（那会污染客户端的 toml）
        registrar.playToServer(ConfigSyncPayload.TYPE, ConfigSyncPayload.STREAM_CODEC, ConfigSyncPayload::handleToServer);
        // 挂牌写字界面在客户端自己弹，写完把 4 行文字回传服务端落盘
        registrar.playToServer(HangingSignEditPayload.TYPE, HangingSignEditPayload.STREAM_CODEC,
            HangingSignEditPayload::handleToServer);
        // 村民身上的挂牌存在持久化数据里，不会自动同步，得自己推给客户端渲染层
        registrar.playToClient(SignBoardSyncPayload.TYPE, SignBoardSyncPayload.STREAM_CODEC,
            SignBoardSyncPayload::handle);
    }
}