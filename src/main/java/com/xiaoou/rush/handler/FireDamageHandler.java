package com.xiaoou.rush.handler;

import com.xiaoou.rush.CreateLaborRush;
import com.xiaoou.rush.ModEffects;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = CreateLaborRush.MODID)
public class FireDamageHandler {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        var entity = event.getEntity();
        var source = event.getSource();

        if (!(entity instanceof LivingEntity living)) return;
        if (!living.hasEffect(ModEffects.WORK_EFFECT)) return;

        // ✅ 使用 DamageTypeTags.IS_FIRE 检测火焰伤害
        boolean isFireDamage = source.is(DamageTypeTags.IS_FIRE) || "onFire".equals(source.getMsgId());

        if (isFireDamage) {
            // ✅ 将伤害设为 0（NeoForge 1.21 正确方法）
            event.setNewDamage(0.0f);
        }
    }
}