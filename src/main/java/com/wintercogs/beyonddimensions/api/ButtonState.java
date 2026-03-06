package com.wintercogs.beyonddimensions.api;

public enum ButtonState
{
    ENABLED,        // 按钮启用-用于二值类按钮
    DISABLED,       // 按钮禁用-用于二值类按钮
    //-------------------------------------------------------------------
    SORT_DEFAULT,   // 默认排序
    SORT_QUANTITY,  // 数量排序
    SORT_NAME,      // 名称排序
    SORT_MODID,      // 按模组ID排序
    SORT_INSERTED_TIME, // 按插入时间排序
    SORT_MODIFIED_TIME, // 按修改时间排序
}
