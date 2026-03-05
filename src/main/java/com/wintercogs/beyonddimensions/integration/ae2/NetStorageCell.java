package com.wintercogs.beyonddimensions.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.StorageCell;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class NetStorageCell implements StorageCell
{

    private final UnifiedStorage storage;
    private final KeyCounter snapshot = new KeyCounter();

    private @Nullable AutoCloseable deltaSub = null;
    private @Nullable AutoCloseable anySub = null;

    public NetStorageCell(UnifiedStorage storage)
    {
        this.storage = storage;

        // 初次全量构建
        fullRebuildSnapshot();

        this.deltaSub = storage.subscribeDeltaWeak(this, NetStorageCell::applyDelta);
        this.anySub = storage.subscribeAnyWeak(this, NetStorageCell::fullRebuildSnapshot);
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
        return AEHelper.fromAEKeyToIStack(what)
                .map(stack -> amount - storage.insert(stack, amount, mode.isSimulate()).amount())
                .orElse(0L);
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source)
    {
        return AEHelper.fromAEKeyToIStack(what)
                .map(stack -> storage.extract(stack, amount, mode.isSimulate(), false).amount())
                .orElse(0L);
    }

    @Override
    public void getAvailableStacks(KeyCounter out)
    {
        out.addAll(snapshot);
    }

    @Override
    public Component getDescription()
    {
        return Component.empty();
    }

    // ========== 快照维护 ==========

    /**
     * 增量补丁：O(1) 更新 KeyCounter
     */
    private void applyDelta(IStackKey<?> type, long size, boolean insert)
    {
        Optional<AEKey> keyOpt = AEHelper.fromIStackToAEKey(type);
        if (keyOpt.isEmpty()) return;
        AEKey key = keyOpt.get();

        long cur = snapshot.get(key);
        long next = insert ? (cur + size) : (cur - size);

        if (next > 0)
        {
            snapshot.set(key, next);
        }
        else
        {
            snapshot.remove(key);
        }
    }

    /**
     * 全量重建（仅在绑定/any 兜底时调用）
     */
    private void fullRebuildSnapshot()
    {
        snapshot.clear();
        for (KeyAmount stack : storage.getStorage())
        {
            if (stack.isEmpty()) continue;
            AEHelper.fromIStackToAEKey(stack.key()).ifPresent(aeKey -> {
                snapshot.add(aeKey, stack.amount());
            });
        }
    }


    // 实际无需主动清理，仅保留此方法，该类生命周期会在被移除出驱动器的时候结束
    // 等待UnifiedStorage对其清理后进入GC
    public void close()
    {
        try
        {
            if (deltaSub != null) deltaSub.close();
        }
        catch (Exception ignored)
        {
        }
        try
        {
            if (anySub != null) anySub.close();
        }
        catch (Exception ignored)
        {
        }
        deltaSub = anySub = null;
    }

}
