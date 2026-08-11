package com.xiaoou.rush.handler;

import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.util.ClientBellCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

/**
 * 创造模式中键拾取（官方 API 方案，替代 PickItemClientMixin）：
 * 原版钟/呼唤铃无 BlockEntity，中键只会复制到普通钟。
 * 监听 InputEvent.InteractionKeyMappingTriggered（中键），命中已放置的附魔钟时
 * 直接给出还原附魔的物品并取消原版拾取流程。
 */
@EventBusSubscriber(modid = CreateLaborRush.MODID, value = Dist.CLIENT)
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
            var fire = player.level().holder(Enchantments.FIRE_ASPECT).orElse(null);
            if (placed[0] > 0 && fire != null) bell.enchant(fire, placed[0]);
            var channeling = player.level().holder(Enchantments.CHANNELING).orElse(null);
            if (placed[1] > 0 && channeling != null) bell.enchant(channeling, placed[1]);
            if (bell.getEnchantments().isEmpty()) return;

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
