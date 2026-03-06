package com.wintercogs.beyonddimensions.integration.AE;

import appeng.api.storage.StorageCells;

public class BD_AEPlugin
{
    public static void register()
    {
        StorageCells.addCellHandler(CellHandler.INSTANCE);
    }
}
