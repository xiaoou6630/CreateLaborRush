package com.xiaoou.rush.handler;

import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.compat.MaidCompat;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 女仆武装起义系统 - 通过 {@link MaidCompat} 直接调用 Touhou Little Maid 的 API（替代反射）。
 * <p>
 * 本文件不引用任何 TLM 类型；未安装 TLM 时由 MaidCompat 静默降级，不影响其他工人起义。
 */
public class MaidRebellionHandler {

    private static final Random RANDOM = new Random();

    private static final int DIALOGUES_COUNT = 22;

    // 记录起义开始时女仆的原任务（UUID -> IMaidTask，用 Object 保存以避免暴露 TLM 类型），结束后恢复
    private static final Map<UUID, Object> ORIGINAL_TASKS = new ConcurrentHashMap<>();

    /**
     * 检查实体是否为女仆（直接 API，未装 TLM 时恒为 false）
     */
    public static boolean isMaid(LivingEntity entity) {
        return MaidCompat.isMaid(entity);
    }

    /**
     * 武装女仆并攻击玩家
     * <p>
     * 流程：
     * 1. 搜索女仆背包，按优先级选择武器（枪械 > 剑/斧 > 弓/弩/三叉戟 > 空手）
     * 2. 装备武器到主手（女仆 AI 会根据主手武器自动切换近战/远程/枪械攻击模式）
     * 3. 设置攻击目标、写入脑记忆并发起攻击
     */
    public static void armAndAttackMaid(LivingEntity maid, Player targetPlayer, Level level) {
        if (!MaidCompat.isMaid(maid)) return;

        try {
            // 1. 获取背包 - getAvailableInv(false) 返回 CombinedInvWrapper（实现 IItemHandler）
            IItemHandler inv = MaidCompat.getAvailableInv(maid);
            if (inv == null) {
                CreateLaborRush.LOGGER.debug("MaidRebellionHandler: 获取女仆背包失败");
                return;
            }

            // 2. 搜索最佳武器
            ItemStack weapon = findBestWeapon(inv);

            // 3. 装备武器到主手（背包无武器时发一把铁剑——女仆攻击 AI 需要 hasAssaultWeapon 才启动）
            if (weapon.isEmpty()) {
                weapon = new ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
            }
            maid.setItemSlot(EquipmentSlot.MAINHAND, weapon);

            // 4. 记录原任务并切换到 attack 任务，激活女仆自身的攻击 AI（近战/远程/枪械追击）
            Object currentTask = MaidCompat.getCurrentTask(maid);
            if (currentTask != null) {
                ORIGINAL_TASKS.putIfAbsent(maid.getUUID(), currentTask);
            }
            Object attackTask = MaidCompat.getAttackTask();
            if (attackTask != null) {
                MaidCompat.setTask(maid, attackTask);
            }

            Mob mob = (Mob) maid;
            // 5. 设置攻击目标
            mob.setTarget(targetPlayer);

            // 6. 关键：把 ATTACK_TARGET 直接写入女仆脑记忆。
            // 女仆 TaskAttack 用 vanilla StartAttacking：只在脑记忆无目标时才自行寻找
            // （findFirstValidAttackTarget 会被 canAttack 限制，玩家常被排除），
            // 而 MaidMeleeAttack/SetWalkTargetFromAttackTarget 只读脑记忆。setTarget 不写脑记忆。
            mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, targetPlayer);

            // 7. 触发一次立即攻击
            mob.doHurtTarget(targetPlayer);

        } catch (Exception e) {
            CreateLaborRush.LOGGER.debug("MaidRebellionHandler: 女仆武装起义调用失败", e);
        }
    }

    /**
     * 强制女仆只打指定目标（起义中每 tick 调用）：
     * setTarget + 直接覆盖脑记忆 ATTACK_TARGET，防止女仆自己的 TaskAttack
     * 换成别的东西（打僵尸、帮主人打架、被其他生物打了反击）——专注打玩家。
     */
    public static void forceAttackTarget(LivingEntity maid, LivingEntity target) {
        if (!MaidCompat.isMaid(maid)) return;
        try {
            Mob mob = (Mob) maid;
            mob.setTarget(target);
            mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
        } catch (Exception e) {
            CreateLaborRush.LOGGER.debug("MaidRebellionHandler: 强制攻击目标失败", e);
        }
    }

    /**
     * 清空女仆的攻击目标（无玩家时每 tick 调用）：setTarget(null) + 擦除 ATTACK_TARGET 脑记忆，
     * 即使她被其他生物攻击也不反击，不打任何非玩家目标。
     */
    public static void clearAttackTarget(LivingEntity maid) {
        if (!MaidCompat.isMaid(maid)) return;
        try {
            Mob mob = (Mob) maid;
            mob.setTarget(null);
            mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        } catch (Exception e) {
            CreateLaborRush.LOGGER.debug("MaidRebellionHandler: 清空攻击目标失败", e);
        }
    }

    /**
     * 检测枪械（TACZ/SWarfare 软依赖，未安装时返回 false）
     */
    private static boolean isGunItem(ItemStack stack) {
        return MaidCompat.isGunItem(stack);
    }

    /**
     * 搜索背包中的最佳武器
     * <p>
     * 优先级：
     * 1. 枪械（TACZ/SWarfare）- 弹药无法检测时直接装备（当作有子弹）
     * 2. 剑/斧（近战）- 按攻击力排序，相同时按耐久度
     * 3. 弓/弩/三叉戟（远程）- 弓/弩需背包中有箭矢
     * 4. 空手
     */
    private static ItemStack findBestWeapon(IItemHandler inv) {
        ItemStack bestGun = ItemStack.EMPTY;

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

            // 第一优先级：枪械（TACZ / SWarfare），选择伤害最高的一把
            if (isGunItem(stack)) {
                // 枪械伤害无法直接读取，无法比较时取第一把；后续可比较枪械属性
                if (bestGun.isEmpty()) {
                    bestGun = stack;
                }
                continue;
            }

            // 第二优先级：剑/斧（近战武器）
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

            // 第三优先级：弓/弩/三叉戟（远程武器）
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

        // 按优先级返回：枪械 > 近战 > 远程 > 空手
        if (!bestGun.isEmpty()) return bestGun;
        if (!bestMelee.isEmpty()) return bestMelee;
        if (!bestRanged.isEmpty()) return bestRanged;
        return ItemStack.EMPTY; // 第四优先级：无武器（空手）
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
     * 起义结束后恢复女仆：切回起义前的任务（无记录则切回默认 idle），
     * 重建脑任务并清除攻击目标，让她回去干活
     */
    public static void restoreMaid(LivingEntity maid, ServerLevel level) {
        if (!MaidCompat.isMaid(maid)) return;

        try {
            Object originalTask = ORIGINAL_TASKS.remove(maid.getUUID());
            if (originalTask == null) {
                // 起义前没有记录到原任务时，切回默认 idle 任务
                originalTask = MaidCompat.getIdleTask();
            }
            if (originalTask != null) {
                MaidCompat.setTask(maid, originalTask);
                // 任务切换后必须重建脑任务，否则女仆 AI 还是旧的攻击任务
                if (level != null) {
                    MaidCompat.refreshBrain(maid, level);
                }
            }
            // 清除攻击目标 + 擦除 ATTACK_TARGET 脑记忆（防止恢复后还记着玩家目标）
            clearAttackTarget(maid);
        } catch (Exception e) {
            CreateLaborRush.LOGGER.debug("MaidRebellionHandler: 女仆任务恢复失败", e);
        }
    }

    /**
     * 显示起义气泡对话
     * <p>
     * 对话内容通过 lang 文件实现国际化。使用 TLM 官方气泡 API
     * （ChatBubbleManager + TextChatBubbleData），失败时只记录日志，不给女仆改名。
     *
     * @param maid      女仆实体
     * @param ownerName 女仆主人名字（通过 getOwner() 获取），可为 null
     */
    public static void showRebellionBubble(LivingEntity maid, String ownerName) {
        if (!MaidCompat.isMaid(maid)) return;

        // 随机选取对话（通过 lang 键，自动切换语言）
        String key = "chat.createlaborrush.maid.dialogue." + RANDOM.nextInt(DIALOGUES_COUNT);
        String dialogue = Component.translatable(key).getString();

        // 有主人名字时，用 lang 键获取本地化前缀
        if (ownerName != null && !ownerName.isEmpty()) {
            String prefix = Component.translatable("chat.createlaborrush.maid.owner_prefix", ownerName).getString();
            dialogue = prefix + dialogue;
        }

        if (!MaidCompat.showTextBubble(maid, dialogue)) {
            CreateLaborRush.LOGGER.info("MaidRebellionHandler: 女仆气泡显示失败（不做改名降级）");
        }
    }
}
