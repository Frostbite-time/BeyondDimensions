package com.wintercogs.beyonddimensions.integration.module.ifs.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.integration.module.ifs.item.WardenSoulTagItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class IFSModuleItems
{
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BDConstants.MODID);

    /**
     * 工业先锋：灵魂涌动联动 - 灵魂标记器
     */
    public static final DeferredItem<Item> WARDEN_SOUL_TAG_ITEM = ITEMS.register("warden_soul_tag_item",
            () -> new WardenSoulTagItem(new Item.Properties())
    );

    public static void register(IEventBus eventBus)
    {
        ITEMS.register(eventBus);
    }
}
