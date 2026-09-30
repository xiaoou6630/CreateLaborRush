package com.xiaoou.rush.handler;

import com.xiaoou.rush.CreateLaborRush;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 火焰附加与引雷在拴绳上互斥。
 *
 * <p>为什么用铁砧事件而不是 Mixin：这个互斥原本写成 {@code @Mixin(Item.class)} 注入
 * {@code canApplyAtEnchantingTable}，但该方法根本不在 {@code Item} 的字节码里——它是
 * {@code IForgeItem} 的接口默认方法。Mixin 注解处理器明确不支持在接口 mixin 里写 {@code @Inject}
 * （报 "Injector in interface is unsupported"），所以改用铁砧事件，零 Mixin 风险。
 *
 * <p>铁砧正是玩家实际把两种附魔凑到同一根拴绳上的路径（用附魔书给拴绳打附魔）。
 */
@Mod.EventBusSubscriber(modid = CreateLaborRush.MODID)
public class LeadEnchantConflictHandler {

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (right.isEmpty()) return;
        // 只有"结果会是一根拴绳"时才需要拦：栏位左是拴绳
        if (!(left.getItem() instanceof LeadItem)) return;

        boolean leftFire = hasEnchant(left, Enchantments.FIRE_ASPECT);
        boolean leftChannel = hasEnchant(left, Enchantments.CHANNELING);
        boolean rightFire = hasEnchant(right, Enchantments.FIRE_ASPECT);
        boolean rightChannel = hasEnchant(right, Enchantments.CHANNELING);

        // 任何一侧已带一种、另一侧带另一种（含右栏自带两种）都会凑成互斥组合 → 不产出
        if ((leftFire && rightChannel) || (leftChannel && rightFire) || (rightFire && rightChannel)) {
            event.setOutput(ItemStack.EMPTY);
        }
    }

    /** 1.20.1 的 ItemStack 没有 getEnchantments()（那是 1.20.5+），走 EnchantmentHelper */
    private static boolean hasEnchant(ItemStack stack, Enchantment enchantment) {
        return EnchantmentHelper.getEnchantments(stack).containsKey(enchantment);
    }

    private LeadEnchantConflictHandler() {
    }
}