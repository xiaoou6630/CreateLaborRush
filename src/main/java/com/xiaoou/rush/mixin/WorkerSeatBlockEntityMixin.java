package com.xiaoou.rush.mixin;

import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.xiaoou.rush.Config;
import com.xiaoou.rush.ModEffects;
import com.yyn.labor.blocks.WorkerSeatBlockEntity;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorkerSeatBlockEntity.class)
public abstract class WorkerSeatBlockEntityMixin {

    @Shadow(remap = false) protected int processingTimer;
    @Shadow(remap = false) protected int outputCooldownDuration;
    @Shadow(remap = false) protected ItemStack processingStack;
    @Shadow(remap = false) protected int outputCooldown;
    @Shadow(remap = false) protected void updateHand() {}

    @Unique
    private LivingEntity laborrush$findWorker() {
        var self = (BlockEntity)(Object)this;
        var level = self.getLevel();
        var pos = self.getBlockPos();
        if (level == null) return null;
        for (SeatEntity seat : level.getEntitiesOfClass(SeatEntity.class, new AABB(pos))) {
            if (!seat.isVehicle()) continue;
            for (Entity passenger : seat.getPassengers()) {
                if (passenger instanceof LivingEntity living) return living;
            }
        }
        return null;
    }

    @Inject(method = "processWork", at = @At("HEAD"), remap = false, require = 0)
    private void laborrush$beforeProcessWork(CallbackInfo ci) {
        if (this.processingTimer <= 0 && this.outputCooldown <= 0) return;
        LivingEntity worker = laborrush$findWorker();
        if (worker == null || !worker.hasEffect(ModEffects.WORK_EFFECT)) return;
        if (this.processingTimer > 0) {
            this.processingTimer = 1;
        }
    }

    @Inject(method = "finishProcessing", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void laborrush$onFinishProcessing(CallbackInfo ci) {
        LivingEntity worker = laborrush$findWorker();
        if (worker == null || !worker.hasEffect(ModEffects.WORK_EFFECT)) return;

        this.outputCooldownDuration = 1;

        double chance = Config.DESTROY_CHANCE.get();
        if (chance > 0 && worker.getRandom().nextDouble() < chance) {
            // 销毁物品
            this.processingStack = ItemStack.EMPTY;
            this.updateHand();
            this.outputCooldown = 1;

            // 获取方块实体、世界、坐标用于同步与播放音效
            var self = (BlockEntity)(Object)this;
            var level = self.getLevel();

            if (level != null) {
                // ★ 播放原版物品损坏音效 (entity.item.break)
                level.playSound(
                    null,                           // 玩家（null 表示无特定来源，播放给所有人）
                    self.getBlockPos(),             // 位置
                    SoundEvents.ITEM_BREAK,         // 音效事件
                    SoundSource.BLOCKS,             // 音效分类（方便玩家调节音量）
                    1.0F,                           // 音量
                    0.8F + level.random.nextFloat() * 0.4F // 随机音调（增加真实感）
                );

                // 发送方块更新（让客户端同步村民手部变化）
                level.sendBlockUpdated(self.getBlockPos(), self.getBlockState(), self.getBlockState(), 3);
            }

            self.setChanged(); // 标记脏数据

            ci.cancel(); // 取消原方法，避免原版因空栈直接返回而跳过我们的清理
        }
        // 未触发销毁时，直接走原版方法（产出物品），不做任何干预
    }
}