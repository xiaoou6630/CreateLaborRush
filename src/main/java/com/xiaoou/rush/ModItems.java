package com.xiaoou.rush;

import com.xiaoou.rush.item.CapitalHatItem;
import com.xiaoou.rush.item.TallHatItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(CreateLaborRush.MODID);

    /** 批斗大会高帽：占玩家头盔位。耐久系数 15，与原版铁头盔一致 */
    public static final DeferredItem<Item> TALL_HAT = ITEMS.register("tall_hat",
        () -> new TallHatItem(ModArmorMaterials.TALL_HAT, ArmorItem.Type.HELMET,
            new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(15))));

    /** 资本帽：同样占头盔位，数值与高帽一致，只是外观是黑呢礼帽 */
    public static final DeferredItem<Item> CAPITAL_HAT = ITEMS.register("capital_hat",
        () -> new CapitalHatItem(ModArmorMaterials.CAPITAL_HAT, ArmorItem.Type.HELMET,
            new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(15))));

    private ModItems() {
    }
}