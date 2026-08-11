package com.xiaoou.rush.mixin;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BellBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class BellItemMixin {

    @Inject(method = "isEnchantable", at = @At("HEAD"), cancellable = true)
    private void onIsEnchantable(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof BellBlock) {
            cir.setReturnValue(stack.getCount() == 1);
        }
    }

    @Inject(method = "getEnchantmentValue", at = @At("HEAD"), cancellable = true)
    private void onGetEnchantmentValue(CallbackInfoReturnable<Integer> cir) {
        // 1.20.1: getEnchantmentValue() 无参数，通过 (Item)(Object)this 获取 Item 实例
        Item self = (Item)(Object)this;
        if (self instanceof BlockItem blockItem && blockItem.getBlock() instanceof BellBlock) {
            cir.setReturnValue(15);
        }
    }
}