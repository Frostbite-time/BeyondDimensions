package com.wintercogs.beyonddimensions.BlockEntity.Custom;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.Api.Registry.CapabilityHelper;
import com.wintercogs.beyonddimensions.Api.Util.CapCtx;
import com.wintercogs.beyonddimensions.Api.Util.CommonHandler;
import com.wintercogs.beyonddimensions.Api.Util.USHandler;
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
                    USHandler handler = CapabilityHelper.USHandlerMap.get(entry.getKey());

                    if(handler != null)
                    {
                        Object result;
                        if (handler.isContextual())
                            result = handler.apply(net.getUnifiedStorage(), new CapCtx(level, getBlockPos(), side, this));
                        else
                            result = handler.apply(net.getUnifiedStorage(), null);
                        return LazyOptional.of(() -> result).cast();
                    }
                    return LazyOptional.empty(); // 无对应handler的回调
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
