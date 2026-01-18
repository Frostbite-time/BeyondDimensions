package com.wintercogs.beyonddimensions.Api.config;

import com.wintercogs.beyonddimensions.Api.DataBase.ButtonState;

public final class CommonConfigRuntime
{
    private CommonConfigRuntime()
    {
    }

    public static ButtonState uiSortButton;
    public static ButtonState uiSecondSortButton;
    public static ButtonState uiReverseButton;
    public static ButtonState uiSearchButton;
    public static ButtonState uiCraftButton;
    public static ButtonState uiCraftReturnButton;
    public static int uiPageNum;
    public static String uiSearch;
    public static boolean searchTextWithJEIEMI;

    public static boolean interfaceCanReceiveResource;
    public static boolean interfaceCanOutputResource;
    public static boolean interfaceCanPopResource;
    public static int interfaceUsableCapacity;
}