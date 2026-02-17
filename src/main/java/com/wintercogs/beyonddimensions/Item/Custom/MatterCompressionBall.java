package com.wintercogs.beyonddimensions.Item.Custom;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
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
        return tag != null && tag.contains("StackList", Tag.TAG_LIST) &&
                !tag.getList("StackList", Tag.TAG_COMPOUND).isEmpty();
    }

    public static List<IStackKey<?>> getIStackList(ItemStack stack)
    {
        List<IStackKey<?>> result = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("StackList", Tag.TAG_LIST))
            return result;

        ListTag listTag = tag.getList("StackList", Tag.TAG_COMPOUND);
        for (Tag element : listTag)
        {
            CompoundTag elementTag = (CompoundTag) element;
            IStackKey<?> stackType = IStackKey.deserializeNBTCommon(elementTag);
            if (stackType != null)
            {
                result.add(stackType);
            }
        }
        return result;
    }

    public static void setIStackList(ItemStack stack, List<IStackKey<?>> stackList)
    {
        ListTag listTag = new ListTag();
        for (IStackKey<?> stackType : stackList)
        {
            CompoundTag elementTag = stackType.serializeNBT();
            // 确保序列化后包含类型标识
            if (!elementTag.contains("Type"))
            {
                elementTag.putString("Type", stackType.getTypeId().toString());
            }
            listTag.add(elementTag);
        }

        CompoundTag tag = stack.getOrCreateTag();
        tag.put("StackList", listTag);
    }

}
