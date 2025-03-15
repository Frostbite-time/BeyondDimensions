package com.wintercogs.beyonddimensions.Common.InterfaceHelper;

@FunctionalInterface
public interface InsertAction<T>
{
    long insert(T handler, int slot, Object stack, boolean simulate);
}
