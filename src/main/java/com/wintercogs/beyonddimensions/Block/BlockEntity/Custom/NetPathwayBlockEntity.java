package com.wintercogs.beyonddimensions.Block.BlockEntity.Custom;

import com.wintercogs.beyonddimensions.Block.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.DataBase.DimensionsItemStorage;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class NetPathwayBlockEntity extends BlockEntity {

    // 存储接口所对应的维度网络id，用于和维度网络交互
    private int netId = -1; // 初始化为-1，表示未绑定
    DimensionsItemStorage itemStorage;


    public NetPathwayBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.NET_PATHWAY_BLOCK_ENTITY.get(), pos, blockState);
    }

    public int getNetId()
    {
        return this.netId;
    }

    public void setNetId(int netId)
    {
        this.netId = netId;
        setChanged();
    }

    //--- 能力注册 (通过事件) ---
    public static void registerCapability(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK, // 标准物品能力
                ModBlockEntities.NET_PATHWAY_BLOCK_ENTITY.get(),
                (be, side) -> {
                    if(be.getNetId()<0)
                    {
                        return null;
                    }
                    DimensionsNet net = DimensionsNet.getNetFromId(be.getNetId(), be.getLevel());
                    return net.getItemStorage();
                } // 根据方向返回处理器
        );
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag,registries);
        this.netId = tag.getInt("netId");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.putInt("netId",this.netId);
    }

}
