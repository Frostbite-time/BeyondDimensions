package com.wintercogs.beyonddimensions.Menu.Slot;

import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.StackCreater;
import com.wintercogs.beyonddimensions.DataBase.Storage.UnifiedStorage;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class StoredStackSlot extends Slot
{
    // 一个空容器，仅用于欺骗父类构造函数，实际存储使用StoredItemStack并结合index
    private static final Container empty_inv = new SimpleContainer(0);
    private final UnifiedStorage unifiedStorage;
    private int theSlot;
    private boolean fake;

    // 简介思路：构建一个slot，使用index结合DimensionsItemStorage中的列表来管理自身对应物品
    // 为此，需要重写网络沟通方案，将DimensionsItemStorage作为原inv，StoredItemStack作为原ItemStack来进行数据同步

    public StoredStackSlot(UnifiedStorage unifiedStorage, int slotIndex, int xPosition, int yPosition)
    {
        super(empty_inv, slotIndex, xPosition, yPosition);
        this.theSlot = slotIndex;
        this.unifiedStorage = unifiedStorage;
    }

    public IStackType getTypedStackFromUnifiedStorage()
    {
        IStackType stackType = unifiedStorage.getStackByIndex(getSlotIndex());
        if(stackType != null)
            return stackType.copy();
        else
            return new ItemStackType();
    }

    public ItemStack getItemStackFromUnifiedStorage()
    {
        //从当前槽索引取物品
        IStackType stackType = unifiedStorage.getStackByIndex(getSlotIndex());
        if(stackType == null)
        {
            return ItemStack.EMPTY;
        }

        if(stackType instanceof ItemStackType itemStackType)
        {
            return itemStackType.getStack();
        }
        else
        {
            return ItemStack.EMPTY;
        }
    }

    // 获取不超过原版最大堆叠数的Stack，一般仅用于GUI类，可以保留Item实现
    public IStackType getVanillaActualStack()
    {
        //从当前槽索引取物品
        IStackType stack = getTypedStackFromUnifiedStorage();
        if (stack.isEmpty())
            return stack;
        if (stack != null)
        {
            if(stack.getStackAmount()>stack.getVanillaMaxStackSize())
            {
                return stack.copyWithCount(stack.getVanillaMaxStackSize());
            }
            else
            {
                return stack.copy();
            }

        }
        return new ItemStackType();
    }

    // 获取原版最大堆叠数的Stack，一般仅用于GUI类，可以保留Item实现
    public IStackType getVanillaMaxSizeStack()
    {
        //从当前槽索引取物品
        IStackType stack = getTypedStackFromUnifiedStorage();
        if (stack.isEmpty())
            return stack;
        if (stack != null)
        {
            return stack.copyWithCount(stack.getVanillaMaxStackSize());
        }
        return new ItemStackType();
    }

    public IStackType getStack()
    {
        if(getSlotIndex()<0)
        {
            return new ItemStackType(ItemStack.EMPTY);
        }
        //从当前槽索引取物品
        IStackType stack = unifiedStorage.getStackByIndex(getSlotIndex());
        if (stack.isEmpty())
            return StackCreater.CreateEmpty(stack.getTypeId());
        if (stack != null)
        {   //使用getActualStack将当前的真正总数返回，可以确保显示数量的正确
            return stack.copy();
        }
        return new ItemStackType(ItemStack.EMPTY);
    }


    // 以下这些重写 覆盖了slot中最基本的要素，以便将Container驱动的inv系统，替换成DimensionsItemStorage驱动
    @Override
    public ItemStack getItem()
    {
        if(getSlotIndex()<0)
        {
            return ItemStack.EMPTY;
        }
        //从当前槽索引取物品
        ItemStack itemStack = getItemStackFromUnifiedStorage();
        if (itemStack.isEmpty())
            return ItemStack.EMPTY;
        if (itemStack != null)
        {   //使用getActualStack将当前的真正总数返回，可以确保显示数量的正确
            return itemStack.copy();
        }
        return ItemStack.EMPTY;

    }

    @Override
    public boolean hasItem()
    {
        //检查当前槽是否为空
        return unifiedStorage.getStackByIndex(getSlotIndex()) != null
                && !unifiedStorage.getStackByIndex(getSlotIndex()).isEmpty();
    }

    @Override
    public void set(ItemStack stack)
    {
        if (stack == ItemStack.EMPTY || stack == null || getSlotIndex() <0)
            return;
        // 当尝试用一个物品真正覆盖这个槽内容会发生什么
        // 如果索引不存在，使用add自增长，如果存在，直接替换
        if (unifiedStorage.getStorage().size() > getSlotIndex())
            unifiedStorage.getStorage().set(getSlotIndex(), new ItemStackType(stack.copy()));
        else if(unifiedStorage.getStorage().size() == getSlotIndex())
            unifiedStorage.getStorage().add(getSlotIndex(), new ItemStackType(stack.copy()));
        else
        {
            // 将size到Index-1之间的位置填充为空，然后填充Index位置
            // 扩展列表直到 targetIndex - 1，并填充 null
            while (unifiedStorage.getStorage().size() < getSlotIndex()) {
                unifiedStorage.getStorage().add(new ItemStackType(ItemStack.EMPTY));  // 填充空值
            }
            unifiedStorage.getStorage().add(getSlotIndex(), new ItemStackType(stack.copy()));
        }


        this.setChanged();
    }

    @Override
    public void setByPlayer(ItemStack newStack, ItemStack oldStack)
    {
        // 当玩家拿着物品点击这个槽会发生什么
        unifiedStorage.insert(new ItemStackType(newStack.copy()), false);
        this.setChanged();
    }


    @Override
    public void setChanged()
    {
        // 重要函数，确保存储被修改后net能被设定为脏数据保存
        this.unifiedStorage.onChange();
    }

    @Override
    public int getMaxStackSize()
    {
        // 获取槽位可存储物品的最大值
        return Integer.MAX_VALUE - 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack)
    {
        // 获取槽位可存储物品的最大值
        return Integer.MAX_VALUE - 1;
    }

    @Override
    public ItemStack remove(int amount)
    {
        if (getItem() == ItemStack.EMPTY || getItem() == null)
        {
            return ItemStack.EMPTY;
        }
        // 从当前槽位移除对应数量的物品 并返回被移除的物品总数
        IStackType typedStack = unifiedStorage.extract(getSlotIndex(), amount,true);
        if (typedStack instanceof ItemStackType)
        {
            ItemStackType trueExtract = (ItemStackType) unifiedStorage.extract(getSlotIndex(), amount,false);
            return trueExtract.copyStack();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotIndex()
    {
        return this.theSlot;
    }

    @Override
    public boolean isSameInventory(Slot other)
    {
        if (other instanceof StoredStackSlot)
        {
            // 比较二者是否是同一个引用  或许以后可以用其他更注重数据的方式比较？
            return this.unifiedStorage == ((StoredStackSlot) other).unifiedStorage;
        }
        return false;
    }

    @Override
    public ItemStack safeInsert(ItemStack stack, int increment)
    {
        // 如果传入的物品不是空气，也可以放入，则放入物品后返回剩余
        // 否则直接返回物品
        if (!stack.isEmpty() && this.mayPlace(stack))
        {
            ItemStack itemstack = this.getItem();
            int i = Math.min(Math.min(increment, stack.getCount()), this.getMaxStackSize(stack) - itemstack.getCount());
            this.setByPlayer(stack.split(i));
        }
        return stack;
    }

    @Override
    public int getContainerSlot()
    {
        return this.theSlot;
    }

    public void setTheSlotIndex(int index)
    {
        this.theSlot = index;
    }

    public long getItemCount()
    {
        if(getSlotIndex()<0)
        {
            return -1;
        }
        //从当前槽索引取物品
        IStackType stack = unifiedStorage.getStackByIndex(getSlotIndex());
        if (stack != null && !stack.isEmpty())
        {   //使用getActualStack将当前的真正总数返回，可以确保显示数量的正确
            return stack.getStackAmount();
        }
        return -1;
    }

    @Override
    public boolean isFake()
    {
        return fake;
    }
}
