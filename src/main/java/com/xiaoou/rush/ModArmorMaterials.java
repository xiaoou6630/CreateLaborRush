package com.xiaoou.rush;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 帽子的盔甲材料：3 点防御、1 点盔甲韧性，铁砧修复材料用 Create 的纸板。
 * 1.20.1 的 ArmorMaterial 只是个接口，不需要注册到注册表。
 */
public class ModArmorMaterials {

    public static final ArmorMaterial TALL_HAT = new ArmorMaterial() {

        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            // 对齐原版头盔耐久：基础 11 × 系数 15（同铁头盔）
            return type == ArmorItem.Type.HELMET ? 11 * 15 : 0;
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return type == ArmorItem.Type.HELMET ? 3 : 0;
        }

        @Override
        public int getEnchantmentValue() {
            return 10;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_LEATHER;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(BuiltInRegistries.ITEM.get(
                new ResourceLocation("create", "cardboard")));
        }

        @Override
        public String getName() {
            return CreateLaborRush.MODID + ":tall_hat";
        }

        @Override
        public float getToughness() {
            return 1.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.0F;
        }
    };

    /** 资本帽：数值与修复材料和高帽完全一致，只是换个名字（贴图由物品自己指定） */
    public static final ArmorMaterial CAPITAL_HAT = new ArmorMaterial() {

        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            // 对齐原版头盔耐久：基础 11 × 系数 15（同铁头盔）
            return type == ArmorItem.Type.HELMET ? 11 * 15 : 0;
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return type == ArmorItem.Type.HELMET ? 3 : 0;
        }

        @Override
        public int getEnchantmentValue() {
            return 10;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_LEATHER;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(BuiltInRegistries.ITEM.get(
                new ResourceLocation("create", "cardboard")));
        }

        @Override
        public String getName() {
            return CreateLaborRush.MODID + ":capital_hat";
        }

        @Override
        public float getToughness() {
            return 1.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.0F;
        }
    };

    private ModArmorMaterials() {
    }
}