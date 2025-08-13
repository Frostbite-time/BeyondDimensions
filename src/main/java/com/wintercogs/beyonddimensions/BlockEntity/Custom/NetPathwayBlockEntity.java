package com.wintercogs.beyonddimensions.BlockEntity.Custom;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.Registry.CapabilityHelper;
import com.wintercogs.beyonddimensions.Api.Util.CapCtx;
import com.wintercogs.beyonddimensions.Api.Util.USHandler;
import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.Unit.SidedCapId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NetPathwayBlockEntity extends NetedBlockEntity
{
    private final Map<SidedCapId, LazyOptional<?>> caps = new HashMap<>();

    public NetPathwayBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.NET_PATHWAY_BLOCK_ENTITY.get(), pos, blockState);
        addNetChangeTask(this::clearCapCache);
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

                    final SidedCapId capId = new SidedCapId(cap, null); //此处无需面信息
                    if(caps.containsKey(capId) && caps.get(capId).isPresent())
                    {
                        return caps.get(capId).cast();
                    }
                    else
                    {
                        // 从类型映射表中获取对应的处理器构造函数
                        USHandler handler = CapabilityHelper.USHandlerMap.get(entry.getKey());
                        if(handler != null)
                        {
                            Object result;
                            if (handler.isContextual())
                                result = handler.apply(net.getUnifiedStorage(), new CapCtx(level, getBlockPos(), this));
                            else
                                result = handler.apply(net.getUnifiedStorage(), null);

                            if(result != null)
                            {
                                LazyOptional<?> opt = LazyOptional.of(() -> result);
                                // 如果opt存在，则放入缓存
                                caps.put(capId, opt);
                                // opt被无效化时，主动移除引用（此处同时判断值，确保移除的是同一个引用下的内容，至少是完全一致的内容）
                                opt.addListener(lo -> caps.remove(capId,lo));
                                return opt.cast();
                            }
                        }
                        return LazyOptional.empty(); // 无对应handler的回调
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
        clearCapCache();
    }

    private void clearCapCache()
    {
        // 无效化能力并清空map
        var snapshot = new ArrayList<>(caps.values());
        for (var opt : snapshot) { // invalidate时也会尝试移除一次，最后以clear保底
            try { opt.invalidate(); } catch (Throwable ignored) {}
        }
        caps.clear();
    }
}
