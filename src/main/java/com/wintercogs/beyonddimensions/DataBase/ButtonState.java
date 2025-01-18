package com.wintercogs.beyonddimensions.DataBase;

public enum ButtonState {
    ENABLED,        // 按钮启用-用于二值类按钮
    DISABLED,       // 按钮禁用-用于二值类按钮
    //
    SORT_DEFAULT,   // 默认排序-即不修改排序，按ItemStroage本身的存储顺序
    SORT_QUANTITY,  // 数量排序
    SORT_NAME       // 名称排序
}
