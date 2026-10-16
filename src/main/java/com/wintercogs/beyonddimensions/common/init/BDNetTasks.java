package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.dimensionnet.NetTickTasks;
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
        NetTickTasks.register(BeyondDimensions.makeId("shattered_crystal"), CrystalTimer::generateCrystal);
    }
}
