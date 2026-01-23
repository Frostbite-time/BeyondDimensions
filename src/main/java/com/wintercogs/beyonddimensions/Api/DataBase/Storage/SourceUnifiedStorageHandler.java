package com.wintercogs.beyonddimensions.Api.DataBase.Storage;


import com.hollingsworth.arsnouveau.api.source.ISourceCap;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.SourceStackKey;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Unit.BDMath;

// 对ISourceCap的适配器，由于ISource类本身注释不完善，方法实现具体使用按照SourceStorage的实现和注释推测
public class SourceUnifiedStorageHandler implements ISourceCap
{
    private UnifiedStorage storage;

    public SourceUnifiedStorageHandler(UnifiedStorage storage)
    {
        this.storage = storage;
    }

    // 能否接收指定数量的魔源？SourceStorage的实现是能接收哪怕一点就算成功，因此，此处模仿其实现
    @Override
    public boolean canAcceptSource(int amount)
    {
        return receiveSource(amount, true) > 0;
    }

    // 能否提供指定数量的魔源？SourceStorage的实现是能提取哪怕一点就算成功
    @Override
    public boolean canProvideSource(int amount)
    {
        return extractSource(amount, true) > 0;
    }

    // 每次操作量-可以提取多少
    @Override
    public int getMaxExtract()
    {
        return Integer.MAX_VALUE;
    }

    // 每次操作量-可以接收多少
    @Override
    public int getMaxReceive()
    {
        return Integer.MAX_VALUE;
    }

    // 获取当前魔源量
    @Override
    public int getSource()
    {
        return BDMath.clampLongToInt(storage.getStackByKey(SourceStackKey.INSTANCE).amount());
    }

    // 获取魔源容量，用于getMaxSource即可
    @Override
    public int getSourceCapacity()
    {
        return BDMath.clampLongToInt(storage.getSlotCapacity(0));
    }

    // 返回最大可容纳数
    @Override
    public int getMaxSource()
    {
        return getSourceCapacity();
    }

    // 能否提取？不重视内部状态
    @Override
    public boolean canExtract()
    {
        return true;
    }

    // 能否接收？不重视内部状态
    @Override
    public boolean canReceive()
    {
        return true;
    }

    // 强行设置容器内魔源数量，但不超过最大值（我真的不想实现这个接口，这会使最大容量被限制在int中，但是好在新生魔艺并未对外使用过此方法）
    @Override
    public void setSource(int amount)
    {
        int wanted = BDMath.clampLongToInt(storage.getSlotCapacity(0));
        long actualInside = getSource();

        long operation = wanted - actualInside;
        BeyondDimensions.LOGGER.info("某个网络的魔源数量被外界强行设置，可能导致错误，最终魔源数量被设置为：{}", operation);
        if (operation > 0)
            storage.insert(SourceStackKey.INSTANCE, operation, false);
        else
            storage.extract(SourceStackKey.INSTANCE, -operation, false, false);
    }

    // 强行设置最大值，不生效，因为UnifiedStorage的容量仅由容器决定（新生魔艺并未使用过这个方法，可以放心）
    @Override
    public void setMaxSource(int max)
    {

    }

    // 返回接受量
    @Override
    public int receiveSource(int amount, boolean sim)
    {
        return (int) (amount - storage.insert(SourceStackKey.INSTANCE, amount, sim).amount());
    }

    // 返回导出量
    @Override
    public int extractSource(int amount, boolean sim)
    {
        return (int) storage.extract(SourceStackKey.INSTANCE, amount, sim, false).amount();
    }
}
