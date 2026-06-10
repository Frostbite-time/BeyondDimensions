package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.common.machine.FuzzyMode;
import com.wintercogs.beyonddimensions.common.machine.PopMode;

public class NetInterfaceSettings
{
    private PopMode popMode = PopMode.STOP;
    private FuzzyMode fuzzyMode = FuzzyMode.DISABLE;

    public PopMode getPopMode()
    {
        return popMode;
    }

    public void setPopMode(PopMode popMode)
    {
        this.popMode = popMode;
    }

    public FuzzyMode getFuzzyMode()
    {
        return fuzzyMode;
    }

    public void setFuzzyMode(FuzzyMode fuzzyMode)
    {
        this.fuzzyMode = fuzzyMode;
    }
}
