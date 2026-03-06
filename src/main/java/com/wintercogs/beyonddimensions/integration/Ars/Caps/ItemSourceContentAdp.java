package com.wintercogs.beyonddimensions.integration.Ars.Caps;

import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class ItemSourceContentAdp implements ISourceCap
{
    private @Nullable SourceStorage sourceStorage = null; // 用于适配器转接

    public ItemSourceContentAdp(ItemStack stack)
    {
        if (stack.getItem() == BlockRegistry.SOURCE_JAR.asItem())// 普通魔源罐
        {
            this.sourceStorage = new SourceStorage(10000, 10000)
            {
                @Override
                public int receiveSource(int toReceive, boolean simulate)
                {
                    int result = super.receiveSource(toReceive, simulate);
                    ItemSourceContentAdp.setSource(stack, getSource());
                    return result;
                }

                @Override
                public int extractSource(int toExtract, boolean simulate)
                {
                    int result = super.extractSource(toExtract, simulate);
                    ItemSourceContentAdp.setSource(stack, getSource());
                    return result;
                }

                @Override
                public int getSource()
                {
                    return super.getSource();
                }
            };
            this.sourceStorage.setSource(ItemSourceContentAdp.getSource(stack));
        }

        else if (stack.getItem() == BlockRegistry.CREATIVE_SOURCE_JAR.asItem())// 创造魔源罐
            this.sourceStorage = new SourceStorage(1000000, 1000000, 1000000, 1000000)
            {
                public int receiveSource(int toReceive, boolean simulate)
                {
                    return toReceive;
                }

                public int extractSource(int toExtract, boolean simulate)
                {
                    return toExtract;
                }

                public int getSource()
                {
                    return 1000000;
                }
            };
    }

    @Override
    public boolean canAcceptSource(int amount)
    {
        return sourceStorage != null && sourceStorage.canAcceptSource(amount);
    }

    @Override
    public boolean canProvideSource(int amount)
    {
        return sourceStorage != null && sourceStorage.canProvideSource(amount);
    }

    @Override
    public int getMaxExtract()
    {
        return sourceStorage != null ? sourceStorage.getMaxExtract() : 0;
    }

    @Override
    public int getMaxReceive()
    {
        return sourceStorage != null ? sourceStorage.getMaxReceive() : 0;
    }

    @Override
    public boolean canExtract()
    {
        return sourceStorage != null && sourceStorage.canExtract();
    }

    @Override
    public boolean canReceive()
    {
        return sourceStorage != null && sourceStorage.canReceive();
    }

    @Override
    public int getSource()
    {
        return sourceStorage != null ? sourceStorage.getSource() : 0;
    }

    @Override
    public int getSourceCapacity()
    {
        return sourceStorage != null ? sourceStorage.getSourceCapacity() : 0;
    }

    @Override
    public int getMaxSource()
    {
        return sourceStorage != null ? sourceStorage.getMaxSource() : 0;
    }

    @Override
    public void setSource(int amount)
    {
        if (sourceStorage != null)
            sourceStorage.setSource(amount);
    }

    // 跳过
    @Override
    public void setMaxSource(int amount)
    {
        if (sourceStorage != null)
            sourceStorage.setMaxSource(amount);
    }

    @Override
    public int receiveSource(int amount, boolean sim)
    {
        return sourceStorage != null ? sourceStorage.receiveSource(amount, sim) : 0;
    }

    @Override
    public int extractSource(int amount, boolean sim)
    {
        return sourceStorage != null ? sourceStorage.extractSource(amount, sim) : 0;
    }

    // 用于获取并设置物品内部魔源
    public static int getSource(ItemStack stack)
    {
        if (stack.isEmpty()) return 0;
        CompoundTag bet = stack.getTagElement("BlockEntityTag");
        return bet != null && bet.contains("source", Tag.TAG_INT) ? bet.getInt("source") : 0;
    }

    public static void setSource(ItemStack stack, int value)
    {
        if (stack.isEmpty()) return;
        CompoundTag bet = stack.getOrCreateTagElement("BlockEntityTag");
        bet.putInt("source", Math.max(0, value));
    }
}
