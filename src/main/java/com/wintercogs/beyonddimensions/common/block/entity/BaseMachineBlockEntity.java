package com.wintercogs.beyonddimensions.common.block.entity;

import com.wintercogs.beyonddimensions.common.machine.BaseMachine;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

// 帮助处理步进时间以及红石控制模式等基本问题，并提供这些信息的基本保存功能
public abstract class BaseMachineBlockEntity extends NetedBlockEntity implements BaseMachine
{
    public BaseMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState)
    {
        super(type, pos, blockState);
    }

    public RedStoneControlMode controlMode = RedStoneControlMode.IGNORE;
    public int stepTick = 0;

    public static void tick(Level level, BlockPos pos, BlockState state, BaseMachineBlockEntity blockEntity)
    {
        if (level.isClientSide())
            return;

        blockEntity.working();
    }

    @Override
    public RedStoneControlMode getControlMode()
    {
        return controlMode;
    }

    @Override
    public boolean hasRedStoneSignal()
    {
        return level.getBestNeighborSignal(worldPosition) > 0;
    }

    @Override
    public int getStepTick()
    {
        return stepTick;
    }

    @Override
    public void setStepTick(int newTick)
    {
        this.stepTick = newTick;
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        this.controlMode = RedStoneControlMode.valueOf(tag.getString("control_mode"));
        this.stepTick = tag.getInt("step_tick");
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.putString("control_mode", controlMode.name());
        tag.putInt("step_tick", stepTick);
    }
}
