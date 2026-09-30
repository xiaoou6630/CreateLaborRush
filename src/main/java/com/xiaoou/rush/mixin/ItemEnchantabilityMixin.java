package com.xiaoou.rush.mixin;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.level.block.BellBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让原版拴绳与钟可以附魔（火焰附加 / 引雷）。
 *
 * <p>为什么必须注入 {@link Item}：{@code isEnchantable} / {@code getEnchantmentValue} 都声明在
 * Item 上，而拴绳、钟都是原版物品，没法用子类覆盖。为了把注入点压到最少，原先拆成
 * LeadItemMixin + BellItemMixin 两个类，现已合并为本类，且 isEnchantable 只保留一处 HEAD 注入。
 * 每处注入内部都先做 instanceof 判断，对其它物品零影响。
 *
 * <p>「火焰附加与引雷互斥」不在这里，它挂在 {@link ItemExtensionMixin} 上
 * （那个方法不在 Item 里，而在 IForgeItem 接口上）。
 */
@Mixin(Item.class)
public class ItemEnchantabilityMixin {

    @Unique
    private boolean laborrush$isBell(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof BellBlock;
    }

    @Inject(method = "isEnchantable", at = @At("HEAD"), cancellable = true)
    private void laborrush$isEnchantable(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.getItem() instanceof LeadItem || laborrush$isBell(stack)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getEnchantmentValue", at = @At("HEAD"), cancellable = true)
    private void laborrush$getEnchantmentValue(CallbackInfoReturnable<Integer> cir) {
        // 1.20.1: getEnchantmentValue() 无参数，通过 (Item)(Object)this 取 Item 实例
        Item self = (Item)(Object)this;
        if (self instanceof BlockItem blockItem && blockItem.getBlock() instanceof BellBlock) {
            cir.setReturnValue(15);
        }
    }
}