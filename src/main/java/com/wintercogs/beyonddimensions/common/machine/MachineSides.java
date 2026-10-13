package com.wintercogs.beyonddimensions.common.machine;

import com.wintercogs.beyonddimensions.api.machine.Machine;
import com.wintercogs.beyonddimensions.api.machine.MachineSetting;
import com.wintercogs.beyonddimensions.api.machine.MachineSettings;
import com.wintercogs.beyonddimensions.api.machine.MachineType;
import com.wintercogs.beyonddimensions.common.block.orientation.FacingRule;
import com.wintercogs.beyonddimensions.common.init.BDMachineSettings;
import com.wintercogs.beyonddimensions.common.machine.setting.SideMode;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 机器的面配置：相对正面的六个面各一项 {@link SideMode} 设置，加上主动弹出（{@link BDMachineSettings#POP}）。
 * 机器类型列出了全部六个面才算有面配置，设置页把六个面合成一个展开图。各面按方块的朝向换算，见 {@link FacingRule}
 */
public final class MachineSides
{
    private static final RelativeSide[] SIDES = RelativeSide.values();

    private MachineSides()
    {
    }

    public static MachineSetting<SideMode> face(RelativeSide side)
    {
        return BDMachineSettings.side(side).get();
    }

    public static boolean isFace(MachineSetting<?> setting)
    {
        for (RelativeSide side : SIDES)
            if (face(side) == setting)
                return true;
        return false;
    }

    public static boolean hasSides(MachineType type)
    {
        for (RelativeSide side : SIDES)
            if (!type.has(face(side)))
                return false;
        return true;
    }

    /**
     * 这项设置是否属于面配置：有面配置的机器的六个面与主动弹出
     */
    public static boolean isSideSetting(MachineType type, MachineSetting<?> setting)
    {
        return (isFace(setting) || setting == BDMachineSettings.POP.get()) && hasSides(type);
    }

    /**
     * 朝 side 的那一面的输入输出；不分面的访问（side 为 null）不受限制
     */
    public static <T extends BlockEntity & Machine> SideMode mode(T machine, @Nullable Direction side)
    {
        if (side == null)
            return SideMode.BOTH;
        return machine.setting(face(RelativeSide.of(FacingRule.orientation(machine.getBlockState()), side)));
    }

    /**
     * 机器镜像后的设置：左右两面互换，仍连着原来的相邻方块
     */
    public static MachineSettings mirrored(Machine machine)
    {
        SideMode left = machine.setting(face(RelativeSide.LEFT));
        SideMode right = machine.setting(face(RelativeSide.RIGHT));
        return machine.settings().with(face(RelativeSide.LEFT), right).with(face(RelativeSide.RIGHT), left);
    }
}
