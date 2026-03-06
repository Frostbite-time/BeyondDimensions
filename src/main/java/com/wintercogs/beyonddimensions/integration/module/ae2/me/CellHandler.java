package com.wintercogs.beyonddimensions.integration.module.ae2.me;

import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.integration.module.ae2.item.NetAEStorageCell;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class CellHandler implements ICellHandler
{
    public static final CellHandler INSTANCE = new CellHandler();

    @Override
    public boolean isCell(ItemStack itemstack)
    {
        return itemstack.getItem() instanceof NetAEStorageCell;
    }

    // host用于通知存储已被更变，我们不需要
    @Override
    public @Nullable StorageCell getCellInventory(ItemStack itemstack, @Nullable ISaveProvider host)
    {
        if (!itemstack.has(BDDataComponents.NET_ID_DATA))
            return null;

        int netId = itemstack.getOrDefault(BDDataComponents.NET_ID_DATA, -1);
        if (netId < 0) return null;

        DimensionsNet net = DimensionsNet.getNetFromId(netId);
        if (net == null) return null;

        return new NetStorageCell(net.getUnifiedStorage());
    }
}
