package com.wintercogs.beyonddimensions.Fluid.Custom;

import net.minecraftforge.fluids.ForgeFlowingFluid;

public abstract class XpFluid extends ForgeFlowingFluid
{
    protected XpFluid(ForgeFlowingFluid.Properties properties)
    {
        super(properties);
    }

    public static class Source extends ForgeFlowingFluid.Source
    {
        public Source(ForgeFlowingFluid.Properties properties)
        {
            super(properties);
        }
    }

    public static class Flowing extends ForgeFlowingFluid.Flowing
    {
        public Flowing(ForgeFlowingFluid.Properties properties)
        {
            super(properties);
        }
    }
}