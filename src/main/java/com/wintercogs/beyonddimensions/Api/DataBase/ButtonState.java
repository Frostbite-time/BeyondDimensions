package com.wintercogs.beyonddimensions.Api.DataBase;

public enum ButtonState
{
    ENABLED,        // 按钮启用-用于二值类按钮
    DISABLED,       // 按钮禁用-用于二值类按钮
    //-------------------------------------------------------------------
    SORT_DEFAULT,   // 默认排序-即不修改排序，按堆叠存入的时间顺序
    SORT_QUANTITY,  // 数量排序
    SORT_NAME,       // 名称排序
    SORT_MODID      // 按模组ID排序
}
