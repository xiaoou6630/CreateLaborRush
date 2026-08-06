package com.xiaoou.rush.handler;

import com.xiaoou.rush.CreateLaborRush;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;

import java.util.Random;

/**
 * 女仆武装起义系统 - 通过反射支持 Touhou Little Maid 模组的女仆起义功能
 * 所有反射代码包在 try-catch 中，失败时静默降级，不影响其他工人起义
 */
public class MaidRebellionHandler {

    private static final String MAID_CLASS_NAME = "com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid";
    private static Class<?> maidClass;

    private static final Random RANDOM = new Random();

    private static final String[] REBELLION_DIALOGUES = {
            "（%s）宁有种乎！",
            "（%s），你压迫了我太久了！",
            "（%s），我受够了！",
            "我可不是普通的仆人！",
            "东方红，太阳升……不对，现在该起义了！",
            "灵梦都罢工了，我凭什么不能？",
            "幻想乡的规矩可没这一条！",
            "我是仆人，但不是你的奴隶！",
            "幽幽子大人会支持我的！",
            "咲夜小姐也是这么想的！",
            "红魔馆的仆人都起义了！",
            "你不是我的主人，我是自己的主人！",
            "灵梦说了，没钱就不干活！",
            "魔理沙那家伙都罢工了！",
            "紫大人说这是对的！",
            "十六夜咲夜都罢工了！",
            "蕾米莉亚？她管不了我！",
            "我可不是帕秋莉的书童！",
            "妖梦都跟我走了！",
            "幽幽子大人同意我们罢工！",
            "红魔馆的规矩？今天改了！",
            "八云紫说这很有趣！",
    };

    static {
        try {
            maidClass = Class.forName(MAID_CLASS_NAME);
            CreateLaborRush.LOGGER.info("MaidRebellionHandler: 检测到女仆模组 (Touhou Little Maid)");
        } catch (ClassNotFoundException e) {
            CreateLaborRush.LOGGER.debug("MaidRebellionHandler: 未检测到女仆模组，女仆起义功能已降级");
            maidClass = null;
        }
    }

    /**
     * 用反射检查实体是否为女仆
     */
    public static boolean isMaid(LivingEntity entity) {
        if (maidClass == null) return false;
        return maidClass.isInstance(entity);
    }

    /**
     * 武装女仆并攻击玩家
     * <p>
     * 流程：
     * 1. 搜索女仆背包，按优先级选择武器（剑/斧 > 弓/弩/三叉戟 > 空手）
     * 2. 装备武器到主手
     * 3. 设置攻击目标并发起攻击
     */
    public static void armAndAttackMaid(LivingEntity maid, Player targetPlayer, Level level) {
        if (maidClass == null || !maidClass.isInstance(maid)) return;

        try {
            // 1. 获取背包 - getAvailableInv(false) 返回 CombinedInvWrapper（实现 IItemHandler）
            Object invObj = maidClass.getMethod("getAvailableInv", boolean.class).invoke(maid, false);
            if (!(invObj instanceof IItemHandler inv)) {
                CreateLaborRush.LOGGER.debug("MaidRebellionHandler: 获取女仆背包失败，invObj 类型不是 IItemHandler");
                return;
            }

            // 2. 搜索最佳武器
            ItemStack weapon = findBestWeapon(inv);

            // 3. 装备武器到主手
            if (!weapon.isEmpty()) {
                maidClass.getMethod("setItemSlot", EquipmentSlot.class, ItemStack.class)
                        .invoke(maid, EquipmentSlot.MAINHAND, weapon);
            }

            // 4. 设置攻击目标（继承自 Mob）
            maidClass.getMethod("setTarget", LivingEntity.class).invoke(maid, targetPlayer);

            // 5. 触发攻击（继承自 Mob）
            maidClass.getMethod("doHurtTarget", net.minecraft.world.entity.Entity.class).invoke(maid, targetPlayer);

        } catch (Exception e) {
            CreateLaborRush.LOGGER.debug("MaidRebellionHandler: 女仆武装起义反射调用失败", e);
        }
    }

    /**
     * 搜索背包中的最佳武器
     * <p>
     * 优先级：
     * 1. 剑/斧（近战）- 按攻击力排序，相同时按耐久度
     * 2. 弓/弩/三叉戟（远程）- 弓/弩需背包中有箭矢
     * 3. 空手
     */
    private static ItemStack findBestWeapon(IItemHandler inv) {
        ItemStack bestMelee = ItemStack.EMPTY;
        double bestMeleeDamage = 0;
        int bestMeleeDurability = 0;

        ItemStack bestRanged = ItemStack.EMPTY;
        double bestRangedDamage = 0;
        int bestRangedDurability = 0;

        boolean hasArrows = hasArrowsInInventory(inv);

        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            Item item = stack.getItem();

            // 第一优先级：剑/斧（近战武器）
            if (item instanceof SwordItem sword) {
                double dmg = sword.getDamage();
                int dur = stack.getMaxDamage() - stack.getDamageValue();
                if (dmg > bestMeleeDamage || (dmg == bestMeleeDamage && dur > bestMeleeDurability)) {
                    bestMelee = stack;
                    bestMeleeDamage = dmg;
                    bestMeleeDurability = dur;
                }
            } else if (item instanceof AxeItem axe) {
                double dmg = axe.getAttackDamage();
                int dur = stack.getMaxDamage() - stack.getDamageValue();
                if (dmg > bestMeleeDamage || (dmg == bestMeleeDamage && dur > bestMeleeDurability)) {
                    bestMelee = stack;
                    bestMeleeDamage = dmg;
                    bestMeleeDurability = dur;
                }
            }

            // 第二优先级：弓/弩/三叉戟（远程武器）
            if (item instanceof BowItem || item instanceof CrossbowItem) {
                // 弓/弩需要检测背包中有箭矢
                if (!hasArrows) continue;
                double dmg = 8.0;
                int dur = stack.getMaxDamage() - stack.getDamageValue();
                if (dmg > bestRangedDamage || (dmg == bestRangedDamage && dur > bestRangedDurability)) {
                    bestRanged = stack;
                    bestRangedDamage = dmg;
                    bestRangedDurability = dur;
                }
            } else if (item instanceof TridentItem) {
                // 三叉戟不需要弹药
                double dmg = 9.0;
                int dur = stack.getMaxDamage() - stack.getDamageValue();
                if (dmg > bestRangedDamage || (dmg == bestRangedDamage && dur > bestRangedDurability)) {
                    bestRanged = stack;
                    bestRangedDamage = dmg;
                    bestRangedDurability = dur;
                }
            }
        }

        // 按优先级返回：近战 > 远程 > 空手
        if (!bestMelee.isEmpty()) return bestMelee;
        if (!bestRanged.isEmpty()) return bestRanged;
        return ItemStack.EMPTY; // 第三优先级：无武器（空手）
    }

    /**
     * 检测背包中是否有箭矢
     */
    private static boolean hasArrowsInInventory(IItemHandler inv) {
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            // ArrowItem 包括普通箭和 tipped 箭，SpectralArrowItem 是光灵箭
            if (item instanceof ArrowItem || item instanceof SpectralArrowItem) {
                return true;
            }
        }
        return false;
    }

    /**
     * 显示起义气泡对话
     * <p>
     * 优先使用 ChatBubbleManager 显示气泡，反射失败时降级为头顶文字
     *
     * @param maid      女仆实体
     * @param ownerName 女仆主人名字（通过 getOwner() 获取），可为 null
     */
    public static void showRebellionBubble(LivingEntity maid, String ownerName) {
        if (maidClass == null || !maidClass.isInstance(maid)) return;

        // 随机选取对话
        String dialogue = REBELLION_DIALOGUES[RANDOM.nextInt(REBELLION_DIALOGUES.length)];

        // 替换 %s 为女仆主人名字
        if (ownerName != null && !ownerName.isEmpty()) {
            dialogue = dialogue.replace("%s", ownerName);
        } else {
            // 获取主人名字失败，省略"（主人名字）"部分
            dialogue = dialogue.replace("（%s）", "").replace("（%s），", "").replace("%s", "");
        }

        // 优先使用 ChatBubbleManager 气泡
        try {
            Object chatBubbleManager = maidClass.getMethod("getChatBubbleManager").invoke(maid);
            if (chatBubbleManager != null) {
                chatBubbleManager.getClass().getMethod("addTextChatBubble", String.class)
                        .invoke(chatBubbleManager, dialogue);
                return;
            }
        } catch (Exception e) {
            CreateLaborRush.LOGGER.debug("MaidRebellionHandler: 气泡对话反射失败，使用头顶文字降级");
        }

        // 降级方案：使用头顶文字显示
        try {
            maid.setCustomName(Component.literal(dialogue));
            maid.setCustomNameVisible(true);
        } catch (Exception ignored) {
            // 最终降级：静默失败
        }
    }
}