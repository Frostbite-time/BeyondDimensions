package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.api.menu.workbench.StorageWorkbenchType;
import com.wintercogs.beyonddimensions.api.menu.workbench.StorageWorkbenchTypes;
import com.wintercogs.beyonddimensions.common.menu.widget.AnvilWidget;
import com.wintercogs.beyonddimensions.common.menu.widget.CraftingWidget;
import com.wintercogs.beyonddimensions.common.menu.widget.SmithingWidget;
import com.wintercogs.beyonddimensions.common.menu.widget.StonecutterWidget;

/**
 * BD 自带的工作台：合成台、铁砧、锻造台与切石机，总能使用。它们的界面在客户端的 BDWorkbenches 里注册
 */
public final class BDWorkbenchTypes
{
    private BDWorkbenchTypes()
    {
    }

    public static void register()
    {
        StorageWorkbenchTypes.register(StorageWorkbenchTypes.CRAFTING, StorageWorkbenchType.of(menu -> new CraftingWidget(menu, menu.storage)));
        StorageWorkbenchTypes.register(StorageWorkbenchTypes.ANVIL, StorageWorkbenchType.of(menu -> new AnvilWidget(menu, menu.storage)));
        StorageWorkbenchTypes.register(StorageWorkbenchTypes.SMITHING, StorageWorkbenchType.of(menu -> new SmithingWidget(menu, menu.storage)));
        StorageWorkbenchTypes.register(StorageWorkbenchTypes.STONECUTTER, StorageWorkbenchType.of(menu -> new StonecutterWidget(menu, menu.storage)));
    }
}
