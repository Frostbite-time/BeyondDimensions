package com.wintercogs.beyonddimensions.BlockEntity.Custom;

import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.Unit.CapabilityHelper;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;

import java.util.Map;
import java.util.function.Function;

public class NetPathwayBlockEntity extends NetedBlockEntity
{
    public NetPathwayBlockEntity() {
        super();
    }

    // 1.12.2 能力检测方法
    @Override
    public boolean hasCapability(Capability<?> cap, EnumFacing side) {
        // 先检查是否支持该能力
        DimensionsNet net = this.getNet();
        if(net != null)
        {
            for (Map.Entry<ResourceLocation, Capability<?>> entry : CapabilityHelper.BlockCapabilityMap.entrySet()) {
                if (entry.getValue() == cap) {
                    return true;
                }
            }
        }
        return super.hasCapability(cap, side);
    }
    // 1.12.2 能力获取方法
    @Override
    public <T> T getCapability(Capability<T> cap, EnumFacing side) {
        DimensionsNet net = this.getNet();
        if (net != null) {
            // 遍历自定义能力映射表
            for (Map.Entry<ResourceLocation, Capability<?>> entry : CapabilityHelper.BlockCapabilityMap.entrySet()) {
                if (entry.getValue() == cap) {
                    // 获取处理器构造器
                    Function<UnifiedStorage, Object> handlerConstructor = UnifiedStorage.typedHandlerMap.get(entry.getKey());
                    if (handlerConstructor != null) {
                        Object handler = handlerConstructor.apply(net.getUnifiedStorage());
                        return (T) handler; // 直接返回实例，无需 LazyOptional
                    }
                }
            }
        }
        return super.getCapability(cap, side);
    }
    // 判断是否支持该能力
    private boolean isCapabilitySupported(Capability<?> cap) {
        return CapabilityHelper.BlockCapabilityMap.values().stream()
                .anyMatch(c -> c == cap);
    }

}
