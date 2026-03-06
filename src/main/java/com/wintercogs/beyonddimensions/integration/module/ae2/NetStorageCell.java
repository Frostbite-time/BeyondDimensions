package com.wintercogs.beyonddimensions.integration.module.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.StorageCell;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public class NetStorageCell implements StorageCell
{

    private final UnifiedStorage storage;

    // ---- 缓存快照 ----
    private final KeyCounter snapshot = new KeyCounter();

    // 订阅句柄
    private AutoCloseable deltaSub = null;
    private AutoCloseable anySub = null;

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
        return true; // 允许其放入其他存储元件，因为此物品本身极其轻量，仅仅作为一个转接器
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
     * 全量重建
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
