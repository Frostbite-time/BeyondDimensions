package com.wintercogs.beyonddimensions.Integration.Ars.Block;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.api.source.SourceManager;
import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.SourceStackKey;
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
        return getNet() != null ? Integer.MAX_VALUE : 0;
    }

    @Override
    public boolean canAcceptSource()
    {
        return getNet() != null;
    }

    @Override
    public boolean canProvideSource()
    {
        return getNet() != null;
    }

    @Override
    public int getSource()
    {
        DimensionsNet net = getNet();
        if (net != null)
        {
            KeyAmount stack = net.getUnifiedStorage().getStackByKey(SourceStackKey.INSTANCE);
            if (stack.key() == SourceStackKey.INSTANCE)
            {
                return BDMath.clampLongToInt(stack.amount());
            }
        }
        return 0;
    }

    @Override
    public int getMaxSource()
    {
        return Integer.MAX_VALUE;
    }

    // 拒绝set，防止内部情况被改变，此处直接返回当前容量
    // 此外，新生魔艺确实未对外使用set
    // 返回值为设置后魔源量，此处不设置，直接返回
    @Override
    public int setSource(int amount)
    {
        return getSource();
    }

    // 返回增加的
    @Override
    public int addSource(int amount, boolean simulate)
    {
        DimensionsNet net = getNet();
        if (net != null)
        {
            // 此处转换安全
            return amount - (int) net.getUnifiedStorage().insert(SourceStackKey.INSTANCE, amount, simulate).amount();
        }
        return 0;
    }

    @Override
    public int addSource(int amount)
    {
        return addSource(amount, false);
    }

    @Override
    public int removeSource(int amount)
    {
        return removeSource(amount, false);
    }

    // 返回导出的
    @Override
    public int removeSource(int amount, boolean simulate)
    {
        DimensionsNet net = getNet();
        if (net != null)
        {
            // 此处转换安全
            return (int) net.getUnifiedStorage().extract(SourceStackKey.INSTANCE, amount, simulate, false).amount();
        }
        return 0;
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        if (!level.isClientSide() && getNet() != null)
        {
            SourceManager.INSTANCE.addInterface(level, new SourcePathwayProvider(this));
        }

    }

    @Override
    public void setChanged()
    {
        super.setChanged(); // 防止level触发NPE
        if (level != null && !level.isClientSide() && getNet() != null)
            SourceManager.INSTANCE.addInterface(level, new SourcePathwayProvider(this));
    }
}
