package com.wintercogs.beyonddimensions.Integration.Ars.Block;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.api.source.SourceManager;
import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.SourceStackType;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetedBlockEntity;
import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.Integration.Ars.Caps.SourcePathwayProvider;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class SourcePathwayBlockEntity extends NetedBlockEntity implements ISourceTile
{
    public SourcePathwayBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(ModBlockEntities.ARS_SOURCE_PATHWAY_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public int getTransferRate()
    {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean canAcceptSource()
    {
        return true;
    }

    @Override
    public int getSource()
    {
        DimensionsNet net = getNet();
        if(net != null)
        {
            IStackType stack = net.getUnifiedStorage().getStackByStack(new SourceStackType(0));
            if(stack != null)
            {
                return BDMath.clampLongToInt(stack.getStackAmount());
            }
        }
        return 0;
    }

    @Override
    public int getMaxSource()
    {
        return Integer.MAX_VALUE;
    }

    @Override
    public void setMaxSource(int amount)
    {

    }

    // 拒绝set，防止内部情况被改变
    // 此外，新生魔艺确实未对外使用set
    @Override
    public int setSource(int amount)
    {
        return 0;
    }

    // 返回增加的
    @Override
    public int addSource(int amount)
    {
        DimensionsNet net = getNet();
        if (net != null)
        {
            // 此处转换安全
            return amount - (int) net.getUnifiedStorage().insert(new SourceStackType(amount), false).getStackAmount();
        }
        return 0;
    }

    // 返回导出的
    @Override
    public int removeSource(int amount)
    {
        DimensionsNet net = getNet();
        if(net != null)
        {
            // 此处转换安全
            return (int) net.getUnifiedStorage().extract(new SourceStackType(amount),false).getStackAmount();
        }
        return 0;
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        if(!level.isClientSide() && getNet() != null)
        {
            SourceManager.INSTANCE.addInterface(level,new SourcePathwayProvider(this));
        }

    }

    @Override
    public void setChanged()
    {
        super.setChanged();
        if(!level.isClientSide() && getNet() != null)
            SourceManager.INSTANCE.addInterface(level,new SourcePathwayProvider(this));
    }
}
