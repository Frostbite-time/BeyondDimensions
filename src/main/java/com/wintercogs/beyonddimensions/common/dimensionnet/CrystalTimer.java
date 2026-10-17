package com.wintercogs.beyonddimensions.common.dimensionnet;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.common.init.BDNetTasks;
import com.wintercogs.beyonddimensions.config.ServerConfigRuntime;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * 网络生成破碎时空结晶的计时：主世界的游戏时间每到配置间隔的整数倍就生成一个，不需要记录任何数据
 */
public final class CrystalTimer
{
    private CrystalTimer()
    {
    }

    /**
     * 按配置的间隔为网络生成破碎的时空结晶，间隔为 0 时不生成；由 {@link BDNetTasks} 注册为每刻任务
     */
    public static void generateCrystal(DimensionsNet net)
    {
        int interval = ServerConfigRuntime.crystalGenerateTime;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        // 冻结（/tick freeze）时游戏时间停住，停在整数倍上会每刻都生成
        if (interval <= 0 || server == null || !server.tickRateManager().runsNormally())
            return;
        if (server.overworld().getGameTime() % (interval * 20L) != 0)
            return;
        ItemStack stack = new ItemStack(BDItems.SHATTERED_SPACE_TIME_CRYSTALLIZATION.get(), 1);
        net.getUnifiedStorage().insert(new ItemStackKey(stack), stack.getCount(), false);
    }
}
