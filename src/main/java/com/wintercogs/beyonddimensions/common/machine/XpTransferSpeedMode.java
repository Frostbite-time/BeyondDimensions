package com.wintercogs.beyonddimensions.common.machine;

public enum XpTransferSpeedMode
{
    SLOW, // 1级
    MID,  // 10级
    HIGH, // 30级
    HIGHEST, // 100级
    OVER_HIGHEST; // 150级 适配神化

    public XpTransferSpeedMode next()
    {
        XpTransferSpeedMode[] v = values();
        return v[(this.ordinal() + 1) % v.length];
    }
}
