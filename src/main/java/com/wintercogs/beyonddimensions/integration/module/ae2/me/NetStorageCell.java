package com.wintercogs.beyonddimensions.integration.module.ae2.me;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.StorageCell;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.integration.module.ae2.AEHelper;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public class NetStorageCell implements StorageCell
{

    private final UnifiedStorage storage;

    // ---- 缓存快照 ----
    private final KeyCounter snapshot = new KeyCounter();

    // 订阅句柄（弱订阅已可自动清理，但保留以便需要时主动关闭）
    private AutoCloseable deltaSub = null;
    private AutoCloseable anySub = null;

    public NetStorageCell(UnifiedStorage storage)
    {
        this.storage = storage;

        // 初次全量构建
        fullRebuildSnapshot();

        // 订阅增量（弱订阅，不捕获强引用到 this）
        this.deltaSub = storage.subscribeDeltaWeak(this, (self, type, size, insert) -> {
            self.applyDelta(type, size, insert);
        });

        // 订阅 any（兜底：仅当拿不到明细时，做一次全量重建）
        this.anySub = storage.subscribeAnyWeak(this, self -> self.fullRebuildSnapshot());
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
     * 增量补丁：O(1) 更新 KeyCounter（避免留下 0 项）
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
            // set 比 add/remove 更直接，且不会留下 0 项
            snapshot.set(key, next);
        }
        else
        {
            // 删除该键
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
