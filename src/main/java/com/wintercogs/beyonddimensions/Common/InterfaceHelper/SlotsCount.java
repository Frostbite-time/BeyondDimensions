package com.wintercogs.beyonddimensions.Common.InterfaceHelper;

@FunctionalInterface
public interface SlotsCount<T>
{
    int getSlots(T handler);
}
