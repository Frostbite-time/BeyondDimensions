package com.wintercogs.beyonddimensions.common.block.entity;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.capability.helper.CapabilityHelper;
import com.wintercogs.beyonddimensions.api.util.CapCtx;
import com.wintercogs.beyonddimensions.api.util.USHandler;
import com.wintercogs.beyonddimensions.common.init.BDBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.NotNull;

public class NetPathwayBlockEntity extends NetedBlockEntity
{
    public NetPathwayBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(BDBlockEntities.NET_PATHWAY_BLOCK_ENTITY.get(), pos, blockState);
    }

    //--- 能力注册 (通过事件) ---
    public static void registerCapability(RegisterCapabilitiesEvent event)
    {

        CapabilityHelper.BlockCapabilityMap.forEach(
                (resourceLocation, directionBlockCapability) -> {
                    USHandler handler = CapabilityHelper.USHandlerMap.get(resourceLocation);
                    event.registerBlockEntity(
                            (BlockCapability<? super Object, ? extends Direction>) directionBlockCapability,
                            BDBlockEntities.NET_PATHWAY_BLOCK_ENTITY.get(),
                            (be, side) -> {
                                DimensionsNet net = be.getNet(); // getNet已经在基类中完成缓存处理
                                if (net != null && handler != null)
                                {
                                    if (handler.isContextual())
                                        return handler.apply(net.getUnifiedStorage(), new CapCtx(be.level, be.getBlockPos(), be));
                                    else
                                        return handler.apply(net.getUnifiedStorage(), null);
                                }
                                return null;
                            } // 根据方向返回处理器
                    );
                }
        );
    }

    @Override
    public void invalidateCapabilities()
    {
        super.invalidateCapabilities();
    }

    @Override
    protected void loadAdditional(@NotNull ValueInput input)
    {
        super.loadAdditional(input);
    }

    @Override
    protected void saveAdditional(@NotNull ValueOutput output)
    {
        super.saveAdditional(output);
    }

}
