package com.wintercogs.beyonddimensions.Integration.AE;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.StorageCell;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import net.minecraft.network.chat.Component;

public class NetStorageCell implements StorageCell
{
    private final UnifiedStorage storage;

    public NetStorageCell(UnifiedStorage storage)
    {
        this.storage = storage;
    }

    @Override
    public CellState getStatus()
    {
        return CellState.NOT_EMPTY;
    }

    @Override
    public double getIdleDrain()
    {
        return 1;
    }

    @Override
    public boolean canFitInsideCell()
    {
        return true;
    }

    @Override
    public void persist()
    {
        // UnifiedStorage在内部操作完成后会自行通知保存
        // 此处无需处理
    }

    @Override
    public boolean isPreferredStorageFor(AEKey what, IActionSource source)
    {
        return true;
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source)
    {
        return AEHelper.fromAEKeyToIStack(what,amount)
                .map(stack -> amount - storage.insert(stack, mode.isSimulate()).getStackAmount())
                .orElse(0L);
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source)
    {
        return AEHelper.fromAEKeyToIStack(what, amount)
                .map(stack -> storage.extract(stack, mode.isSimulate()).getStackAmount())
                .orElse(0L);
    }

    @Override
    public void getAvailableStacks(KeyCounter out)
    {
        for(IStackType stack : storage.getStorage())
        {
            AEHelper.fromIStackToAEKey(stack).ifPresent(aeKey -> out.add(aeKey,stack.getStackAmount()));
        }
    }

    @Override
    public Component getDescription()
    {
        return Component.empty();
    }

}
