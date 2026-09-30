package com.xiaoou.rush;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 本模组自己的创造模式标签页。
 *
 * <p>刻意不往 CVL 的标签页里塞物品：那样需要引用 CVL 的内部字段，
 * 一旦 CVL 改结构就会连带把我们的物品（乃至 CVL 的分类）一起搞崩。
 */
public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateLaborRush.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RUSH_TAB =
        CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.createlaborrush"))
            .icon(() -> new ItemStack(ModItems.TALL_HAT.get()))
            .displayItems((params, output) -> {
                output.accept(ModItems.TALL_HAT.get());
                output.accept(ModItems.CAPITAL_HAT.get());
            })
            .build());

    private ModCreativeTabs() {
    }
}