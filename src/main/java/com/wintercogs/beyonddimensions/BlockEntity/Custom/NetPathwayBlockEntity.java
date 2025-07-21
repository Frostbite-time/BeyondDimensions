package com.wintercogs.beyonddimensions.BlockEntity.Custom;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.Api.Registry.CapabilityHelper;
import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import java.util.Map;
import java.util.function.Function;

public class NetPathwayBlockEntity extends NetedBlockEntity
{
    public NetPathwayBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.NET_PATHWAY_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side)
    {
        DimensionsNet net = this.getNet();
        if(net != null)
        {
            // 遍历注册的能力映射表
            for (Map.Entry<ResourceLocation, Capability<?>> entry : CapabilityHelper.BlockCapabilityMap.entrySet()) {
                // 检查当前请求的能力是否匹配注册的能力
                if (entry.getValue() == cap) {
                    // 从类型映射表中获取对应的处理器构造函数
                    Function<UnifiedStorage,Object> handlerConstructor = UnifiedStorage.typedHandlerMap.get(entry.getKey());

                    if (handlerConstructor != null) {
                        // 创建处理器实例并转换为请求的能力类型
                        Object handler = handlerConstructor.apply(net.getUnifiedStorage());
                        // 安全类型转换后包装为 LazyOptional
                        return LazyOptional.of(() -> handler).cast();
                    }
                }
            }
        }

        // 未找到匹配能力则调用父类实现
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps()
    {
        super.invalidateCaps();

    }



}
