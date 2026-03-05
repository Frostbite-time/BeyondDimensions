package com.wintercogs.beyonddimensions.integration.IFS.Item;

import com.buuz135.industrialforegoingsouls.capabilities.ISoulHandler;
import com.wintercogs.beyonddimensions.integration.IFS.BD_SoulCaps;
import com.wintercogs.beyonddimensions.Item.ModItems;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class WardenSoulTagItem extends Item
{
    public WardenSoulTagItem(Properties properties)
    {
        super(properties);
    }

    // 所有WardenSoulTagItem都指向同一个ISoulHandler，仅发挥其作为标记物品的用处
    public static final ISoulHandler EMPTY_CONTAINER = new ISoulHandler()
    {
        @Override
        public int getSoulTanks()
        {
            return 1;
        }

        @Override
        public int getSoulInTank(int i)
        {
            return 0;
        }

        @Override
        public int getTankCapacity(int i)
        {
            return 0;
        }

        @Override
        public int fill(int i, Action action)
        {
            return 0;
        }

        @Override
        public int drain(int i, Action action)
        {
            return 0;
        }
    };

    public static void registerCapability(RegisterCapabilitiesEvent event)
    {
        event.registerItem(
                BD_SoulCaps.ITEM,
                (stack, ctx) -> WardenSoulTagItem.EMPTY_CONTAINER,
                ModItems.WARDEN_SOUL_TAG_ITEM.get()
        );
    }


}
