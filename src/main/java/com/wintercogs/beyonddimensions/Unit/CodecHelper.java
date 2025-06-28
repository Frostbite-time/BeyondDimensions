package com.wintercogs.beyonddimensions.Unit;

import com.mojang.serialization.Codec;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.core.NonNullList;

import java.util.function.Function;

public class CodecHelper
{
    public static <E> Codec<NonNullList<E>> nonNullListMutableCodecOf(Codec<E> entryCodec, E defaultValue) {
        return entryCodec.listOf().xmap(
                inputList -> {
                    // 创建可修改的空列表
                    NonNullList<E> newList = NonNullList.withSize(inputList.size(), defaultValue);
                    // 使用公开的 add 方法填充元素
                    int index = 0;
                    for (E element : inputList) {
                        newList.set(index,element); // 调用内部效果
                        index++;
                    }
                    return newList;
                },
                Function.identity()
        );
    }
}
