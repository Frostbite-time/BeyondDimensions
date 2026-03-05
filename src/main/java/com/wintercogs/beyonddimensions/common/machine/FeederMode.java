package com.wintercogs.beyonddimensions.common.machine;

public enum FeederMode
{
    SATURATION_KEEP, // 始终保持玩家有饱和度
    CRAZY, // 饱食度一旦降低就喂食
    NORMAL, // 饱食度低于一半才喂食
    HUNGER_TO_EAT // 保食度低于10%才喂食
}
