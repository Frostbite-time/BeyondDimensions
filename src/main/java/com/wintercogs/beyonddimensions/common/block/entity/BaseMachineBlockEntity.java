package com.wintercogs.beyonddimensions.common.block.entity;

import com.wintercogs.beyonddimensions.api.machine.Machine;
import com.wintercogs.beyonddimensions.api.machine.MachineSetting;
import com.wintercogs.beyonddimensions.api.machine.MachineSettings;
import com.wintercogs.beyonddimensions.api.machine.MachineType;
import com.wintercogs.beyonddimensions.common.init.BDMachineSettings;
import com.wintercogs.beyonddimensions.common.machine.BaseMachine;
import com.wintercogs.beyonddimensions.common.machine.setting.RedStoneControlMode;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 方块机器：步进时间与红石控制，设置按机器类型统一保存（{@link Machine}），配置卡与设置页都经它读写
 */
public abstract class BaseMachineBlockEntity extends NetedBlockEntity implements BaseMachine, Machine
{
    public BaseMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState)
    {
        super(type, pos, blockState);
    }

    private MachineSettings settings = MachineSettings.EMPTY;
    public int stepTick = 0;

    public static void tick(Level level, BlockPos pos, BlockState state, BaseMachineBlockEntity blockEntity)
    {
        if (level.isClientSide())
            return;

        blockEntity.working();
    }

    @Override
    public MachineSettings settings()
    {
        return settings;
    }

    @Override
    public void setSettings(MachineSettings settings)
    {
        if (this.settings.equals(settings))
            return;
        this.settings = settings;
        onSettingsChanged();
    }

    /**
     * 设置改变之后标记保存；子类可以追加，例如刷新能力
     */
    protected void onSettingsChanged()
    {
        setChanged();
    }

    /**
     * 方块被扳手转了朝向：各面的输入输出随之换了方向，与设置改变一样处理
     */
    public void onFacingChanged()
    {
        onSettingsChanged();
    }

    @Override
    public RedStoneControlMode getControlMode()
    {
        return machineType().has(BDMachineSettings.REDSTONE.get()) ? setting(BDMachineSettings.REDSTONE.get()) : RedStoneControlMode.IGNORE;
    }

    public void setControlMode(RedStoneControlMode controlMode)
    {
        setSetting(BDMachineSettings.REDSTONE.get(), controlMode);
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
        this.settings = readSettings(machineType(), tag);
        this.stepTick = tag.getInt("step_tick");
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.put(SETTINGS, settings.toTag());
        tag.putInt("step_tick", stepTick);
    }

    /**
     * 读出机器的设置：新存档在 settings 下，旧存档按设置登记的旧写法读出
     */
    public static MachineSettings readSettings(MachineType type, CompoundTag tag)
    {
        if (tag.contains(SETTINGS, Tag.TAG_COMPOUND))
            return MachineSettings.fromTag(tag.getCompound(SETTINGS));
        MachineSettings legacy = MachineSettings.EMPTY;
        for (MachineType.Entry<?> entry : type.settings())
            legacy = readLegacy(entry.setting(), tag, legacy);
        return legacy;
    }

    private static <T> MachineSettings readLegacy(MachineSetting<T> setting, CompoundTag tag, MachineSettings into)
    {
        return setting.readLegacy(tag).map(value -> into.with(setting, value)).orElse(into);
    }
}
