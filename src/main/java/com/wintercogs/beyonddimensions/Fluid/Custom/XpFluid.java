package com.wintercogs.beyonddimensions.Fluid.Custom;

import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public abstract class XpFluid extends BaseFlowingFluid
{
    protected XpFluid(Properties properties)
    {
        super(properties);
    }

    public static class Source extends BaseFlowingFluid.Source
    {
        public Source(Properties properties)
        {
            super(properties);
        }
    }

    public static class Flowing extends BaseFlowingFluid.Flowing
    {
        public Flowing(Properties properties)
        {
            super(properties);
        }
    }
}
