package com.wintercogs.beyonddimensions.integration.module.ae2.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.ids.BDItemIds;
import com.wintercogs.beyonddimensions.integration.module.ae2.item.NetAEStorageCell;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AE2ModuleItems
{
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BDConstants.MODID);


    /**
     * AE存储磁盘
     */
    public static final DeferredItem<Item> NET_AE_STORAGE_CELL = ITEMS.register(BDItemIds.NET_AE_STORAGE_CELL,
            () -> new NetAEStorageCell(new Item.Properties())
    );


    public static void register(IEventBus eventBus)
    {
        ITEMS.register(eventBus);
    }
}
