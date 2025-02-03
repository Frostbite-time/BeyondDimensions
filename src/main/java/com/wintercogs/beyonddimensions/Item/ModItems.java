package com.wintercogs.beyonddimensions.Item;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Item.Custom.NetCreater;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems
{
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BeyondDimensions.MODID);

    public static final DeferredItem<Item> NET_CREATER = ITEMS.register("net_creater",
            () -> new NetCreater(new Item.Properties()));



    public static void register(IEventBus eventBus)
    {
        ITEMS.register(eventBus);
    }
}
