package com.wintercogs.beyonddimensions.integration.module.ae2;

import appeng.api.storage.StorageCells;

public class BD_AEPlugin
{
    public static void register()
    {
        StorageCells.addCellHandler(CellHandler.INSTANCE);
    }
}
