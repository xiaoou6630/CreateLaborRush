package com.xiaoou.rush.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LeadItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让原版栓绳可以被附魔。
 * 玩家可以使用铁砧将火焰附加附魔书打在栓绳上，
 * 从而触发 Work II（火焰附加 I）或 Work III（火焰附加 II）。
 */
@Mixin(LeadItem.class)
public class LeadItemMixin {

    @Inject(method = "isEnchantable", at = @At("RETURN"), cancellable = true)
    private void makeLeadEnchantable(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}