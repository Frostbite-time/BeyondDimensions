package com.wintercogs.beyonddimensions.BlockEntity.Custom;

import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.Storage.ItemStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Storage.TypedHandlerManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.function.Function;

public class NetPathwayBlockEntity extends NetedBlockEntity
{
    public NetPathwayBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.NET_PATHWAY_BLOCK_ENTITY.get(), pos, blockState);
    }

    //--- 能力注册 (通过事件) ---
    public static void registerCapability(RegisterCapabilitiesEvent event) {
        TypedHandlerManager.CapHandlerMap.forEach(
                (cap,handlerF)->{
                    event.registerBlockEntity(
                            (BlockCapability<? super Object, ? extends Direction>) cap, // 标准物品能力
                            ModBlockEntities.NET_PATHWAY_BLOCK_ENTITY.get(),
                            (be, side) -> {
                                if(be.getNetId()<0)
                                {
                                    return null;
                                }
                                DimensionsNet net = be.getNet();
                                if(net != null)
                                {
                                    // 能力系统自带缓存能力，使用new不会有多少性能损耗
                                    //return new ItemStackTypedHandler(net);
                                    Function handler = TypedHandlerManager.getHandler(cap,DimensionsNet.class);
                                    return handler.apply(net);
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
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag,registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
    }

}
