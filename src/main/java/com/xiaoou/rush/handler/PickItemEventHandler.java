package com.xiaoou.rush.handler;

import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.util.ClientBellCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 创造模式中键拾取（官方 API 方案，替代 PickItemClientMixin）：
 * 原版钟/呼唤铃无 BlockEntity，中键只会复制到普通钟。
 * 监听 InputEvent.InteractionKeyMappingTriggered（中键），命中已放置的附魔钟时
 * 直接给出还原附魔的物品并取消原版拾取流程。
 */
@Mod.EventBusSubscriber(modid = CreateLaborRush.MODID, value = Dist.CLIENT)
public class PickItemEventHandler {

    @SubscribeEvent
    public static void onPickBlock(InputEvent.InteractionKeyMappingTriggered event) {
        try {
            if (!event.isPickBlock()) return;

            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null || !player.getAbilities().instabuild) return;
            if (!(mc.hitResult instanceof BlockHitResult blockHit)) return;
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = mc.level.getBlockState(pos);
            if (!BellEnchantHandler.isBellBlock(state.getBlock())) return;

            int[] placed = ClientBellCache.get(BellEnchantHandler.keyOf(player.level().dimension(), pos));
            if (placed == null || (placed[0] <= 0 && placed[1] <= 0)) return;

            ItemStack bell = new ItemStack(state.getBlock().asItem());
            if (placed[0] > 0) bell.enchant(Enchantments.FIRE_ASPECT, placed[0]);
            if (placed[1] > 0) bell.enchant(Enchantments.CHANNELING, placed[1]);
            if (EnchantmentHelper.getEnchantments(bell).isEmpty()) return;

            int index = player.getInventory().selected;
            player.getInventory().setItem(index, bell);
            mc.gameMode.handleCreativeModeItemAdd(bell, index);
            event.setCanceled(true);
            CreateLaborRush.LOGGER.info("Picked enchanted {} at {}: fireAspect={}, channeling={}",
                state.getBlock(), pos, placed[0], placed[1]);
        } catch (Exception e) {
            CreateLaborRush.LOGGER.warn("PickItemEventHandler failed", e);
        }
    }
}
