package com.xiaoou.rush;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 画作注册（Forge 1.20.1 专用）。
 *
 * <p>1.21 起画作是数据驱动的（有个 {@code painting_variant} 目录下的 JSON），
 * 但 1.20.1 没有这些 JSON —— 原版 31 个变体全是在代码里注册进
 * {@code BuiltInRegistries.PAINTING_VARIANT} 的，所以这里也只能用代码注册。
 *
 * <p>贴图路径不需要额外声明：{@code PaintingTextureManager} 会用变体的注册表 ID
 * 去 {@code minecraft:paintings} 图集取 sprite，而该图集的来源是
 * {@code {"type":"directory","source":"painting"}}——它会扫描<b>所有命名空间</b>的
 * {@code textures/painting/} 目录。所以只要贴图放在
 * {@code assets/createlaborrush/textures/painting/slogan.png}，sprite ID 就是
 * {@code createlaborrush:slogan}，与原版机制天然对齐，无需覆盖原版图集文件。
 */
public class ModPaintings {

    public static final DeferredRegister<PaintingVariant> PAINTING_VARIANTS =
        DeferredRegister.create(Registries.PAINTING_VARIANT, CreateLaborRush.MODID);

    /**
     * 「时间就是金钱，效率就是生命」标语画：4x3 格。
     *
     * <p><b>注意单位</b>：1.20.1 的 {@code PaintingVariant(width, height)} 是按
     * <b>贴图像素</b>算的（16 像素 = 1 格），不是格数——原版 1x1 的 kebab 写的是
     * {@code (16, 16)}，4x4 的 wither 是 {@code (64, 64)}。
     * 所以 4x3 格要写 {@code (64, 48)}。
     * 之前写的 {@code (4, 3)} 相当于 0.25 x 0.19 格，尺寸非法，
     * 游戏里表现为"识别成 1x1、根本放不下去"。
     *
     * <p>贴图 256x192（每格 64px），见 tools/make_slogan_painting.py。
     * 渲染时整张贴图会被拉伸到 4x3 格上，与贴图分辨率无关。
     */
    public static final RegistryObject<PaintingVariant> SLOGAN =
        PAINTING_VARIANTS.register("slogan", () -> new PaintingVariant(64, 48));

    private ModPaintings() {
    }
}