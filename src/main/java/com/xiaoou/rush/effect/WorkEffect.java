package com.xiaoou.rush.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * The "Work!" effect — a purely visual status effect.
 *
 * <p>This effect is classified as {@link MobEffectCategory#BENEFICIAL} and
 * renders with a red (0xFF0000) particle color. It has no inherent logic
 * of its own; all speed-boosting and item-destruction behavior is
 * implemented via Mixins in {@code WorkerSeatBlockEntityMixin}.</p>
 */
public class WorkEffect extends MobEffect {
    public WorkEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF0000);
    }
}