package com.wintercogs.beyonddimensions.mixin.integration.create.target;

import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.wintercogs.beyonddimensions.integration.module.create.block.entity.SchematicannonPathWayBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 一个仅作通知用途的mixin
@Mixin(value = SchematicannonBlockEntity.class, remap = false)
public abstract class SchematicannonBlockEntityMixin extends BlockEntity
{
    private SchematicannonBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState blockState)
    {
        super(type, pos, blockState);
    }

    @Inject(
            method = "updateChecklist",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/schematics/cannon/SchematicannonBlockEntity;findInventories()V"
            )
    )
    private void beyonddimensions$refreshPathwayBeforeInventoryScan(CallbackInfo ci)
    {
        Level level = getLevel();
        if (level == null) return;

        SchematicannonPathWayBlockEntity.updateAdjacentPathways(level, getBlockPos());
    }
}
