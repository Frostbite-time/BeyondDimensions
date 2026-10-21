package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.common.item.MatterCompressionBall;

public final class BDStorageRules
{
    private BDStorageRules()
    {
    }

    public static void register()
    {
        // 解压物质球
        MatterCompressionBall.registerStorageRule();
    }
}
