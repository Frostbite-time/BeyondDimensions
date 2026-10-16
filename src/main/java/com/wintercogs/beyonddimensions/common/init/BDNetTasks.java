package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.dimensionnet.NetTickTasks;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey;
import com.wintercogs.beyonddimensions.common.dimensionnet.CrystalTimer;

/**
 * BD 自己在每个维度网络每刻运行的任务
 */
public final class BDNetTasks
{
    private BDNetTasks()
    {
    }

    public static void register()
    {
        // 记下网络的能量存量，供能量通道的界面显示
        NetTickTasks.register(BeyondDimensions.makeId("energy_history"), net -> net.getOrCreateData(BDNetDataTypes.ENERGY_HISTORY.get())
                .record(net.getUnifiedStorage().getStackByKey(EnergyStackKey.INSTANCE).amount()));
        NetTickTasks.register(BeyondDimensions.makeId("shattered_crystal"), CrystalTimer::generateCrystal);
    }
}
