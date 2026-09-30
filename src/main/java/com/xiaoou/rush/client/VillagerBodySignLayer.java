package com.xiaoou.rush.client;

import com.xiaoou.rush.util.ClientSignBoardCache;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.npc.Villager;

/**
 * 村民胸前的挂牌：文字来自 {@link ClientSignBoardCache}。
 *
 * <p>服务端的村民把牌子存在持久化数据里，而 NeoForge 不会同步 {@code getPersistentData()}，
 * 所以服务端会在挂上 / 摘下 / 玩家开始追踪时把文字推过来（见 {@code SignBoardSyncPayload}），
 * 客户端只认这份缓存。
 */
public class VillagerBodySignLayer extends BodySignLayer<Villager, VillagerModel<Villager>> {

    /**
     * 板子中心偏移（格）。村民躯干是 8x12x6、正面在 z=-3，比玩家厚一点。
     *
     * <p>村民双臂是抱在胸前的，胳膊会伸到 z≈-0.35 格，牌子必须比它还靠前才不会被手挡住。
     * 【需实机微调】
     */
    private static final float BOARD_Y = 0.375F;
    private static final float BOARD_Z = -0.5F;

    public VillagerBodySignLayer(RenderLayerParent<Villager, VillagerModel<Villager>> parent) {
        super(parent, BOARD_Y, BOARD_Z);
    }

    @Override
    protected String[] getWornLines(Villager villager) {
        return ClientSignBoardCache.get(villager.getId());
    }
}
