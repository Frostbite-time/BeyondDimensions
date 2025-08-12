package com.wintercogs.beyonddimensions.Integration.Ars.Caps;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;

public class BlockSourceAdp implements ISourceCap
{
    // 方块实体
    private ISourceTile sourceTile;

    public BlockSourceAdp(ISourceTile sourceTile)
    {
        this.sourceTile = sourceTile;
    }


    @Override
    public boolean canAcceptSource(int amount)
    {
        return sourceTile.canAcceptSource();
    }

    @Override
    public boolean canProvideSource(int amount)
    {
        return sourceTile.getSource() > 0;
    }

    @Override
    public int getMaxExtract()
    {
        return sourceTile.getTransferRate();
    }

    @Override
    public int getMaxReceive()
    {
        return sourceTile.getTransferRate();
    }

    @Override
    public int getSource()
    {
        return sourceTile.getSource();
    }

    @Override
    public int getSourceCapacity()
    {
        return sourceTile.getMaxSource();
    }

    @Override
    public void setSource(int amount)
    {
        sourceTile.setSource(amount);
    }

    @Override
    public void setMaxSource(int amount)
    {
        sourceTile.setMaxSource(amount);
    }

    @Override
    public int receiveSource(int amount, boolean sim)
    {
        int actInsert = Math.min(amount,Math.min(sourceTile.getMaxSource() - sourceTile.getSource(), sourceTile.getTransferRate()));
        if(!sim)
            return sourceTile.addSource(actInsert);
        else
            return actInsert;
    }

    @Override
    public int extractSource(int amount, boolean sim)
    {
        int actExtract = Math.max(amount,Math.min(sourceTile.getSource(),sourceTile.getTransferRate()));
        if(!sim)
            return sourceTile.removeSource(actExtract);
        else
            return actExtract;
    }
}
