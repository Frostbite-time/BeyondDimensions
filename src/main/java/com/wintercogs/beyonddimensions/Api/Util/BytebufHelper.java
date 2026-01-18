package com.wintercogs.beyonddimensions.Api.Util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class BytebufHelper
{
    // 向FriendlyByteBuf写入物品信息
    // 用于避开某些模组对网络读写的注入-这真的让人头疼
    public static void writeItemBuf(FriendlyByteBuf buf, ItemStack stack)
    {
        if (stack.isEmpty())
        {
            buf.writeBoolean(false);
        }
        else
        {
            buf.writeBoolean(true);
            Item item = stack.getItem();
            buf.writeId(BuiltInRegistries.ITEM, item);
            buf.writeByte(stack.getCount());
            CompoundTag compoundtag = null;
            if (item.isDamageable(stack) || item.shouldOverrideMultiplayerNbt())
            {
                compoundtag = stack.tag;
            }

            buf.writeNbt(compoundtag);
        }
    }

    // 用于FriendlyByteBuf读取物品，同时防止自动添加damage等耐久值
    // 比起buf自带的函数，这取消了nbt验证以及耐久度验证，以确保网络传输时的数据本身的完整
    public static ItemStack readItemBuf(FriendlyByteBuf buf)
    {
        if (!buf.readBoolean())
        {
            return ItemStack.EMPTY;
        }
        else
        {
            Item item = (Item) buf.readById(BuiltInRegistries.ITEM);
            int num = buf.readByte();
            ItemStack itemstack = new ItemStack(item, num);
            itemstack.tag = buf.readNbt();
            return itemstack;
        }
    }
}
