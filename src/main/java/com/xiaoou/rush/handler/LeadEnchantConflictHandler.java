package com.xiaoou.rush.handler;

import com.xiaoou.rush.CreateLaborRush;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

/**
 * 火焰附加与引雷在拴绳上互斥。
 *
 * <p>为什么用铁砧事件而不是 Mixin：这个互斥原本写成 {@code @Mixin(Item.class)} 注入
 * {@code canApplyAtEnchantingTable}，但 NeoForge 1.21.1 里 Item 根本没有这个方法
 * （附魔能力迁到了 {@code IItemExtension}），Mixin 匹配不到又不设 {@code require}，
 * 于是从上线起就静默失效。改用铁砧事件，零 Mixin 风险。
 *
 * <p>铁砧正是玩家实际把两种附魔凑到同一根拴绳上的路径（用附魔书给拴绳打附魔）。
 */
@EventBusSubscriber(modid = CreateLaborRush.MODID)
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

    /** 1.21 的附魔是数据驱动的，Enchantments.FIRE_ASPECT 是 ResourceKey，用 Holder#is 比对 */
    private static boolean hasEnchant(ItemStack stack, ResourceKey<Enchantment> enchantment) {
        for (Holder<Enchantment> held : stack.getEnchantments().keySet()) {
            if (held.is(enchantment)) return true;
        }
        return false;
    }

    private LeadEnchantConflictHandler() {
    }
}