package com.wintercogs.beyonddimensions.integration.module.rs.storage;

import com.refinedmods.refinedstorage.api.storage.cache.IStorageCache;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.integration.module.rs.RSHelper;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

/**
 * 维护“已上报给 RS 网络缓存(ItemStorageCache)的物品快照”。
 *
 * <ul>
 *   <li>{@code all/keys/indexByKey} 即“已上报快照”，{@code getStacks()} 直接返回它。</li>
 *   <li>RS 在每次 {@code invalidate()} 时会清空网络列表并用各存储的 {@code getStacks()} 全量重建；
 *       因此本类绝不再向缓存额外推送一份“全量基线”，否则会与 {@code getStacks()} 叠加成双倍。</li>
 *   <li>日常增量({@link #onDelta}) 只入队，在 flush 时“更新快照 + 推送缓存”同步进行(lockstep)，
 *       保证 {@code getStacks()} 永远等于已推送状态，任何 invalidate 交错都不会多算。</li>
 *   <li>绑定/解绑/AnyChange 等“整体变化”用 {@link #needResync} 标记，在 flush 时对
 *       目标(live unified)与当前快照做差量(diff)推送，再令快照=目标。</li>
 * </ul>
 */
public class BD_RS120ExternalStorageItemsMirror
{
    private final Object2IntOpenHashMap<ItemKey> indexByKey = new Object2IntOpenHashMap<>();
    private final ArrayList<ItemKey> keys = new ArrayList<>();
    private final ArrayList<ItemStack> all = new ArrayList<>();

    private static final class Delta
    {
        final ItemStack key; // 原型（count=1）
        final long diff;     // 正为增加，负为减少

        Delta(ItemStack key, long diff)
        {
            this.key = key;
            this.diff = diff;
        }
    }

    private final Deque<Delta> deltaQueue = new ArrayDeque<>();

    // 整体重同步标记：绑定/解绑/AnyChange 时置位，flush 时对 live 目标做差量
    private volatile boolean needResync = false;

    public BD_RS120ExternalStorageItemsMirror()
    {
        indexByKey.defaultReturnValue(-1);
    }

    public List<ItemStack> getAllView()
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
    public void onDelta(ItemStack keyStack, long diff)
    {
        if (diff == 0) return;
        if (keyStack.isEmpty()) return;

        ItemStack proto = keyStack.copy();
        proto.setCount(1);

        deltaQueue.addLast(new Delta(proto, diff));
    }

    /**
     * 把已入队的增量 / 整体重同步统一冲入 RS cache。
     *
     * @param filter ctx.acceptsItem，保证 RS 侧只看见允许的物品
     * @param target 当前绑定的 live unified（null 表示已解绑，目标为空）
     */
    public void flushToRsCache(IStorageCache<ItemStack> cache, Predicate<ItemStack> filter, @Nullable UnifiedStorage target)
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
    private void resyncDiff(IStorageCache<ItemStack> cache, Predicate<ItemStack> filter, @Nullable UnifiedStorage target)
    {
        // 1) 构建目标快照：key -> 原型(count 已 clamp 为目标数量)
        LinkedHashMap<ItemKey, ItemStack> targetMap = new LinkedHashMap<>();
        if (target != null)
        {
            for (KeyAmount ka : target.getStorage())
            {
                if (ka == null || ka.isEmpty()) continue;
                RSHelper.fromIStackToItemStack(ka).ifPresent(stk -> {
                    if (stk.isEmpty()) return;
                    long amt = ka.amount();
                    ItemKey key = ItemKey.from(stk);
                    ItemStack exist = targetMap.get(key);
                    if (exist == null)
                    {
                        ItemStack view = stk.copy();
                        view.setCount((int) Math.min(amt, Integer.MAX_VALUE));
                        targetMap.put(key, view);
                    }
                    else
                    {
                        long now = (long) exist.getCount() + amt;
                        exist.setCount((int) Math.min(now, Integer.MAX_VALUE));
                    }
                });
            }
        }

        boolean changed = false;

        // 2) 现有上报键：按 (目标数量 - 已报数量) 推送差量
        for (int i = 0; i < keys.size(); i++)
        {
            ItemStack reported = all.get(i);
            int oldAmt = reported.getCount();
            ItemStack tgt = targetMap.get(keys.get(i));
            int newAmt = (tgt == null) ? 0 : tgt.getCount();
            int delta = newAmt - oldAmt;
            if (delta != 0 && filter.test(reported))
            {
                if (delta > 0) pushAdd(cache, reported, delta);
                else pushRemove(cache, reported, -delta);
                changed = true;
            }
        }

        // 3) 目标中全新的键：整额加入
        for (Map.Entry<ItemKey, ItemStack> e : targetMap.entrySet())
        {
            if (indexByKey.containsKey(e.getKey())) continue; // 已在第 2 步处理
            ItemStack proto = e.getValue();
            int amt = proto.getCount();
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
        for (Map.Entry<ItemKey, ItemStack> e : targetMap.entrySet())
        {
            int idx = all.size();
            indexByKey.put(e.getKey(), idx);
            keys.add(e.getKey());
            all.add(e.getValue());
        }

        if (changed) cache.flush();
    }

    private void pushAdd(IStorageCache<ItemStack> cache, ItemStack proto, long amount)
    {
        long left = amount;
        while (left > 0)
        {
            int step = (int) Math.min(left, Integer.MAX_VALUE);
            cache.add(proto, step, false, true);
            left -= step;
        }
    }

    private void pushRemove(IStorageCache<ItemStack> cache, ItemStack proto, long amount)
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
    private long applyDeltaToView(ItemStack keyStack, long diff)
    {
        if (diff == 0) return 0;

        ItemKey key = ItemKey.from(keyStack);
        int idx = indexByKey.getInt(key);

        if (diff > 0)
        {
            if (idx >= 0)
            {
                ItemStack cur = all.get(idx);
                int oldCount = cur.getCount();
                long now = (long) oldCount + diff;
                int newCount = (int) Math.min(now, Integer.MAX_VALUE);
                cur.setCount(newCount);
                return (long) newCount - oldCount;
            }
            else
            {
                ItemStack add = keyStack.copy();
                int newCount = (int) Math.min(diff, Integer.MAX_VALUE);
                add.setCount(newCount);
                int newIdx = all.size();
                indexByKey.put(key, newIdx);
                keys.add(key);
                all.add(add);
                return newCount;
            }
        }
        else // diff < 0
        {
            if (idx < 0) return 0;

            ItemStack cur = all.get(idx);
            int oldCount = cur.getCount();
            long now = (long) oldCount + diff; // 减
            if (now > 0)
            {
                int newCount = (int) Math.min(now, Integer.MAX_VALUE);
                cur.setCount(newCount);
                return (long) newCount - oldCount; // 负
            }
            else
            {
                // O(1) 尾删交换
                int last = all.size() - 1;
                if (idx != last)
                {
                    ItemStack tailStack = all.get(last);
                    ItemKey tailKey = keys.get(last);
                    all.set(idx, tailStack);
                    keys.set(idx, tailKey);
                    indexByKey.put(tailKey, idx);
                }
                all.remove(last);
                keys.remove(last);
                indexByKey.removeInt(key);
                return -(long) oldCount;
            }
        }
    }

    private static final class ItemKey
    {
        private final CompoundTag keyNbt; // Count=1
        private final int hash;

        private ItemKey(CompoundTag keyNbt)
        {
            this.keyNbt = keyNbt;
            this.hash = keyNbt.hashCode();
        }

        static ItemKey from(ItemStack stack)
        {
            CompoundTag tag = new CompoundTag();
            stack.save(tag);
            tag.putByte("Count", (byte) 1);
            return new ItemKey(tag);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (!(o instanceof ItemKey other)) return false;
            return this.keyNbt.equals(other.keyNbt);
        }

        @Override
        public int hashCode()
        {
            return hash;
        }
    }
}
