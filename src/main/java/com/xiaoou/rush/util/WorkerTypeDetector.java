package com.xiaoou.rush.util;

import com.xiaoou.rush.CreateLaborRush;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 工人类型检测工具 - 通过反射支持软前置模组（女仆、千年村庄等）
 * 所有反射代码包在 try-catch 中，类不存在时静默降级
 */
public class WorkerTypeDetector {

    // 女仆实体类路径
    private static final String MAID_CLASS = "com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid";
    // 千年村庄村民类路径（仅 NeoForge 1.21.1）
    private static final String MILL_VILLAGER_CLASS = "org.dizzymii.millenaire2.entity.MillVillager";

    private static Class<?> maidClass = null;
    private static Class<?> millVillagerClass = null;

    static {
        initReflection();
    }

    private static void initReflection() {
        try {
            maidClass = Class.forName(MAID_CLASS);
            CreateLaborRush.LOGGER.info("WorkerTypeDetector: 检测到女仆模组 (Touhou Little Maid)");
        } catch (ClassNotFoundException e) {
            CreateLaborRush.LOGGER.debug("WorkerTypeDetector: 未检测到女仆模组，女仆功能已降级");
        }

        try {
            millVillagerClass = Class.forName(MILL_VILLAGER_CLASS);
            CreateLaborRush.LOGGER.info("WorkerTypeDetector: 检测到千年村庄模组 (Millénaire)");
        } catch (ClassNotFoundException e) {
            CreateLaborRush.LOGGER.debug("WorkerTypeDetector: 未检测到千年村庄模组");
        }
    }

    /**
     * 判断实体是否为受支持的工人类型
     */
    public static boolean isWorker(LivingEntity entity) {
        // 原版村民
        if (entity instanceof Villager) return true;
        // 玩家
        if (entity instanceof Player) return true;
        // 反射检测女仆
        if (isMaid(entity)) return true;
        // 反射检测千年村庄村民
        if (isMillVillager(entity)) return true;
        return false;
    }

    /**
     * 判断是否为女仆（反射）
     */
    public static boolean isMaid(LivingEntity entity) {
        if (maidClass == null) return false;
        return maidClass.isInstance(entity);
    }

    /**
     * 判断是否为千年村庄村民（反射）
     */
    public static boolean isMillVillager(LivingEntity entity) {
        if (millVillagerClass == null) return false;
        return millVillagerClass.isInstance(entity);
    }

    /**
     * 获取女仆的主人（反射）
     */
    public static LivingEntity getMaidOwner(LivingEntity maid) {
        if (maidClass == null || !maidClass.isInstance(maid)) return null;
        try {
            return (LivingEntity) maidClass.getMethod("getOwner").invoke(maid);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 反射调用女仆的 chatBubble 显示文字
     */
    public static void showMaidChatBubble(LivingEntity maid, String text) {
        if (maidClass == null || !maidClass.isInstance(maid)) return;
        try {
            Object chatBubbleManager = maidClass.getMethod("getChatBubbleManager").invoke(maid);
            if (chatBubbleManager != null) {
                chatBubbleManager.getClass().getMethod("addTextChatBubble", String.class)
                    .invoke(chatBubbleManager, text);
            }
        } catch (Exception e) {
            // 反射失败，使用头顶文字作为降级方案
            try {
                var newName = net.minecraft.network.chat.Component.literal(text);
                maid.setCustomName(newName);
                maid.setCustomNameVisible(true);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 女仆背包搜索并装备武器（反射调用）
     * @param level 世界
     * @param maid 女仆实体
     * @param targetPlayer 目标玩家
     */
    public static void maidArmAndAttack(Level level, LivingEntity maid, Player targetPlayer) {
        if (maidClass == null || !maidClass.isInstance(maid)) return;

        try {
            // 1. 获取背包
            Object availableInv = maidClass.getMethod("getAvailableInv", boolean.class).invoke(maid, false);
            var invClass = availableInv.getClass();

            // 搜索武器 - 按优先级
            net.minecraft.world.item.ItemStack weapon = findBestWeapon(availableInv, invClass);

            // 2. 装备武器到主手
            if (weapon != null && !weapon.isEmpty()) {
                maidClass.getMethod("setItemSlot",
                    net.minecraft.world.entity.EquipmentSlot.class,
                    net.minecraft.world.item.ItemStack.class)
                    .invoke(maid,
                        net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                        weapon);
            }

            // 3. 设置攻击目标
            maidClass.getMethod("setTarget", LivingEntity.class).invoke(maid, targetPlayer);

            // 4. 触发攻击
            maidClass.getMethod("doHurtTarget", net.minecraft.world.entity.Entity.class).invoke(maid, targetPlayer);

        } catch (Exception e) {
            CreateLaborRush.LOGGER.debug("WorkerTypeDetector: 女仆武装起义反射调用失败", e);
        }
    }

    /**
     * 搜索背包中的最佳武器（按优先级：枪械 > 远程 > 近战 > 空手）
     */
    private static net.minecraft.world.item.ItemStack findBestWeapon(Object inv, Class<?> invClass) {
        int slots = 0;
        try {
            slots = (int) invClass.getMethod("getSlots").invoke(inv);
        } catch (Exception e) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }

        net.minecraft.world.item.ItemStack bestWeapon = net.minecraft.world.item.ItemStack.EMPTY;
        double bestDamage = 0;
        int bestDurability = 0;

        for (int i = 0; i < slots; i++) {
            try {
                net.minecraft.world.item.ItemStack stack = (net.minecraft.world.item.ItemStack)
                    invClass.getMethod("getStackInSlot", int.class).invoke(inv, i);
                if (stack.isEmpty()) continue;

                net.minecraft.world.item.Item item = stack.getItem();

                // 第一优先级：剑（近战武器中最实用）
                if (item instanceof net.minecraft.world.item.SwordItem sword) {
                    double dmg = sword.getDamage(stack);
                    int dur = stack.getMaxDamage() - stack.getDamageValue();
                    if (dmg > bestDamage || (dmg == bestDamage && dur > bestDurability)) {
                        bestWeapon = stack;
                        bestDamage = dmg;
                        bestDurability = dur;
                    }
                }
                // 第一优先级：斧（也很强）
                else if (item instanceof net.minecraft.world.item.AxeItem axe) {
                    // 斧的攻击力通常比剑高，但速度慢，所以适当降低权重
                    double dmg = 7.0 * 0.9; // 默认斧头攻击力，1.21.1 通过属性修饰符获取
                    int dur = stack.getMaxDamage() - stack.getDamageValue();
                    if (dmg > bestDamage || (dmg == bestDamage && dur > bestDurability)) {
                        bestWeapon = stack;
                        bestDamage = dmg;
                        bestDurability = dur;
                    }
                }
                // 第二优先级：弓/弩（远程）
                else if (item instanceof net.minecraft.world.item.BowItem ||
                         item instanceof net.minecraft.world.item.CrossbowItem ||
                         item instanceof net.minecraft.world.item.TridentItem) {
                    double dmg = 8.0; // 远程武器默认伤害
                    int dur = stack.getMaxDamage() - stack.getDamageValue();
                    if (dmg > bestDamage || (dmg == bestDamage && dur > bestDurability)) {
                        bestWeapon = stack;
                        bestDamage = dmg;
                        bestDurability = dur;
                    }
                }
            } catch (Exception e) {
                // 跳过这个槽位
            }
        }

        return bestWeapon;
    }
}