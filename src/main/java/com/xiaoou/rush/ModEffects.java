package com.xiaoou.rush;

import com.xiaoou.rush.effect.WorkEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registry for custom mob effects used by Create: Labor Rush.
 *
 * <p>Currently registers a single effect:
 * <ul>
 *   <li>{@link #WORK_EFFECT} — "Work!" — boosts processing speed
 *       and enables configurable item destruction.</li>
 * </ul>
 */
public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
        DeferredRegister.create(Registries.MOB_EFFECT, CreateLaborRush.MODID);

    /** The "Work!" effect — boosts processing speed and enables item destruction. */
    public static final DeferredHolder<MobEffect, MobEffect> WORK_EFFECT =
        EFFECTS.register("work", WorkEffect::new);
}