package com.wintercogs.beyonddimensions.Integration.AE;

import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Item.Custom.NetAEStorageCell;
import com.wintercogs.beyonddimensions.Item.Custom.NetedItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.ServerLifecycleHooks;

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
        if(NetedItem.getNetId(itemstack) < 0)
            return null;
        int netId = NetedItem.getNetId(itemstack);
        if(netId >=0)
        {
            DimensionsNet net = DimensionsNet.getNetFromId(netId, ServerLifecycleHooks.getCurrentServer().overworld());
            if(net != null)
            {
                return new NetStorageCell(net.getUnifiedStorage());
            }
        }
        return null;
    }
}
