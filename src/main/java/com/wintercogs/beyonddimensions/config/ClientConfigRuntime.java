package com.wintercogs.beyonddimensions.config;

import com.wintercogs.beyonddimensions.common.menu.widget.StorageSort;

public final class ClientConfigRuntime
{
    private ClientConfigRuntime()
    {
    }

    public static volatile boolean disableMultiNetworkSwitching = false;

    // 存储界面
    public static volatile StorageSort storageSort = StorageSort.NAME;
    public static volatile boolean storageUseSecondarySort = true;
    public static volatile StorageSort storageSecondarySort = StorageSort.INSERTED_TIME;
    public static volatile boolean storageReverse = false;
    public static volatile int storageRows = 5;
    public static volatile int storageColumns = 9;
    public static volatile boolean storageKeepSearch = false;
    public static volatile String storageSearch = "";
    public static volatile boolean storageOpenCrafting = false;
    public static volatile boolean storageRightClickMenu = true;

    // 合成区
    public static volatile boolean craftingReturnToStorage = false;
    public static volatile boolean craftingKeepGrid = false;

    // 配方查看器
    public static volatile boolean recipeViewerSyncSearch = true;
    public static volatile boolean recipeViewerEmiNetworkContents = false;
}
