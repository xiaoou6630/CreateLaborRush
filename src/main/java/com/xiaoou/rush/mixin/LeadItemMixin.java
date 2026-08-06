package com.xiaoou.rush.mixin;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class LeadItemMixin {

    @Inject(method = "isEnchantable", at = @At("RETURN"), cancellable = true)
    private void makeLeadEnchantable(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.getItem() instanceof LeadItem) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "canApplyAtEnchantingTable", at = @At("HEAD"), cancellable = true, remap = false)
    private void onCanApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment, CallbackInfoReturnable<Boolean> cir) {
        if (!(stack.getItem() instanceof LeadItem)) return;

        // Fire Aspect and Channeling are mutually exclusive
        if (enchantment == Enchantments.FIRE_ASPECT) {
            if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.CHANNELING, stack) > 0) {
                cir.setReturnValue(false);
            }
        }
        if (enchantment == Enchantments.CHANNELING) {
            if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, stack) > 0) {
                cir.setReturnValue(false);
            }
        }
    }
}