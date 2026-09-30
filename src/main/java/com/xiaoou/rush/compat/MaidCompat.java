package com.xiaoou.rush.compat;

import com.xiaoou.rush.CreateLaborRush;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;

/**
 * 车万女仆（TLM）兼容入口类。
 * <p>
 * 这是「类加载隔离」的入口：{@link com.xiaoou.rush.util.WorkerTypeDetector} 和
 * {@link com.xiaoou.rush.handler.MaidRebellionHandler} 只允许调用本类。
 * <b>本类的字段、方法签名与泛型里绝不能出现任何 TLM 类型</b>，否则在未安装 TLM
 * （mods.toml 里是软依赖 mandatory=false）的环境下加载本类时会因解析不到 TLM 类而崩溃。
 * <p>
 * 所有真正引用 TLM 类型的代码都放在 {@link MaidCompatImpl} 里，并且只有先确认
 * TLM（mod id: touhou_little_maid）已加载后才会触碰它。
 */
public final class MaidCompat {

    /** 车万女仆的 mod id */
    public static final String MOD_ID = "touhou_little_maid";

    /** TLM 是否已加载（运行期软依赖判断，只算一次） */
    private static final boolean LOADED;

    static {
        boolean loaded = false;
        try {
            loaded = ModList.get().isLoaded(MOD_ID);
        } catch (Throwable t) {
            // 极端情况下 ModList 尚不可用，按未安装处理
            loaded = false;
        }
        LOADED = loaded;
        if (LOADED) {
            CreateLaborRush.LOGGER.info("MaidCompat: 检测到女仆模组 (Touhou Little Maid)，启用直接 API 调用");
        } else {
            CreateLaborRush.LOGGER.debug("MaidCompat: 未检测到女仆模组，女仆功能已降级");
        }
    }

    private MaidCompat() {
    }

    /** TLM 是否已加载 */
    public static boolean isLoaded() {
        return LOADED;
    }

    /** 实体是否为女仆 */
    public static boolean isMaid(LivingEntity entity) {
        return LOADED && MaidCompatImpl.isMaid(entity);
    }

    /** 获取女仆主人；非女仆或 TLM 未加载时返回 null */
    public static LivingEntity getOwner(LivingEntity maid) {
        if (!LOADED) {
            return null;
        }
        try {
            return MaidCompatImpl.getOwner(maid);
        } catch (Throwable t) {
            CreateLaborRush.LOGGER.debug("MaidCompat: 获取女仆主人失败", t);
            return null;
        }
    }

    /** 获取女仆可用背包（IItemHandler）；非女仆或不可用时返回 null */
    public static IItemHandler getAvailableInv(LivingEntity maid) {
        if (!LOADED) {
            return null;
        }
        try {
            return MaidCompatImpl.getAvailableInv(maid);
        } catch (Throwable t) {
            CreateLaborRush.LOGGER.debug("MaidCompat: 获取女仆背包失败", t);
            return null;
        }
    }

    /**
     * 读取女仆当前任务。
     * <p>
     * 以 {@code Object} 返回（实际是 TLM 的 IMaidTask），这样可以避免在入口类签名里
     * 出现 TLM 类型；调用方只负责原样保存、再通过 {@link #setTask} 还回去。
     */
    public static Object getCurrentTask(LivingEntity maid) {
        if (!LOADED) {
            return null;
        }
        try {
            return MaidCompatImpl.getCurrentTask(maid);
        } catch (Throwable t) {
            CreateLaborRush.LOGGER.debug("MaidCompat: 读取女仆任务失败", t);
            return null;
        }
    }

    /** 获取女仆默认 idle 任务（Object，实际是 IMaidTask） */
    public static Object getIdleTask() {
        if (!LOADED) {
            return null;
        }
        try {
            return MaidCompatImpl.getIdleTask();
        } catch (Throwable t) {
            CreateLaborRush.LOGGER.debug("MaidCompat: 获取女仆默认任务失败", t);
            return null;
        }
    }

    /** 获取女仆攻击任务（Object，实际是 IMaidTask）；未找到返回 null */
    public static Object getAttackTask() {
        if (!LOADED) {
            return null;
        }
        try {
            return MaidCompatImpl.getAttackTask();
        } catch (Throwable t) {
            CreateLaborRush.LOGGER.debug("MaidCompat: 获取女仆攻击任务失败", t);
            return null;
        }
    }

    /** 给女仆设置任务，{@code task} 必须是先前从本类获取到的任务对象 */
    public static void setTask(LivingEntity maid, Object task) {
        if (!LOADED || task == null) {
            return;
        }
        try {
            MaidCompatImpl.setTask(maid, task);
        } catch (Throwable t) {
            CreateLaborRush.LOGGER.debug("MaidCompat: 设置女仆任务失败", t);
        }
    }

    /** 重建女仆脑任务（任务切换后必须调用，否则 AI 还是旧的） */
    public static void refreshBrain(LivingEntity maid, ServerLevel level) {
        if (!LOADED) {
            return;
        }
        try {
            MaidCompatImpl.refreshBrain(maid, level);
        } catch (Throwable t) {
            CreateLaborRush.LOGGER.debug("MaidCompat: 重建女仆脑任务失败", t);
        }
    }

    /** 通过 TLM 官方气泡 API 显示一段文字气泡；成功返回 true，失败返回 false */
    public static boolean showTextBubble(LivingEntity maid, String text) {
        if (!LOADED) {
            return false;
        }
        try {
            MaidCompatImpl.showTextBubble(maid, text);
            return true;
        } catch (Throwable t) {
            CreateLaborRush.LOGGER.info("MaidCompat: 女仆气泡显示失败", t);
            return false;
        }
    }

    /** 物品是否为枪械（TACZ / SWarfare 兼容检测）；未加载或异常时返回 false */
    public static boolean isGunItem(ItemStack stack) {
        if (!LOADED) {
            return false;
        }
        try {
            return MaidCompatImpl.isGunItem(stack);
        } catch (Throwable t) {
            return false;
        }
    }
}
