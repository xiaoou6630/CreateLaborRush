package com.xiaoou.rush;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import com.xiaoou.rush.network.BellSyncPayload;
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
        modEventBus.addListener(this::registerPayloads);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(BellSyncPayload.TYPE, BellSyncPayload.STREAM_CODEC, BellSyncPayload::handle);
    }
}