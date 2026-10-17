package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.api.dimensionnet.data.NetDataType;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.registry.BDRegistries;
import com.wintercogs.beyonddimensions.common.menu.state.EnergyHistory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * BD 自己挂在维度网络上的数据，由 {@link BDNetTasks} 的每刻任务推进
 */
public final class BDNetDataTypes
{
    public static final DeferredRegister<NetDataType<?>> TYPES = DeferredRegister.create(BDRegistries.NET_DATA_TYPE_KEY, BDConstants.MODID);

    /**
     * 能量的近期记录，供能量通道的界面显示；只在内存中，合并网络时留下原网络的记录
     */
    public static final DeferredHolder<NetDataType<?>, NetDataType<EnergyHistory>> ENERGY_HISTORY = TYPES.register("energy_history",
            () -> NetDataType.builder(EnergyHistory::new).merge((kept, absorbed) -> kept).build());

    private BDNetDataTypes()
    {
    }

    public static void register(IEventBus modBus)
    {
        TYPES.register(modBus);
    }
}
