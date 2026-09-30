package com.xiaoou.rush.util;

import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.compat.MaidCompat;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;

/**
 * 工人类型检测工具 - 通过软依赖支持女仆、千年村庄等模组。
 * <p>
 * 女仆(TLM)部分已改为直接 API 调用，并通过 {@link MaidCompat} 做类加载隔离
 * （本文件不引用任何 TLM 类型）；千年村庄仍使用反射，不在本次改造范围内。
 */
public class WorkerTypeDetector {

    // 千年村庄村民类路径（仅 NeoForge 1.21.1）
    private static final String MILL_VILLAGER_CLASS = "org.dizzymii.millenaire2.entity.MillVillager";

    private static Class<?> millVillagerClass = null;

    static {
        initReflection();
    }

    private static void initReflection() {
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
        // 女仆（TLM 软依赖，直接 API 检测）
        if (isMaid(entity)) return true;
        // 千年村庄村民（反射）
        if (isMillVillager(entity)) return true;
        return false;
    }

    /**
     * 判断是否为女仆（直接 API 检测，未装 TLM 时恒为 false）
     */
    public static boolean isMaid(LivingEntity entity) {
        return MaidCompat.isMaid(entity);
    }

    /**
     * 判断是否为千年村庄村民（反射）
     */
    public static boolean isMillVillager(LivingEntity entity) {
        if (millVillagerClass == null) return false;
        return millVillagerClass.isInstance(entity);
    }

    /**
     * 获取女仆的主人（直接 API）
     */
    public static LivingEntity getMaidOwner(LivingEntity maid) {
        return MaidCompat.getOwner(maid);
    }

    /**
     * 通过女仆官方气泡 API 显示文字；失败时降级为头顶文字（保持原有降级行为）
     */
    public static void showMaidChatBubble(LivingEntity maid, String text) {
        if (!MaidCompat.isMaid(maid)) return;
        if (MaidCompat.showTextBubble(maid, text)) {
            return;
        }
        // 气泡失败，使用头顶文字作为降级方案
        try {
            maid.setCustomName(Component.literal(text));
            maid.setCustomNameVisible(true);
        } catch (Exception ignored) {
        }
    }

    /**
     * 女仆背包搜索并装备武器（直接 API）
     *
     * @param level        世界
     * @param maid         女仆实体
     * @param targetPlayer 目标玩家
     */
    public static void maidArmAndAttack(Level level, LivingEntity maid, Player targetPlayer) {
        if (!MaidCompat.isMaid(maid)) return;

        try {
            // 1. 获取背包
            IItemHandler inv = MaidCompat.getAvailableInv(maid);
            if (inv == null) return;

            // 搜索武器 - 按优先级
            ItemStack weapon = findBestWeapon(inv);

            // 2. 装备武器到主手
            if (!weapon.isEmpty()) {
                maid.setItemSlot(EquipmentSlot.MAINHAND, weapon);
            }

            Mob mob = (Mob) maid;
            // 3. 设置攻击目标
            mob.setTarget(targetPlayer);

            // 4. 触发攻击
            mob.doHurtTarget(targetPlayer);
        } catch (Exception e) {
            CreateLaborRush.LOGGER.debug("WorkerTypeDetector: 女仆武装起义调用失败", e);
        }
    }

    /**
     * 搜索背包中的最佳武器（按优先级：剑/斧 > 弓/弩/三叉戟 > 空手）
     */
    private static ItemStack findBestWeapon(IItemHandler inv) {
        ItemStack bestWeapon = ItemStack.EMPTY;
        double bestDamage = 0;
        int bestDurability = 0;

        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            Item item = stack.getItem();

            // 第一优先级：剑（近战武器中最实用）
            if (item instanceof SwordItem sword) {
                double dmg = sword.getDamage();
                int dur = stack.getMaxDamage() - stack.getDamageValue();
                if (dmg > bestDamage || (dmg == bestDamage && dur > bestDurability)) {
                    bestWeapon = stack;
                    bestDamage = dmg;
                    bestDurability = dur;
                }
            }
            // 第一优先级：斧（也很强）
            else if (item instanceof AxeItem axe) {
                // 斧的攻击力通常比剑高，但速度慢，所以适当降低权重
                double dmg = axe.getAttackDamage() * 0.9;
                int dur = stack.getMaxDamage() - stack.getDamageValue();
                if (dmg > bestDamage || (dmg == bestDamage && dur > bestDurability)) {
                    bestWeapon = stack;
                    bestDamage = dmg;
                    bestDurability = dur;
                }
            }
            // 第二优先级：弓/弩/三叉戟（远程）
            else if (item instanceof BowItem ||
                     item instanceof CrossbowItem ||
                     item instanceof TridentItem) {
                double dmg = 8.0; // 远程武器默认伤害
                int dur = stack.getMaxDamage() - stack.getDamageValue();
                if (dmg > bestDamage || (dmg == bestDamage && dur > bestDurability)) {
                    bestWeapon = stack;
                    bestDamage = dmg;
                    bestDurability = dur;
                }
            }
        }

        return bestWeapon;
    }
}
