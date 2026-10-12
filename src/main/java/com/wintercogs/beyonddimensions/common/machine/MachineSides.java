package com.wintercogs.beyonddimensions.common.machine;

import com.wintercogs.beyonddimensions.api.machine.Machine;
import com.wintercogs.beyonddimensions.api.machine.MachineSetting;
import com.wintercogs.beyonddimensions.api.machine.MachineSettings;
import com.wintercogs.beyonddimensions.api.machine.MachineType;
import com.wintercogs.beyonddimensions.common.init.BDMachineSettings;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.function.UnaryOperator;

/**
 * 机器的面配置：六个面各一项 {@link SideMode} 设置，加上主动弹出（{@link BDMachineSettings#POP}）。
 * 机器类型列出了全部六个面才算有面配置，这些设置在面配置页上编辑，不出现在设置页
 */
public final class MachineSides
{
    private static final Direction[] DIRECTIONS = Direction.values();

    private MachineSides()
    {
    }

    public static MachineSetting<SideMode> face(Direction side)
    {
        return BDMachineSettings.side(side).get();
    }

    public static boolean isFace(MachineSetting<?> setting)
    {
        for (Direction side : DIRECTIONS)
            if (face(side) == setting)
                return true;
        return false;
    }

    public static boolean hasSides(MachineType type)
    {
        for (Direction side : DIRECTIONS)
            if (!type.has(face(side)))
                return false;
        return true;
    }

    /**
     * 这项设置是否在面配置页上编辑
     */
    public static boolean onSidePage(MachineType type, MachineSetting<?> setting)
    {
        return (isFace(setting) || setting == BDMachineSettings.POP.get()) && hasSides(type);
    }

    /**
     * 朝 side 的那一面的输入输出；不分面的访问（side 为 null）不受限制
     */
    public static SideMode mode(Machine machine, @Nullable Direction side)
    {
        return side == null ? SideMode.BOTH : machine.setting(face(side));
    }

    /**
     * 方块被旋转或镜像后的面配置：原来朝 side 的面改为朝 transform(side)
     */
    public static MachineSettings transformed(MachineSettings settings, UnaryOperator<Direction> transform)
    {
        MachineSettings next = settings;
        for (Direction side : DIRECTIONS)
            next = next.with(face(transform.apply(side)), settings.get(face(side)));
        return next;
    }
}
