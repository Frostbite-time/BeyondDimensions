package com.wintercogs.beyonddimensions.common.machine;

public enum HopperRangeMode
{
    RADIUS_LOWEST, // 半径1格
    RADIUS_LOW,    // 半径三格
    RADIUS_MID,    // 半径五格
    RADIUS_HIGH,   // 半径七格
    RADIUS_HIGHEST,// 半径十格
    CHUNK_MODE     // 当前所在区块，启用此模式时注意降低轮询频率
}
