package com.xiaoou.rush.client;

import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 把「挂牌」渲染层挂到原版渲染器上（NeoForge 的 {@link EntityRenderersEvent.AddLayers}）。
 *
 * <p>玩家和村民的原版渲染器都是 {@code LivingEntityRenderer}，直接 {@code addLayer} 即可。
 * 玩家两种皮肤模型（{@link PlayerSkin.Model#WIDE} 即 {@code "default"}、
 * {@link PlayerSkin.Model#SLIM} 即 {@code "slim"}）共用一个自带 {@code PlayerRenderer}，
 * 所以要各挂一层。
 */
public final class BodySignRenderers {

    private BodySignRenderers() {
    }

    /** 由 {@link CreateLaborRushClient} 注册到 mod 事件总线（AddLayers 属于 mod 总线事件） */
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        PlayerRenderer wide = event.getSkin(PlayerSkin.Model.WIDE);
        if (wide != null) {
            wide.addLayer(new PlayerBodySignLayer(wide));
        }
        PlayerRenderer slim = event.getSkin(PlayerSkin.Model.SLIM);
        if (slim != null) {
            slim.addLayer(new PlayerBodySignLayer(slim));
        }

        VillagerRenderer villagerRenderer = event.getRenderer(EntityType.VILLAGER);
        if (villagerRenderer != null) {
            villagerRenderer.addLayer(new VillagerBodySignLayer(villagerRenderer));
        }
    }
}
