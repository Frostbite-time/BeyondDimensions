package com.wintercogs.beyonddimensions.common.machine;

// 红石控制模式
public enum RedStoneControlMode
{
    IGNORE, // 始终工作
    POWERED, // 有信号时工作
    UNPOWERED, // 无信号时工作
    NOT_WORKING // 始终不工作
}
