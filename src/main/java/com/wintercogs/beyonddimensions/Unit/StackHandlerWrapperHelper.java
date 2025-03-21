package com.wintercogs.beyonddimensions.Unit;

import com.wintercogs.beyonddimensions.DataBase.StackHandlerWrapper.IStackHandlerWrapper;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class StackHandlerWrapperHelper
{
    public static final Map<ResourceLocation, Function<?,IStackHandlerWrapper<?>>> stackWrappers = new HashMap<>();

}
