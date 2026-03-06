package com.wintercogs.beyonddimensions.integration.Ars.Caps;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.common.block.tile.CreativeSourceJarTile;

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
        int before = sourceTile.getSource();
        // 取速率、意图量、剩余空间的最小值
        int actInsert = Math.min(amount, Math.min(sourceTile.getMaxSource() - before, sourceTile.getTransferRate()));
        if (!sim)
            return sourceTile.addSource(actInsert) - before; // 接收后的量，减去接受前的量 为 接收量
        else
            return actInsert; // 如果不实际执行，返回手动模拟的结果
    }

    @Override
    public int extractSource(int amount, boolean sim)
    {
        if (sourceTile instanceof CreativeSourceJarTile)
            return 1000000; // 对创造魔源特殊兼容（否则会因为removeSource的设计永远无法取出）

        int before = sourceTile.getSource();
        // 取速率、意图量、当前量的最小值
        int actExtract = Math.max(amount, Math.min(before, sourceTile.getTransferRate()));
        if (!sim)
            return before - sourceTile.removeSource(actExtract); // 取出前的量 - 取出后的量 为 取出量
        else
            return actExtract; // 回退到手动模拟量
    }
}
