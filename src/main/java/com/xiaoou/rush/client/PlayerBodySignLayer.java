package com.xiaoou.rush.client;

import com.xiaoou.rush.util.HangingSignData;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * 玩家胸前的挂牌：文字来自胸甲栏里那件悬挂告示牌（潜行抬牌子就是把它穿到胸甲栏）。
 */
public class PlayerBodySignLayer extends BodySignLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    /**
     * 板子中心偏移（格）。玩家躯干是 8x12x4、正面在 z=-2。
     * 【需实机微调】
     */
    private static final float BOARD_Y = 0.375F;
    private static final float BOARD_Z = -0.16F;

    public PlayerBodySignLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent, BOARD_Y, BOARD_Z);
    }

    @Override
    protected String[] getWornLines(AbstractClientPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return chest.is(ItemTags.HANGING_SIGNS) ? HangingSignData.readLines(chest) : null;
    }
}
