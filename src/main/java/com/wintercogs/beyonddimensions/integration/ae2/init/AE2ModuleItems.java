package com.wintercogs.beyonddimensions.integration.ae2.init;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.integration.ae2.item.NetAEStorageCell;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AE2ModuleItems
{
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BeyondDimensions.MODID);

    public static final DeferredItem<Item> NET_AE_STORAGE_CELL = ITEMS.register("net_ae_storage_cell",
            () -> new NetAEStorageCell(new Item.Properties())
    );

    public static void register(IEventBus eventBus)
    {
        ITEMS.register(eventBus);
    }
}
