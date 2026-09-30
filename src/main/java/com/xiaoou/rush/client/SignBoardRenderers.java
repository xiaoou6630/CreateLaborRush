package com.xiaoou.rush.client;

import com.xiaoou.rush.CreateLaborRush;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 给玩家和村民挂上「牌子」渲染层。
 *
 * <p>用 {@link EntityRenderersEvent.AddLayers}（模组总线）而不是在实体注册时插队，
 * 是因为玩家渲染器走的是 skinMap（default / slim 两套），只有这个事件能拿到。
 */
// 【已停用】挂牌渲染层暂时不接入游戏，入口注解已摘掉（见 handler.HangingSignHandler 的说明）。
// @Mod.EventBusSubscriber(modid = CreateLaborRush.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class SignBoardRenderers {

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        // 玩家：default（经典）和 slim（纤细）两套模型都要挂
        for (String skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new SignBoardLayer<>(renderer, SignBoardLayer::playerLines));
            }
        }
        VillagerRenderer villagerRenderer = event.getRenderer(EntityType.VILLAGER);
        if (villagerRenderer != null) {
            villagerRenderer.addLayer(new SignBoardLayer<>(villagerRenderer, SignBoardLayer::villagerLines));
        }
    }
}
