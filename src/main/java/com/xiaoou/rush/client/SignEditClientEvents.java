package com.xiaoou.rush.client;

import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.network.SignTextPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端侧：手持悬挂告示牌、不潜行、右键空气 → 打开写字界面。
 *
 * <p>潜行那一档（把牌子换到胸甲槽）由服务端的 {@code HangingSignHandler} 处理，
 * 这里直接放行，好让 Serverbound 包照常发上去。
 */
// 【已停用】挂牌功能暂时不接入游戏，入口注解已摘掉（见 handler.HangingSignHandler 的说明）。
// @Mod.EventBusSubscriber(modid = CreateLaborRush.MODID, value = Dist.CLIENT)
public class SignEditClientEvents {

    private static final double REACH = 20.0D;

    @SubscribeEvent
    public static void onRightClickAir(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.isShiftKeyDown()) return;
        if (!event.getItemStack().is(ItemTags.HANGING_SIGNS)) return;
        if (player.pick(REACH, 0.0F, false).getType() != HitResult.Type.MISS) return;

        // 用原版告示牌编辑界面写字：先造一块灌好现有文字的离屏假 BE，再交给界面编辑
        Minecraft.getInstance().setScreen(new HangingSignEditScreen(
            HangingSignEditScreen.createDetachedSign(event.getItemStack()), SignTextPacket.Target.MAIN_HAND));
        event.setCanceled(true);
    }
}
