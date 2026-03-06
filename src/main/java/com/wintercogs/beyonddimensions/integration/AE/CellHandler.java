package com.wintercogs.beyonddimensions.integration.AE;

import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.integration.AE.Item.NetAEStorageCell;
import com.wintercogs.beyonddimensions.common.item.NetedItem;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class CellHandler implements ICellHandler
{
    public static final CellHandler INSTANCE = new CellHandler();

    @Override
    public boolean isCell(ItemStack itemstack)
    {
        return itemstack.getItem() instanceof NetAEStorageCell;
    }

    // host用于通知存储已被更变
    @Override
    public @Nullable StorageCell getCellInventory(ItemStack itemstack, @Nullable ISaveProvider host)
    {
        if (NetedItem.getNetId(itemstack) < 0)
            return null;
        int netId = NetedItem.getNetId(itemstack);
        if (netId >= 0)
        {
            DimensionsNet net = DimensionsNet.getNetFromId(netId);
            if (net != null)
            {
                return new NetStorageCell(net.getUnifiedStorage());
            }
        }
        return null;
    }
}
