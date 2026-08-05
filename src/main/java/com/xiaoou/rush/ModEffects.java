package com.xiaoou.rush;

import com.xiaoou.rush.effect.WorkEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
        DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, CreateLaborRush.MODID);

    public static final RegistryObject<MobEffect> WORK_EFFECT =
        EFFECTS.register("work", WorkEffect::new);
}