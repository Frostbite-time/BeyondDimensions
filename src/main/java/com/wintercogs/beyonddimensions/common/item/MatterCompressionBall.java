package com.wintercogs.beyonddimensions.common.item;

import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MatterCompressionBall extends Item
{
    public MatterCompressionBall(Properties properties)
    {
        super(properties);
    }

    public static boolean hasIStackList(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains("stack_list", Tag.TAG_LIST) &&
                !tag.getList("stack_list", Tag.TAG_COMPOUND).isEmpty();
    }

    public static List<KeyAmount> getIStackList(ItemStack stack)
    {
        List<KeyAmount> result = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("stack_list", Tag.TAG_LIST))
            return result;

        ListTag listTag = tag.getList("stack_list", Tag.TAG_COMPOUND);
        for (Tag element : listTag)
        {
            CompoundTag elementTag = (CompoundTag) element;
            KeyAmount stackType = KeyAmount.deserializeNBT(elementTag);
            result.add(stackType);
        }
        return result;
    }

    public static void setIStackList(ItemStack stack, List<KeyAmount> stackList)
    {
        ListTag listTag = new ListTag();
        for (KeyAmount stackType : stackList)
        {
            CompoundTag elementTag = KeyAmount.serializeNBT(stackType);
            listTag.add(elementTag);
        }

        CompoundTag tag = stack.getOrCreateTag();
        tag.put("stack_list", listTag);
    }

}
