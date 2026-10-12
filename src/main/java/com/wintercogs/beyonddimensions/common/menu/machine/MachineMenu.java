package com.wintercogs.beyonddimensions.common.menu.machine;

import com.wintercogs.beyonddimensions.api.machine.Machine;
import org.jetbrains.annotations.Nullable;

/**
 * 有机器设置页的菜单：设置的同步与修改由 {@link MachineSettingsSync} 统一处理
 */
public interface MachineMenu
{
    /**
     * 服务端：菜单所操作的机器；机器已不存在时为 null
     */
    @Nullable Machine machine();

    /**
     * 客户端：同步来的设置
     */
    MachineSettingsSync.Mirror machineSettings();
}
