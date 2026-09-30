package com.xiaoou.rush;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Map;

/**
 * 帽子的盔甲材料：3 点防御、1 点盔甲韧性，铁砧修复材料用 Create 的纸板。
 *
 * <p>1.21 的盔甲贴图不再由物品自己决定，而是由材料的 {@link ArmorMaterial.Layer} 推出来
 * （{@code textures/models/armor/<资产名>_layer_1.png}）。所以每种不同贴图的帽子都必须
 * 单独注册一份材料——哪怕数值完全一样，只靠改 assetName 指向各自的贴图。
 */
public class ModArmorMaterials {

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
        DeferredRegister.create(Registries.ARMOR_MATERIAL, CreateLaborRush.MODID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> TALL_HAT =
        ARMOR_MATERIALS.register("tall_hat", () -> new ArmorMaterial(
            Map.of(ArmorItem.Type.HELMET, 3),
            10,
            SoundEvents.ARMOR_EQUIP_LEATHER,
            () -> Ingredient.of(BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("create", "cardboard"))),
            List.of(new ArmorMaterial.Layer(
                ResourceLocation.fromNamespaceAndPath(CreateLaborRush.MODID, "tall_hat"))),
            1.0F,
            0.0F
        ));

    /** 资本帽：数值与高帽一致，只是贴图指向 capital_hat_layer_1.png */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> CAPITAL_HAT =
        ARMOR_MATERIALS.register("capital_hat", () -> new ArmorMaterial(
            Map.of(ArmorItem.Type.HELMET, 3),
            10,
            SoundEvents.ARMOR_EQUIP_LEATHER,
            () -> Ingredient.of(BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("create", "cardboard"))),
            List.of(new ArmorMaterial.Layer(
                ResourceLocation.fromNamespaceAndPath(CreateLaborRush.MODID, "capital_hat"))),
            1.0F,
            0.0F
        ));

    private ModArmorMaterials() {
    }
}