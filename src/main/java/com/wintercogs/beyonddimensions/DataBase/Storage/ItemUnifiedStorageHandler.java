package com.wintercogs.beyonddimensions.DataBase.Storage;

import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

// 以IStackType为基础实现IItemHandler的类
public class ItemUnifiedStorageHandler implements IItemHandler
{
    private UnifiedStorage storage;

    public ItemUnifiedStorageHandler(UnifiedStorage storage) {
        this.storage = storage;
    }


    @Override
    public int getSlots()
    {
        // 默认返回长度比实际大1，可以让其他模组不因为存储长度而无法插入物品
        // 此类封装性良好，只需内部方法对使用的索引进行二次检查，即可避免NPE问题
        // 最后，UnifiedStorage实际并无槽位数限制且自动合并同类物品，除了读取信息和提取指定槽位物品都无需索引参与，对于超出索引的读取返回EMPTY即可
        // 所以，这样做是安全的
        return storage.getTypeIdIndexList(ItemStackType.ID)
                .map(list -> list.size()+1)
                .orElse(1);
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        // 此处的slot参数是基于特化类型ItemStackType的索引
        return storage.getTypeIdIndexList(ItemStackType.ID)
                .filter(slots -> slot>=0 && slot<slots.size())
                .map(slots -> slots.get(slot))
                .filter(actualIndex -> actualIndex>=0)
                .map(actualIndex -> (ItemStackType)storage.getStackBySlot(actualIndex))
                .map(ItemStackType::getStack)
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack itemStack, boolean sim)
    {
        ItemStackType typedStack = (ItemStackType) storage.insert(new ItemStackType(itemStack),sim);
        return typedStack.getStack();
    }

    @Override
    public ItemStack extractItem(int slot, int count, boolean sim)
    {
        // 这个调用链分为三步
        // 1.专为物品提供的假列表中获取指定物品并转为IStackType
        // 2.使用存储器导出
        // 3.获取返回值的Stack，然后转为ItemStack再返回
        return (ItemStack) storage.extract(new ItemStackType(getStackInSlot(slot).copyWithCount(count)),sim)
                .getStack();
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return BDMath.clampLongToInt(storage.getSlotCapacity(0));
    }

    @Override
    public boolean isItemValid(int slot, ItemStack itemStack)
    {
        return true;
    }
}
