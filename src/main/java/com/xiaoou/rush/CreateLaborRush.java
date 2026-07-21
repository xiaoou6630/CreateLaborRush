package com.xiaoou.rush;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

/**
 * Main entry point for Create: Labor Rush.
 *
 * <p>This is an addon for <b>Create: Villager Labor</b> that speeds up
 * seated workers by applying the "Work!" effect via a Lead or Bell.
 * All processing acceleration is handled through Mixins targeting
 * {@code WorkerSeatBlockEntity} — no source code changes to the base mod.</p>
 *
 * <p>On mod construction, this class registers the common config
 * ({@link Config}) and the custom mob effect ({@link ModEffects}).</p>
 */
@Mod(CreateLaborRush.MODID)
public class CreateLaborRush {
    public static final String MODID = "createlaborrush";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateLaborRush(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ModEffects.EFFECTS.register(modEventBus);
    }
}