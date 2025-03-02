package com.wintercogs.beyonddimensions.DataBase.Storage;

import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;

public class FluidStorage implements IFluidHandler
{

    private DimensionsNet net; // 用于通知维度网络进行保存
    // 实际的存储
    private final ArrayList<FluidStack> fluidStorage;


    public FluidStorage(DimensionsNet net)
    {
        this.net = net;
        this.fluidStorage = new ArrayList<>();
    }

    private void OnChange()
    {
        net.setDirty();
    }

    // 将物品存储转换为 NBT 数据
    public CompoundTag serializeNBT(HolderLookup.Provider levelRegistryAccess)
    {
        CompoundTag tag = new CompoundTag();
        ListTag fluidsTag = new ListTag();

        fluidStorage.forEach((fluidStack) ->
        {
            if (fluidStack == null || fluidStack == FluidStack.EMPTY)
            {
                return; // 在此处用于跳过空物品
            }
            CompoundTag fluidTag = new CompoundTag();
            fluidTag.put("FluidStack", fluidStack.save(levelRegistryAccess));
            fluidsTag.add(fluidTag);
        });

        tag.put("Fluids", fluidsTag);
        return tag;
    }

    // 从 NBT 数据加载物品存储
    public void deserializeNBT(HolderLookup.Provider levelRegistryAccess, CompoundTag tag)
    {

        if (tag.contains("Fluids", 9))
        { // 9 表示 ListTag 类型
            ListTag fluidsTag = tag.getList("Fluids", 10); // 10 表示 CompoundTag 类型

            for (int i = 0; i < fluidsTag.size(); i++)
            {
                CompoundTag fluidTag = fluidsTag.getCompound(i);
                FluidStack fluidStack = FluidStack.parseOptional(levelRegistryAccess, fluidTag.getCompound("FluidStack"));
                fill(fluidStack,FluidAction.EXECUTE);
            }
        }
    }

    // 获取储罐总数 储罐类似于物品中的槽位
    @Override
    public int getTanks()
    {
        return fluidStorage.size();
    }

    // 获取储罐所对应的流体堆
    @Override
    public FluidStack getFluidInTank(int tank)
    {
        return fluidStorage.get(tank);
    }

    // 获取对应储罐的最大容量
    @Override
    public int getTankCapacity(int tank)
    {
        return Integer.MAX_VALUE-1;
    }

    // 检查对应储罐是否支持容纳该流体
    @Override
    public boolean isFluidValid(int tank, FluidStack fluidStack)
    {
        return true;
    }

    // 尝试将流体填入储罐 （自动处理要填入哪一个）
    @Override
    public int fill(FluidStack fluidStack, FluidAction fluidAction)
    {
        if(fluidAction.simulate())
        {
            return fluidStack.getAmount();
        }
        else if(fluidAction.execute())
        {
            if (fluidStack.isEmpty())
            {
                return 0;
            }
            // 增加已有物品的数量 添加未有的物品
            for (FluidStack fluidExist : fluidStorage)
            {
                if (FluidStack.isSameFluidSameComponents(fluidExist,fluidStack))
                {
                    fluidExist.grow(fluidStack.getAmount());
                    OnChange();
                    return fluidStack.getAmount();
                }
            }
            fluidStorage.add(fluidStack.copy());
            OnChange();
            return fluidStack.getAmount();
        }
        return 0;
    }


    // 按流体类型排出液体
    @Override
    public FluidStack drain(FluidStack fluidStack, FluidAction fluidAction)
    {
        if (fluidStorage.contains(fluidStack))
        {
            for (FluidStack fluidExist : fluidStorage)
            {
                if (FluidStack.isSameFluidSameComponents(fluidExist,fluidStack))
                {
                    if(fluidAction.simulate())
                    {
                        FluidStack simStack = fluidExist.copy();
                        simStack.shrink(fluidStack.getAmount());
                        if(simStack.getAmount()>=0)
                        {
                            return fluidStack;
                        }
                        else
                        {
                            fluidStack.grow(simStack.getAmount());
                            return fluidStack;
                        }
                    }
                    else
                    {
                        fluidExist.shrink(fluidStack.getAmount());
                        if(fluidExist.getAmount()>=0)
                        {
                            OnChange();
                            return fluidStack;
                        }
                        else
                        {
                            fluidStack.grow(fluidExist.getAmount());
                            fluidStorage.remove(fluidExist);
                            OnChange();
                            return fluidStack;
                        }
                    }
                }
            }
        }
        else
        {
            return FluidStack.EMPTY;
        }
        return FluidStack.EMPTY;
    }

    // 按量排出液体，自动选择流体类型
    @Override
    public FluidStack drain(int count, FluidAction fluidAction)
    {
        FluidStack fluidExist = getFluidInTank(0);
        if(fluidExist.getAmount()>=count)
        {
            if(fluidAction.execute())
            {
                //split将会返回stack被减少数量的那部分，并且stack本身会在操作之后减少对应数量
                FluidStack splitFluid = fluidExist.split(count);
                OnChange();
                return splitFluid;
            }
            else if (fluidAction.simulate())
            {
                FluidStack splitFluid = fluidExist.copy().split(count);
                return splitFluid;
            }
        }
        else
        {
            if (fluidAction.execute())
            {
                fluidStorage.remove(fluidExist);
                OnChange();
                return fluidExist;
            }
            else if (fluidAction.simulate())
            {
                return fluidExist.copy();
            }
        }
        return FluidStack.EMPTY;
    }
}
