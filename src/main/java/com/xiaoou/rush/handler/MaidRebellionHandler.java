package com.xiaoou.rush.handler;

import com.xiaoou.rush.CreateLaborRush;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 女仆武装起义系统 - 通过反射支持 Touhou Little Maid 模组的女仆起义功能
 * 所有反射代码包在 try-catch 中，失败时静默降级，不影响其他工人起义
 */
public class MaidRebellionHandler {

    private static final String MAID_CLASS_NAME = "com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid";
    private static Class<?> maidClass;

    // 女仆枪械检测工具（TACZ/SWarfare 软依赖，未安装时 isGun 返回 false）
    private static final String GUN_UTIL_CLASS_NAME = "com.github.tartaricacid.touhoulittlemaid.compat.gun.common.GunCommonUtil";
    private static Class<?> gunUtilClass;

    // 女仆任务管理（用于把女仆切换到攻击任务，激活其攻击 AI）
    private static final String TASK_MANAGER_CLASS_NAME = "com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager";
    private static final String TASK_INTERFACE_CLASS_NAME = "com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask";
    private static Class<?> taskManagerClass;
    private static Class<?> taskInterfaceClass;

    // 女仆气泡（1.2.1 jar 实际 API：EntityMaid.addChatBubble(long, ChatText)，时间戳必须用 ChatBubbleManger.getEndTime()）
    private static final String CHAT_TEXT_CLASS_NAME = "com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.ChatText";
    private static final String CHAT_TEXT_TYPE_CLASS_NAME = "com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.ChatTextType";
    private static final String CHAT_BUBBLE_MANGER_CLASS_NAME = "com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.ChatBubbleManger";
    private static Class<?> chatTextClass;
    private static Class<?> chatTextTypeClass;
    private static Class<?> chatBubbleMangerClass;

    // 脑记忆操作缓存（起义期间每 tick 强制目标用，避免每 tick 反射查找开销）
    private static java.lang.reflect.Method getBrainMethod;
    private static java.lang.reflect.Method setMemoryMethod;
    private static java.lang.reflect.Method eraseMemoryMethod;
    private static Object attackTargetMemoryType;

    private static final Random RANDOM = new Random();

    // 记录起义开始时女仆的原任务（UUID -> IMaidTask），结束后恢复
    private static final Map<UUID, Object> ORIGINAL_TASKS = new ConcurrentHashMap<>();

    private static final int DIALOGUES_COUNT = 22;

    static {
        try {
            maidClass = Class.forName(MAID_CLASS_NAME);
            CreateLaborRush.LOGGER.info("MaidRebellionHandler: 检测到女仆模组 (Touhou Little Maid)");
        } catch (ClassNotFoundException e) {
            CreateLaborRush.LOGGER.debug("MaidRebellionHandler: 未检测到女仆模组，女仆起义功能已降级");
            maidClass = null;
        }

        try {
            gunUtilClass = Class.forName(GUN_UTIL_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            gunUtilClass = null;
        }

        try {
            taskManagerClass = Class.forName(TASK_MANAGER_CLASS_NAME);
            taskInterfaceClass = Class.forName(TASK_INTERFACE_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            taskManagerClass = null;
            taskInterfaceClass = null;
        }

        try {
            chatTextClass = Class.forName(CHAT_TEXT_CLASS_NAME);
            chatTextTypeClass = Class.forName(CHAT_TEXT_TYPE_CLASS_NAME);
            chatBubbleMangerClass = Class.forName(CHAT_BUBBLE_MANGER_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            chatTextClass = null;
            chatTextTypeClass = null;
            chatBubbleMangerClass = null;
        }

        if (maidClass != null) {
            try {
                getBrainMethod = maidClass.getMethod("getBrain");
                Class<?> memClass = Class.forName("net.minecraft.world.entity.ai.memory.MemoryModuleType");
                attackTargetMemoryType = memClass.getField("ATTACK_TARGET").get(null);
                Class<?> brainClass = getBrainMethod.getReturnType();
                setMemoryMethod = brainClass.getMethod("setMemory", memClass, Object.class);
                eraseMemoryMethod = brainClass.getMethod("eraseMemory", memClass);
            } catch (Exception e) {
                CreateLaborRush.LOGGER.debug("MaidRebellionHandler: 缓存脑记忆反射方法失败，目标强制功能降级", e);
                getBrainMethod = null;
                setMemoryMethod = null;
                eraseMemoryMethod = null;
                attackTargetMemoryType = null;
            }
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
     * 1. 搜索女仆背包，按优先级选择武器（枪械 > 剑/斧 > 弓/弩/三叉戟 > 空手）
     * 2. 装备武器到主手（女仆 AI 会根据主手武器自动切换近战/远程/枪械攻击模式）
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

            // 3. 装备武器到主手（背包无武器时发一把铁剑——女仆攻击 AI 需要 hasAssaultWeapon 才启动）
            if (weapon.isEmpty()) {
                weapon = new ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
            }
            maidClass.getMethod("setItemSlot", EquipmentSlot.class, ItemStack.class)
                    .invoke(maid, EquipmentSlot.MAINHAND, weapon);

            // 4. 把女仆任务切换为 attack，激活女仆自身的攻击 AI（近战/远程/枪械追击）
            // 直接反射 TaskAttack.UID 查任务（比硬编码 "attack" id 更稳）
            if (taskManagerClass != null && taskInterfaceClass != null) {
                ORIGINAL_TASKS.putIfAbsent(maid.getUUID(), getCurrentTask(maid));
                Object taskInstance = null;
                try {
                    Object uid = Class.forName("com.github.tartaricacid.touhoulittlemaid.entity.task.TaskAttack")
                            .getField("UID").get(null);
                    Object taskOpt = taskManagerClass.getMethod("findTask", ResourceLocation.class)
                            .invoke(null, uid);
                    if (taskOpt instanceof Optional<?> opt && opt.isPresent()) {
                        taskInstance = opt.get();
                    }
                } catch (Exception e) {
                    CreateLaborRush.LOGGER.debug("MaidRebellionHandler: 查找 attack 任务失败", e);
                }
                if (taskInstance != null) {
                    maidClass.getMethod("setTask", taskInterfaceClass).invoke(maid, taskInstance);
                }
            }

            // 5. 设置攻击目标（继承自 Mob）
            maidClass.getMethod("setTarget", LivingEntity.class).invoke(maid, targetPlayer);

            // 6. 关键：把 ATTACK_TARGET 直接写入女仆脑记忆。
            // 女仆 TaskAttack 用 vanilla StartAttacking：只在脑记忆无目标时才自行寻找
            // （findFirstValidAttackTarget 会被 canAttack 限制，玩家常被排除），
            // 而 MaidMeleeAttack/SetWalkTargetFromAttackTarget 只读脑记忆。setTarget 不写脑记忆。
            try {
                Object brain = maidClass.getMethod("getBrain").invoke(maid);
                Class<?> memClass = Class.forName("net.minecraft.world.entity.ai.memory.MemoryModuleType");
                Object attackTargetMem = memClass.getField("ATTACK_TARGET").get(null);
                brain.getClass().getMethod("setMemory", memClass, Object.class)
                        .invoke(brain, attackTargetMem, targetPlayer);
            } catch (Exception e) {
                CreateLaborRush.LOGGER.debug("MaidRebellionHandler: 写入 ATTACK_TARGET 脑记忆失败", e);
            }

            // 7. 触发一次立即攻击（继承自 Mob）
            maidClass.getMethod("doHurtTarget", net.minecraft.world.entity.Entity.class).invoke(maid, targetPlayer);

        } catch (Exception e) {
            CreateLaborRush.LOGGER.debug("MaidRebellionHandler: 女仆武装起义反射调用失败", e);
        }
    }

    /**
     * 强制女仆只打指定目标（起义中每 tick 调用）：
     * setTarget + 直接覆盖脑记忆 ATTACK_TARGET，防止女仆自己的 TaskAttack
     * 换成别的东西（打僵尸、帮主人打架、被其他生物打了反击）——专注打玩家。
     */
    public static void forceAttackTarget(LivingEntity maid, LivingEntity target) {
        if (maidClass == null || !maidClass.isInstance(maid)) return;
        try {
            maidClass.getMethod("setTarget", LivingEntity.class).invoke(maid, target);
            if (getBrainMethod != null && setMemoryMethod != null && attackTargetMemoryType != null) {
                Object brain = getBrainMethod.invoke(maid);
                setMemoryMethod.invoke(brain, attackTargetMemoryType, target);
            }
        } catch (Exception e) {
            CreateLaborRush.LOGGER.debug("MaidRebellionHandler: 强制攻击目标失败", e);
        }
    }

    /**
     * 清空女仆的攻击目标（无玩家时每 tick 调用）：setTarget(null) + 擦除 ATTACK_TARGET 脑记忆，
     * 即使她被其他生物攻击也不反击，不打任何非玩家目标。
     */
    public static void clearAttackTarget(LivingEntity maid) {
        if (maidClass == null || !maidClass.isInstance(maid)) return;
        try {
            maidClass.getMethod("setTarget", LivingEntity.class).invoke(maid, new Object[]{null});
            if (getBrainMethod != null && eraseMemoryMethod != null && attackTargetMemoryType != null) {
                Object brain = getBrainMethod.invoke(maid);
                eraseMemoryMethod.invoke(brain, attackTargetMemoryType);
            }
        } catch (Exception e) {
            CreateLaborRush.LOGGER.debug("MaidRebellionHandler: 清空攻击目标失败", e);
        }
    }

    /**
     * 反射调用女仆 GunCommonUtil.isGun 检测枪械（TACZ/SWarfare 软依赖）
     */
    private static boolean isGunItem(ItemStack stack) {
        if (gunUtilClass == null) return false;
        try {
            return (boolean) gunUtilClass.getMethod("isGun", ItemStack.class).invoke(null, stack);
        } catch (Exception e) {
            return false;
        }
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
                // 枪械伤害无法直接读取，无法比较时取第一把；后续可反射枪械属性比较
                if (bestGun.isEmpty()) {
                    bestGun = stack;
                }
                continue;
            }

            // 第二优先级：剑/斧（近战武器）
            if (item instanceof SwordItem sword) {
                double dmg = sword.getDamage(stack);
                int dur = stack.getMaxDamage() - stack.getDamageValue();
                if (dmg > bestMeleeDamage || (dmg == bestMeleeDamage && dur > bestMeleeDurability)) {
                    bestMelee = stack;
                    bestMeleeDamage = dmg;
                    bestMeleeDurability = dur;
                }
            } else if (item instanceof AxeItem axe) {
                double dmg = 7.0; // 默认斧头攻击力，1.21.1 通过属性修饰符获取
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
     * 反射读取女仆当前任务（IMaidTask）
     */
    private static Object getCurrentTask(LivingEntity maid) {
        try {
            return maidClass.getMethod("getTask").invoke(maid);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 起义结束后恢复女仆：切回起义前的任务（无记录则切回默认 idle），
     * 重建脑任务并清除攻击目标，让她回去干活
     */
    public static void restoreMaid(LivingEntity maid, ServerLevel level) {
        if (maidClass == null || !maidClass.isInstance(maid)) return;

        try {
            Object originalTask = ORIGINAL_TASKS.remove(maid.getUUID());
            if (originalTask == null && taskManagerClass != null) {
                // 起义前没有记录到原任务时，切回默认 idle 任务
                try {
                    originalTask = taskManagerClass.getMethod("getIdleTask").invoke(null);
                } catch (Exception ignored) {
                }
            }
            if (originalTask != null && taskInterfaceClass != null) {
                maidClass.getMethod("setTask", taskInterfaceClass).invoke(maid, originalTask);
                // 任务切换后必须重建脑任务，否则女仆 AI 还是旧的攻击任务
                if (level != null) {
                    maidClass.getMethod("refreshBrain", ServerLevel.class).invoke(maid, level);
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
     * 使用 1.2.1 女仆的实际气泡 API：EntityMaid.addChatBubble(long, ChatText)。
     * 反射失败时只记录日志，不再给女仆改名（用户明确要求气泡而非头顶文字）。
     *
     * @param maid      女仆实体
     * @param ownerName 女仆主人名字（通过 getOwner() 获取），可为 null
     */
    public static void showRebellionBubble(LivingEntity maid, String ownerName) {
        if (maidClass == null || !maidClass.isInstance(maid)) return;

        // 随机选取对话（通过 lang 键，自动切换语言）
        String key = "chat.createlaborrush.maid.dialogue." + RANDOM.nextInt(DIALOGUES_COUNT);
        String dialogue = Component.translatable(key).getString();

        // 有主人名字时，用 lang 键获取本地化前缀
        if (ownerName != null && !ownerName.isEmpty()) {
            String prefix = Component.translatable("chat.createlaborrush.maid.owner_prefix", ownerName).getString();
            dialogue = prefix + dialogue;
        }

        try {
            if (chatTextClass == null || chatTextTypeClass == null || chatBubbleMangerClass == null) {
                CreateLaborRush.LOGGER.debug("MaidRebellionHandler: 女仆气泡类不可用，跳过气泡");
                return;
            }
            // ChatText 序列化会无条件编码 iconPath，必须用 EMPTY_ICON_PATH（null 会导致
            // SynchedEntityData 编码 NPE 使玩家掉线）
            Object emptyIcon = chatTextClass.getField("EMPTY_ICON_PATH").get(null);
            Object textType = chatTextTypeClass.getMethod("valueOf", String.class).invoke(null, "TEXT");
            Object chatText = chatTextClass.getConstructor(chatTextTypeClass, ResourceLocation.class, String.class)
                    .newInstance(textType, emptyIcon, dialogue);
            // 时间戳必须用 ChatBubbleManger.getEndTime()（currentTimeMillis 会被渲染端视为已过期而不显示）
            var endTimeMethod = chatBubbleMangerClass.getDeclaredMethod("getEndTime");
            endTimeMethod.setAccessible(true);
            long endTime = (long) endTimeMethod.invoke(null);
            // maid.addChatBubble(endTime, chatText)
            maidClass.getMethod("addChatBubble", long.class, chatTextClass)
                    .invoke(maid, endTime, chatText);
        } catch (Exception e) {
            CreateLaborRush.LOGGER.info("MaidRebellionHandler: 女仆气泡显示失败（不做改名降级）", e);
        }
    }
}