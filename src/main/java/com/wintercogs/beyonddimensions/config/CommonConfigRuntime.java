package com.wintercogs.beyonddimensions.config;

import com.wintercogs.beyonddimensions.api.ButtonState;

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
    public static boolean emiAllowNetworkStorageInfo;

    public static boolean interfaceCanReceiveResource;
    public static boolean interfaceCanOutputResource;
    public static boolean interfaceCanPopResource;
    public static int interfaceUsableCapacity;
}
