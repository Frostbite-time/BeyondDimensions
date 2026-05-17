package com.wintercogs.beyonddimensions.util;

import com.mojang.serialization.Codec;
import net.minecraft.core.NonNullList;

import java.util.function.Function;

public class CodecHelper
{
    public static <E> Codec<NonNullList<E>> nonNullListMutableCodecOf(Codec<E> entryCodec, E defaultValue)
    {
        return entryCodec.listOf().xmap(
                inputList -> {
                    NonNullList<E> newList = NonNullList.withSize(inputList.size(), defaultValue);
                    int index = 0;
                    for (E element : inputList)
                    {
                        newList.set(index, element);
                        index++;
                    }
                    return newList;
                },
                Function.identity()
        );
    }
}
