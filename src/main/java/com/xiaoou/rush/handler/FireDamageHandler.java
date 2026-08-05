package com.xiaoou.rush.handler;

import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.ModEffects;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateLaborRush.MODID)
public class FireDamageHandler {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        var living = event.getEntity();
        var source = event.getSource();

        if (!living.hasEffect(ModEffects.WORK_EFFECT.get())) return;

        boolean isFireDamage = source.is(DamageTypeTags.IS_FIRE) || "onFire".equals(source.getMsgId());

        if (isFireDamage) {
            event.setAmount(0.0f);
        }
    }
}