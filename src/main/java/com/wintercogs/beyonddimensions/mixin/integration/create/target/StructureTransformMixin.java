package com.wintercogs.beyonddimensions.mixin.integration.create.target;

import com.simibubi.create.content.contraptions.StructureTransform;
import com.wintercogs.beyonddimensions.integration.module.create.MachineSideMirroring;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 机械动力 0.5.1 没有方块实体变换的注册表，镜像时在这里交换机器左右两面的配置；6.0 起由 Create6Module 注册，插件不应用这个 Mixin
@Mixin(value = StructureTransform.class, remap = false)
public abstract class StructureTransformMixin
{
    @Shadow
    public Mirror mirror;

    @Inject(method = "apply(Lnet/minecraft/world/level/block/entity/BlockEntity;)V", at = @At("HEAD"))
    private void beyonddimensions$mirrorMachineSides(BlockEntity be, CallbackInfo ci)
    {
        if (be != null && MachineSideMirroring.types().contains(be.getType()))
            MachineSideMirroring.apply(be, mirror);
    }
}
