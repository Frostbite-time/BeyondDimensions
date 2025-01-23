package com.wintercogs.beyonddimensions.DataBase;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

// 作为一个可以存储无限数量的 ItemStack
public class StoredItemStack
{
    private ItemStack itemStack;
    private long count;

    public static final StreamCodec<RegistryFriendlyByteBuf,StoredItemStack> STREAM_CODEC = storedItemStack_StreamCodec();

    public StoredItemStack(ItemStack itemStack)
    {
        this.itemStack = itemStack.copy();
        this.itemStack.setCount(1);
        this.count = 1;
    }

    public StoredItemStack(ItemStack itemStack, long count)
    {
        this.itemStack = itemStack.copy();
        this.itemStack.setCount(1);
        this.count = count;
    }


    public ItemStack getItemStack()
    {
        return itemStack;
    }

    public long getCount()
    {
        return count;
    }

    public void addCount(long num)
    {
        this.count += num;
    }

    public void subCount(long num)
    {
        this.count -= num;
        if (this.count <= 0)
        {
            this.count = 0;
        }
    }


    public ItemStack getActualStack()
    {
        ItemStack s = itemStack.copy();
        s.setCount((int) count);
        return s;
    }

    public ItemStack getVanillaMaxSizeStack()
    {
        ItemStack s = itemStack.copy();
        s.setCount(s.getMaxStackSize());
        return s;
    }


    // 用于比较2个StoredItemStack储存的是否是完全一致的物品  不检查数量
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        StoredItemStack other = (StoredItemStack) obj;
        if (itemStack == null)
        {
            if (other.itemStack != null) return false;
        }
        else if (!ItemStack.isSameItemSameComponents(itemStack, other.itemStack)) return false;
        return true;
    }

    // 不检查数量的比较
    public boolean equals(StoredItemStack other)
    {
        if (this == other) return true;
        if (other == null) return false;
        if (itemStack == null)
        {
            if (other.itemStack != null) return false;
        }
        else if (!ItemStack.isSameItemSameComponents(itemStack, other.itemStack)) return false;
        return true;
    }


    // 编码器，用于进行网络传输
    static StreamCodec<RegistryFriendlyByteBuf, StoredItemStack> storedItemStack_StreamCodec()
    {
        return new StreamCodec<RegistryFriendlyByteBuf, StoredItemStack>() {
            public StoredItemStack decode(RegistryFriendlyByteBuf buf) {
                long count = buf.readLong();
                ItemStack itemStack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                return new StoredItemStack(itemStack,count);
            }

            public void encode(RegistryFriendlyByteBuf buf, StoredItemStack storedItemStack) {
                // 先写入 count（物品的数量）
                buf.writeLong(storedItemStack.count);
                // 然后序列化 ItemStack 对象
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, storedItemStack.getItemStack());
            }
        };
    }

    @Override
    public int hashCode()
    {
        return ItemStack.hashItemAndComponents(this.itemStack);
    }
}
