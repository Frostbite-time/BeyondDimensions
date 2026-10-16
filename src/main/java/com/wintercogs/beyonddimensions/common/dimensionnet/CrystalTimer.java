package com.wintercogs.beyonddimensions.common.dimensionnet;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.common.init.BDNetDataTypes;
import com.wintercogs.beyonddimensions.common.init.BDNetTasks;
import com.wintercogs.beyonddimensions.config.ServerConfigRuntime;
import net.minecraft.world.item.ItemStack;

/**
 * 网络生成破碎时空结晶的计时，作为只在内存中的挂接数据 {@link BDNetDataTypes#CRYSTAL_TIMER} 挂在网络上；服务器重启后重新计时
 */
public final class CrystalTimer
{
    private int ticks;

    /**
     * 按配置的间隔为网络生成破碎的时空结晶，间隔为 0 时不生成；由 {@link BDNetTasks} 注册为每刻任务
     */
    public static void generateCrystal(DimensionsNet net)
    {
        int interval = ServerConfigRuntime.crystalGenerateTime;
        if (interval <= 0)
            return;
        if (net.getOrCreateData(BDNetDataTypes.CRYSTAL_TIMER.get()).advance(interval * 20))
        {
            ItemStack stack = new ItemStack(BDItems.SHATTERED_SPACE_TIME_CRYSTALLIZATION.get(), 1);
            net.getUnifiedStorage().insert(new ItemStackKey(stack), stack.getCount(), false);
        }
    }

    // 走过一刻；满 period 刻时归零并返回 true
    private boolean advance(int period)
    {
        if (++ticks < period)
            return false;
        ticks = 0;
        return true;
    }
}
