package com.wintercogs.beyonddimensions.Block.BlockEntity.Custom;

import com.wintercogs.beyonddimensions.Block.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.ItemStackHandler;

public class NetPathwayBlockEntity extends NetedBlockEntity
{

    public NetPathwayBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.NET_PATHWAY_BLOCK_ENTITY.get(), pos, blockState);
    }

    //--- 能力注册 (通过事件) ---
    public static void registerCapability(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK, // 标准物品能力
                ModBlockEntities.NET_PATHWAY_BLOCK_ENTITY.get(),
                (be, side) -> {
                    if(be.getNetId()<0)
                    {
                        return new ItemStackHandler(0);
                    }
                    DimensionsNet net = be.getNet();
                    if(net != null)
                    {
                        return net.getItemStorage();
                    }
                    return new ItemStackHandler(0);
                } // 根据方向返回处理器
        );
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
