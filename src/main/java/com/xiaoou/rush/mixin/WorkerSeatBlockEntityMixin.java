package com.xiaoou.rush.mixin;

import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.xiaoou.rush.Config;
import com.xiaoou.rush.ModEffects;
import com.yyn.labor.blocks.WorkerSeatBlockEntity;
import com.yyn.labor.blocks.SeatMaterial;

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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorkerSeatBlockEntity.class)
public abstract class WorkerSeatBlockEntityMixin {

    @Shadow(remap = false) protected int processingTimer;
    @Shadow(remap = false) protected int outputCooldownDuration;
    @Shadow(remap = false) protected ItemStack processingStack;
    @Shadow(remap = false) protected int outputCooldown;
    @Shadow(remap = false) protected void updateHand() {}
    @Shadow(remap = false) protected void finishProcessing() {}

    @Unique
    private LivingEntity laborrush$findWorker() {
        var self = (BlockEntity)(Object)this;
        var level = self.getLevel();
        var pos = self.getBlockPos();
        if (level == null) return null;
        AABB searchBox = new AABB(pos).inflate(0.5);
        for (SeatEntity seat : level.getEntitiesOfClass(SeatEntity.class, searchBox)) {
            if (!seat.isVehicle()) continue;
            for (Entity passenger : seat.getPassengers()) {
                if (passenger instanceof LivingEntity living) return living;
            }
        }
        return null;
    }

    // ===== 加工加速 =====
    @Inject(method = "processWork", at = @At("HEAD"), remap = false, require = 1)
    private void laborrush$beforeProcessWork(CallbackInfo ci) {
        if (this.processingTimer <= 0 && this.outputCooldown <= 0) return;
        LivingEntity worker = laborrush$findWorker();
        if (worker == null || !worker.hasEffect(ModEffects.WORK_EFFECT)) return;
        if (this.processingTimer > 0) {
            this.processingTimer = 1;
        }
    }

    // ===== 批量覆盖（皮带） =====
    @Redirect(
        method = "tryTakeFromBelt",
        at = @At(
            value = "INVOKE",
            target = "Lcom/yyn/labor/blocks/SeatMaterial;getBatchSize()I"
        ),
        remap = false,
        require = 1
    )
    private int laborrush$overrideBatchSize(SeatMaterial material) {
        // ✅ 创造座椅直接放行
        if (material == SeatMaterial.CREATIVE) {
            return material.getBatchSize();
        }

        LivingEntity worker = laborrush$findWorker();
        if (worker != null && worker.hasEffect(ModEffects.WORK_EFFECT)) {
            int customBatch = worker.getPersistentData().getInt("laborrush.batchSize");
            if (customBatch > 0) {
                return customBatch;  // 32 或 64
            }
        }
        return material.getBatchSize();
    }

    // ===== 批量覆盖（货板） =====
    @Redirect(
        method = "tryTakeFromDepot",
        at = @At(
            value = "INVOKE",
            target = "Lcom/yyn/labor/blocks/SeatMaterial;getBatchSize()I"
        ),
        remap = false,
        require = 1
    )
    private int laborrush$overrideBatchSizeDepot(SeatMaterial material) {
        return laborrush$overrideBatchSize(material);
    }

    // ===== 批量覆盖（工作盆） =====
    @Redirect(
        method = "tryTakeFromBasin",
        at = @At(
            value = "INVOKE",
            target = "Lcom/yyn/labor/blocks/SeatMaterial;getBatchSize()I"
        ),
        remap = false,
        require = 1
    )
    private int laborrush$overrideBatchSizeBasin(SeatMaterial material) {
        return laborrush$overrideBatchSize(material);
    }

    // ===== 销毁 + 音效（支持按比例销毁） =====
    @Redirect(
        method = "processWork",
        at = @At(
            value = "INVOKE",
            target = "Lcom/yyn/labor/blocks/WorkerSeatBlockEntity;finishProcessing()V"
        ),
        remap = false,
        require = 1
    )
    private void laborrush$redirectFinishProcessing(WorkerSeatBlockEntity self) {
        LivingEntity worker = laborrush$findWorker();
        if (worker == null || !worker.hasEffect(ModEffects.WORK_EFFECT)) {
            this.finishProcessing();
            return;
        }

        this.outputCooldownDuration = 1;

        double chance = Config.DESTROY_CHANCE.get();
        if (chance > 0 && worker.getRandom().nextDouble() < chance) {
            int currentCount = this.processingStack.getCount();

            // ✅ 使用配置文件中的销毁比例范围
            double minRatio = Config.DESTROY_RATIO_MIN.get();
            double maxRatio = Config.DESTROY_RATIO_MAX.get();

            // 确保 min <= max
            if (minRatio > maxRatio) {
                double temp = minRatio;
                minRatio = maxRatio;
                maxRatio = temp;
            }

            // 随机生成销毁比例
            float destroyRatio = (float)(minRatio + worker.getRandom().nextDouble() * (maxRatio - minRatio));

            if (currentCount > 1 && destroyRatio > 0) {
                int toDestroy = Math.max(1, (int)(currentCount * destroyRatio));
                int remaining = currentCount - toDestroy;

                if (remaining > 0) {
                    // ✅ 保留剩余物品，走正常流程产出
                    this.processingStack.setCount(remaining);

                    // 播放销毁音效
                    var selfBE = (BlockEntity)(Object)this;
                    var level = selfBE.getLevel();
                    var pos = selfBE.getBlockPos();
                    if (level != null) {
                        level.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F + level.random.nextFloat() * 0.4F);
                        level.sendBlockUpdated(pos, selfBE.getBlockState(), selfBE.getBlockState(), 3);
                    }
                    selfBE.setChanged();

                    // ✅ 走原逻辑产出剩余物品
                    this.finishProcessing();
                    return;
                }
            }

            // ✅ 只剩 1 个或全部被销毁时，走原销毁逻辑
            this.processingStack = ItemStack.EMPTY;
            this.updateHand();
            this.outputCooldown = 1;

            var selfBE = (BlockEntity)(Object)this;
            var level = selfBE.getLevel();
            var pos = selfBE.getBlockPos();

            if (level != null) {
                level.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F + level.random.nextFloat() * 0.4F);
                level.sendBlockUpdated(pos, selfBE.getBlockState(), selfBE.getBlockState(), 3);
            }
            selfBE.setChanged();
        } else {
            this.finishProcessing();
        }
    }
}