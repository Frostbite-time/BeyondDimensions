package com.wintercogs.beyonddimensions.Api.Registry;

import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.IStackHandlerWrapper;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * stackWrappers用于将不同类型的外部接口包装为IStackHandlerWrapper，使超越维度能始终将可操作的外界容器视为IStackHandlerWrapper，降低与不同模组间的交互成本
 */
public class StackHandlerWrapperHelper
{
    // 资源类型 -> 包装器
    public static final Map<Identifier, Function<?, IStackHandlerWrapper<?>>> stackWrappers = new HashMap<>();
}
