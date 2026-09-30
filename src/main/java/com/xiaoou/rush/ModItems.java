package com.xiaoou.rush;

import com.xiaoou.rush.item.CapitalHatItem;
import com.xiaoou.rush.item.TallHatItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, CreateLaborRush.MODID);

    /** 批斗大会高帽：占玩家头盔位 */
    public static final RegistryObject<Item> TALL_HAT = ITEMS.register("tall_hat",
        () -> new TallHatItem(ModArmorMaterials.TALL_HAT, ArmorItem.Type.HELMET, new Item.Properties()));

    /** 资本帽：同样占玩家头盔位，纯外观差异 */
    public static final RegistryObject<Item> CAPITAL_HAT = ITEMS.register("capital_hat",
        () -> new CapitalHatItem(ModArmorMaterials.CAPITAL_HAT, ArmorItem.Type.HELMET, new Item.Properties()));

    private ModItems() {
    }
}