package com.wintercogs.beyonddimensions.config;

import com.wintercogs.beyonddimensions.api.ButtonState;

public final class CommonConfigRuntime
{
    private CommonConfigRuntime()
    {
    }

    public static ButtonState uiSortButton = ButtonState.SORT_NAME;
    public static ButtonState uiSecondSortButton = ButtonState.SORT_INSERTED_TIME;
    public static ButtonState uiReverseButton = ButtonState.DISABLED;
    public static ButtonState uiSearchButton = ButtonState.DISABLED;
    public static ButtonState uiCraftButton = ButtonState.DISABLED;
    public static ButtonState uiCraftReturnButton = ButtonState.DISABLED;
    public static int uiPageNum = 5;
    public static String uiSearch = "";
    public static boolean searchTextWithJEIEMI = false;
    public static boolean emiAllowNetworkStorageInfo = false;

    public static boolean interfaceCanReceiveResource = true;
    public static boolean interfaceCanOutputResource = true;
    public static boolean interfaceCanPopResource = true;
    public static int interfaceUsableCapacity = 27;
}