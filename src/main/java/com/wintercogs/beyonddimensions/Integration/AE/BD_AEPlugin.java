package com.wintercogs.beyonddimensions.Integration.AE;

import appeng.api.storage.StorageCells;

public class BD_AEPlugin
{
    public static void register()
    {
        // AE的网络仅会在需要时从元件中获取全部信息
        // 配合元件内部的缓存机制，几乎完全抹平了和原生元件的性能差距
        StorageCells.addCellHandler(CellHandler.INSTANCE);
    }
}
