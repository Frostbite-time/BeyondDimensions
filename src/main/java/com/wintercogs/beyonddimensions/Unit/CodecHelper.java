package com.wintercogs.beyonddimensions.Unit;

import com.mojang.serialization.Codec;
import net.minecraft.core.NonNullList;

import java.util.function.Function;

public class CodecHelper
{
    public static <E> Codec<NonNullList<E>> nonNullListMutableCodecOf(Codec<E> entryCodec) {
        return entryCodec.listOf().xmap(
                inputList -> {
                    // 创建可修改的空列表
                    NonNullList<E> newList = NonNullList.create();
                    // 使用公开的 add 方法填充元素
                    for (E element : inputList) {
                        newList.add(element);
                    }
                    return newList;
                },
                Function.identity()
        );
    }
}
