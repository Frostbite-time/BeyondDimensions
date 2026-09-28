package com.wintercogs.beyonddimensions.config;

import com.wintercogs.beyonddimensions.api.ButtonState;

public final class CommonConfigRuntime
{
    private CommonConfigRuntime()
    {
    }

    public static volatile ButtonState uiSortButton = ButtonState.SORT_NAME;
    public static volatile ButtonState uiSecondSortButton = ButtonState.SORT_INSERTED_TIME;
    public static volatile ButtonState uiReverseButton = ButtonState.DISABLED;
    public static volatile ButtonState uiSearchButton = ButtonState.DISABLED;
    public static volatile ButtonState uiCraftButton = ButtonState.DISABLED;
    public static volatile ButtonState uiCraftReturnButton = ButtonState.DISABLED;
    public static volatile int uiPageNum = 5;
    public static volatile int uiColumns = 9;
    public static volatile String uiSearch = "";
    public static volatile boolean searchTextWithJEIEMI = false;
    public static volatile boolean emiAllowNetworkStorageInfo = false;

    public static volatile boolean interfaceCanReceiveResource = true;
    public static volatile boolean interfaceCanOutputResource = true;
    public static volatile boolean interfaceCanPopResource = true;
    public static volatile int interfaceUsableCapacity = 27;
}
