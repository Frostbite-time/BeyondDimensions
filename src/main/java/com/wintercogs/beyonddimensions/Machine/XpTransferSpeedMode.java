package com.wintercogs.beyonddimensions.Machine;

public enum XpTransferSpeedMode
{
    SLOW, // 1级
    MID,  // 10级
    HIGH, // 30级
    HIGHEST; // 100级

    public XpTransferSpeedMode next() {
        XpTransferSpeedMode[] v = values();
        return v[(this.ordinal() + 1) % v.length];
    }
}
