package com.xiaoou.rush.compat;

import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.common.GunCommonUtil;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.implement.TextChatBubbleData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskAttack;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/**
 * 真正持有 TLM 类型引用的隔离实现类。
 * <p>
 * 本类只允许在 {@link MaidCompat} 确认 TLM 已加载之后被触碰。类加载是懒解析的，
 * 只要 TLM 未安装时不会走到这里，就不会触发 TLM 类的解析，也就不会崩。
 */
final class MaidCompatImpl {

    private MaidCompatImpl() {
    }

    static boolean isMaid(LivingEntity entity) {
        return entity instanceof EntityMaid;
    }

    static LivingEntity getOwner(LivingEntity maid) {
        if (maid instanceof EntityMaid entityMaid) {
            return entityMaid.getOwner();
        }
        return null;
    }

    static IItemHandler getAvailableInv(LivingEntity maid) {
        if (maid instanceof EntityMaid entityMaid) {
            // getAvailableInv(false) 返回 CombinedInvWrapper（实现 IItemHandler）
            return entityMaid.getAvailableInv(false);
        }
        return null;
    }

    static Object getCurrentTask(LivingEntity maid) {
        if (maid instanceof EntityMaid entityMaid) {
            return entityMaid.getTask();
        }
        return null;
    }

    static Object getIdleTask() {
        return TaskManager.getIdleTask();
    }

    static Object getAttackTask() {
        // 直接读取 TaskAttack.UID 查任务（比硬编码 "attack" id 更稳）
        return TaskManager.findTask(TaskAttack.UID).orElse(null);
    }

    static void setTask(LivingEntity maid, Object task) {
        if (maid instanceof EntityMaid entityMaid && task instanceof IMaidTask maidTask) {
            entityMaid.setTask(maidTask);
        }
    }

    static void refreshBrain(LivingEntity maid, ServerLevel level) {
        if (maid instanceof EntityMaid entityMaid && level != null) {
            entityMaid.refreshBrain(level);
        }
    }

    static void showTextBubble(LivingEntity maid, String text) {
        if (!(maid instanceof EntityMaid entityMaid)) {
            return;
        }
        // 1.5.x 官方气泡 API：TextChatBubbleData 在内部处理 iconPath 与序列化，
        // 不再需要旧版 ChatText 的 EMPTY_ICON_PATH 兼容处理（旧做法 iconPath 为 null 会导致玩家掉线）
        entityMaid.getChatBubbleManager().addChatBubble(TextChatBubbleData.type2(Component.literal(text)));
    }

    static boolean isGunItem(ItemStack stack) {
        // GunCommonUtil 是 TLM 自带的兼容类（随 TLM 一起存在），其 isGun 内部通过
        // TacCompat/SWarfareCompat 的 INSTALLED 标记判断；未安装 TACZ/SWarfare 时安全返回 false，
        // 不会触碰枪械模组的类，因此可以安全地直接调用（无需保留反射）。
        return GunCommonUtil.isGun(stack);
    }
}
