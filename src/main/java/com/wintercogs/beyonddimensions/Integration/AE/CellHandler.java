package com.wintercogs.beyonddimensions.Integration.AE;

import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataComponents.ModDataComponents;
import com.wintercogs.beyonddimensions.Item.Custom.NetAEStorageCell;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

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
        if(!itemstack.has(ModDataComponents.NET_ID_DATA))
            return null;
        int netId = itemstack.get(ModDataComponents.NET_ID_DATA);
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
