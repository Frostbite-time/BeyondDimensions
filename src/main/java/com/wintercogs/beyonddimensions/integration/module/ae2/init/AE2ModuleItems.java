package com.wintercogs.beyonddimensions.integration.module.ae2.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.ids.BDItemIds;
import com.wintercogs.beyonddimensions.integration.module.ae2.item.NetAEStorageCell;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class AE2ModuleItems
{
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BDConstants.MODID);

    public static final RegistryObject<Item> NET_AE_STORAGE_CELL = ITEMS.register(BDItemIds.NET_AE_STORAGE_CELL,
            () -> new NetAEStorageCell(new Item.Properties())
    );

    public static void register(IEventBus eventBus)
    {
        ITEMS.register(eventBus);
    }
}
