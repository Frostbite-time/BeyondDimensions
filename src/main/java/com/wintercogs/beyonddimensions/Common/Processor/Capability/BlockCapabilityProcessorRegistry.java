package com.wintercogs.beyonddimensions.Common.Processor.Capability;

import java.util.HashSet;
import java.util.Set;

public class BlockCapabilityProcessorRegistry
{
    private static final Set<IBlockCapabilityProcessor<?>> Processors = new HashSet<>();

    public static void register(IBlockCapabilityProcessor processor)
    {
        Processors.add(processor);
    }
}
