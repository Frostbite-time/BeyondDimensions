package com.wintercogs.beyonddimensions.integration.module.rs.storage;

import com.refinedmods.refinedstorage.api.storage.cache.IStorageCache;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.integration.module.rs.RSHelper;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

/**
 * 维护“已上报给 RS 网络缓存(FluidStorageCache)的流体快照”。
 *
 * <p>结构与 {@link BD_RS120ExternalStorageItemsMirror} 完全对称，
 * 不再向缓存推送“全量基线”，而是以本地快照为基准只推送差量，并令快照与推送保持 lockstep。</p>
 */
public class BD_RS120ExternalStorageFluidsMirror
{
    private final Object2IntOpenHashMap<FluidKey> indexByKey = new Object2IntOpenHashMap<>();
    private final ArrayList<FluidKey> keys = new ArrayList<>();
    private final ArrayList<FluidStack> all = new ArrayList<>();

    private static final class Delta
    {
        final FluidStack key; // amount=1
        final long diff;

        Delta(FluidStack key, long diff)
        {
            this.key = key;
            this.diff = diff;
        }
    }

    private final Deque<Delta> deltaQueue = new ArrayDeque<>();

    // 整体重同步标记：绑定/解绑/AnyChange 时置位，flush 时对 live 目标做差量
    private volatile boolean needResync = false;

    public BD_RS120ExternalStorageFluidsMirror()
    {
        indexByKey.defaultReturnValue(-1);
    }

    public List<FluidStack> getAllView()
    {
        return Collections.unmodifiableList(all);
    }

    /**
     * 硬复位：仅清空本地快照与队列，不向缓存推送任何变更。
     */
    public void clearAll()
    {
        indexByKey.clear();
        keys.clear();
        all.clear();
        deltaQueue.clear();
        needResync = false;
    }

    /**
     * 请求一次整体差量重同步（在下一次 flush 时针对 live 目标执行）。
     */
    public void requestResync()
    {
        needResync = true;
    }

    /**
     * 日常增量：仅入队。真正应用到快照与推送缓存都发生在 {@link #flushToRsCache}（保持 lockstep）。
     */
    public void onDelta(FluidStack keyStack, long diff)
    {
        if (diff == 0) return;
        if (keyStack.isEmpty()) return;

        FluidStack proto = keyStack.copy();
        proto.setAmount(1);

        deltaQueue.addLast(new Delta(proto, diff));
    }

    /**
     * 把已入队的增量 / 整体重同步统一冲入 RS cache。
     *
     * @param filter ctx.acceptsFluid，保证 RS 侧只看见允许的流体
     * @param target 当前绑定的 live unified（null 表示已解绑，目标为空）
     */
    public void flushToRsCache(IStorageCache<FluidStack> cache, Predicate<FluidStack> filter, @Nullable UnifiedStorage target)
    {
        if (needResync)
        {
            needResync = false;
            deltaQueue.clear(); // 整体差量会覆盖此前的增量
            resyncDiff(cache, filter, target);
            return;
        }

        boolean changed = false;
        Delta d;
        while ((d = deltaQueue.pollFirst()) != null)
        {
            // 先更新本地快照，得到“实际生效的有符号增量”，再据此推送，确保 lockstep
            long applied = applyDeltaToView(d.key, d.diff);
            if (applied == 0) continue;
            if (!filter.test(d.key)) continue;

            if (applied > 0) pushAdd(cache, d.key, applied);
            else pushRemove(cache, d.key, -applied);
            changed = true;
        }
        if (changed) cache.flush();
    }

    /**
     * 对 live 目标与当前快照做差量推送，并令快照=目标。
     * 仅推送“差额”，因此不会与 getStacks() 的全量重建叠加成双倍。
     */
    private void resyncDiff(IStorageCache<FluidStack> cache, Predicate<FluidStack> filter, @Nullable UnifiedStorage target)
    {
        // 1) 构建目标快照：key -> 原型(amount 已 clamp 为目标数量)
        LinkedHashMap<FluidKey, FluidStack> targetMap = new LinkedHashMap<>();
        if (target != null)
        {
            for (KeyAmount ka : target.getStorage())
            {
                if (ka == null || ka.isEmpty()) continue;
                RSHelper.fromIStackToFluidStack(ka).ifPresent(fs -> {
                    if (fs.isEmpty()) return;
                    long amt = ka.amount();
                    FluidKey key = FluidKey.from(fs);
                    FluidStack exist = targetMap.get(key);
                    if (exist == null)
                    {
                        FluidStack view = fs.copy();
                        view.setAmount((int) Math.min(amt, Integer.MAX_VALUE));
                        targetMap.put(key, view);
                    }
                    else
                    {
                        long now = (long) exist.getAmount() + amt;
                        exist.setAmount((int) Math.min(now, Integer.MAX_VALUE));
                    }
                });
            }
        }

        boolean changed = false;

        // 2) 现有上报键：按 (目标数量 - 已报数量) 推送差量
        for (int i = 0; i < keys.size(); i++)
        {
            FluidStack reported = all.get(i);
            int oldAmt = reported.getAmount();
            FluidStack tgt = targetMap.get(keys.get(i));
            int newAmt = (tgt == null) ? 0 : tgt.getAmount();
            int delta = newAmt - oldAmt;
            if (delta != 0 && filter.test(reported))
            {
                if (delta > 0) pushAdd(cache, reported, delta);
                else pushRemove(cache, reported, -delta);
                changed = true;
            }
        }

        // 3) 目标中全新的键：整额加入
        for (Map.Entry<FluidKey, FluidStack> e : targetMap.entrySet())
        {
            if (indexByKey.containsKey(e.getKey())) continue; // 已在第 2 步处理
            FluidStack proto = e.getValue();
            int amt = proto.getAmount();
            if (amt > 0 && filter.test(proto))
            {
                pushAdd(cache, proto, amt);
                changed = true;
            }
        }

        // 4) 用目标替换本地快照（新的“已上报快照”）
        indexByKey.clear();
        keys.clear();
        all.clear();
        for (Map.Entry<FluidKey, FluidStack> e : targetMap.entrySet())
        {
            int idx = all.size();
            indexByKey.put(e.getKey(), idx);
            keys.add(e.getKey());
            all.add(e.getValue());
        }

        if (changed) cache.flush();
    }

    private void pushAdd(IStorageCache<FluidStack> cache, FluidStack proto, long amount)
    {
        long left = amount;
        while (left > 0)
        {
            int step = (int) Math.min(left, Integer.MAX_VALUE);
            cache.add(proto, step, false, true);
            left -= step;
        }
    }

    private void pushRemove(IStorageCache<FluidStack> cache, FluidStack proto, long amount)
    {
        long left = amount;
        while (left > 0)
        {
            int step = (int) Math.min(left, Integer.MAX_VALUE);
            cache.remove(proto, step, true);
            left -= step;
        }
    }

    /**
     * 把增量应用到本地快照，返回“实际生效的有符号数量”（用于 lockstep 推送）。
     * 返回 0 表示快照未变化（如对不存在的键做减法）。
     */
    private long applyDeltaToView(FluidStack keyStack, long diff)
    {
        if (diff == 0) return 0;

        FluidKey key = FluidKey.from(keyStack);
        int idx = indexByKey.getInt(key);

        if (diff > 0)
        {
            if (idx >= 0)
            {
                FluidStack cur = all.get(idx);
                int oldAmt = cur.getAmount();
                long now = (long) oldAmt + diff;
                int newAmt = (int) Math.min(now, Integer.MAX_VALUE);
                cur.setAmount(newAmt);
                return (long) newAmt - oldAmt;
            }
            else
            {
                FluidStack add = keyStack.copy();
                int newAmt = (int) Math.min(diff, Integer.MAX_VALUE);
                add.setAmount(newAmt);
                int newIdx = all.size();
                indexByKey.put(key, newIdx);
                keys.add(key);
                all.add(add);
                return newAmt;
            }
        }
        else // diff < 0
        {
            if (idx < 0) return 0;

            FluidStack cur = all.get(idx);
            int oldAmt = cur.getAmount();
            long now = (long) oldAmt + diff; // 减
            if (now > 0)
            {
                int newAmt = (int) Math.min(now, Integer.MAX_VALUE);
                cur.setAmount(newAmt);
                return (long) newAmt - oldAmt; // 负
            }
            else
            {
                // O(1) 尾删交换
                int last = all.size() - 1;
                if (idx != last)
                {
                    FluidStack tailStack = all.get(last);
                    FluidKey tailKey = keys.get(last);
                    all.set(idx, tailStack);
                    keys.set(idx, tailKey);
                    indexByKey.put(tailKey, idx);
                }
                all.remove(last);
                keys.remove(last);
                indexByKey.removeInt(key);
                return -(long) oldAmt;
            }
        }
    }

    private static final class FluidKey
    {
        private final CompoundTag keyNbt; // Amount=1
        private final int hash;

        private FluidKey(CompoundTag keyNbt)
        {
            this.keyNbt = keyNbt;
            this.hash = keyNbt.hashCode();
        }

        static FluidKey from(FluidStack stack)
        {
            CompoundTag tag = new CompoundTag();
            stack.writeToNBT(tag);
            tag.putInt("Amount", 1);
            return new FluidKey(tag);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (!(o instanceof FluidKey other)) return false;
            return this.keyNbt.equals(other.keyNbt);
        }

        @Override
        public int hashCode()
        {
            return hash;
        }
    }
}
