package com.xiaoou.rush.mixin;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LeadItem.class)
public class LeadItemMixin {

    @Inject(method = "isEnchantable", at = @At("RETURN"), cancellable = true)
    private void makeLeadEnchantable(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    @Inject(method = "canApplyAtEnchantingTable", at = @At("HEAD"), cancellable = true)
    private void onCanApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment, CallbackInfoReturnable<Boolean> cir) {
        // Fire Aspect and Channeling are mutually exclusive
        // Get the enchantment registry to identify the enchantment being applied
        var enchantmentRegistry = (Registry<Enchantment>) BuiltInRegistries.REGISTRY.get(Registries.ENCHANTMENT.location());
        if (enchantmentRegistry == null) return;

        var keyOpt = enchantmentRegistry.getResourceKey(enchantment);
        if (keyOpt.isEmpty()) return;

        var key = keyOpt.get();

        // Check what enchantments the stack already has
        boolean hasFireAspect = false;
        boolean hasChanneling = false;
        for (var holder : stack.getEnchantments().keySet()) {
            var heldKeyOpt = holder.unwrapKey();
            if (heldKeyOpt.isPresent()) {
                var heldKey = heldKeyOpt.get();
                if (heldKey.equals(Enchantments.FIRE_ASPECT)) hasFireAspect = true;
                if (heldKey.equals(Enchantments.CHANNELING)) hasChanneling = true;
            }
        }

        // If trying to add Fire Aspect but already has Channeling
        if (key.equals(Enchantments.FIRE_ASPECT) && hasChanneling) {
            cir.setReturnValue(false);
        }
        // If trying to add Channeling but already has Fire Aspect
        if (key.equals(Enchantments.CHANNELING) && hasFireAspect) {
            cir.setReturnValue(false);
        }
    }
}