package com.wintercogs.beyonddimensions.Integration.RS.ExternalStorage;

import com.refinedmods.refinedstorage.api.storage.cache.IStorageCache;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.Integration.RS.RSHelper;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

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

    private final List<ItemStack> pendingClear = new ArrayList<>();
    private volatile boolean needClearOnce = false;

    private final List<ItemStack> pendingBaseline = new ArrayList<>();
    private volatile boolean needBaselineOnce = false;

    public BD_RS120ExternalStorageItemsMirror()
    {
        indexByKey.defaultReturnValue(-1);
    }

    public List<ItemStack> getAllView()
    {
        return Collections.unmodifiableList(all);
    }

    public void clearAll()
    {
        indexByKey.clear();
        keys.clear();
        all.clear();
        deltaQueue.clear();
        pendingClear.clear();
        pendingBaseline.clear();
        needClearOnce = false;
        needBaselineOnce = false;
    }

    /**
     * 解绑用：把当前视图拍成待清空快照
     */
    public void scheduleClearFromCurrentView()
    {
        if (!all.isEmpty())
        {
            pendingClear.clear();
            for (ItemStack s : all) pendingClear.add(s.copy());
            needClearOnce = true;
        }
    }

    /**
     * 重绑/重建后：把当前视图拍成待推送基线
     */
    public void scheduleBaselineFromCurrentView()
    {
        if (!all.isEmpty())
        {
            pendingBaseline.clear();
            for (ItemStack s : all) pendingBaseline.add(s.copy());
            needBaselineOnce = true;
        }
        else
        {
            pendingBaseline.clear();
            needBaselineOnce = false;
        }
    }

    /**
     * 用 unified 进行一次“全量重建”，不带 ctx 过滤（过滤在 flush 时由 adapter 提供）
     */
    public void fullRebuild(@Nullable UnifiedStorage unified)
    {
        indexByKey.clear();
        keys.clear();
        all.clear();
        deltaQueue.clear(); // 重建后，旧增量作废

        if (unified == null) return;

        for (KeyAmount ka : unified.getStorage())
        {
            if (ka == null || ka.isEmpty()) continue;
            RSHelper.fromIStackToItemStack(ka).ifPresent(stk -> {
                if (stk.isEmpty()) return;

                long amt = ka.amount();
                ItemKey key = ItemKey.from(stk);
                int idx = indexByKey.getInt(key);

                if (idx < 0)
                {
                    ItemStack view = stk.copy();
                    view.setCount((int) Math.min(amt, Integer.MAX_VALUE));
                    int newIdx = all.size();
                    indexByKey.put(key, newIdx);
                    keys.add(key);
                    all.add(view);
                }
                else
                {
                    ItemStack exist = all.get(idx);
                    long now = (long) exist.getCount() + amt;
                    exist.setCount((int) Math.min(now, Integer.MAX_VALUE));
                }
            });
        }
    }

    /**
     * AnyChange 场景：用“清空旧视图 + 推送新基线”强制 RS 缓存对齐
     */
    public void resyncFromUnified(@Nullable UnifiedStorage unified)
    {
        // 1) 先把旧视图拍快照用于 remove
        scheduleClearFromCurrentView();

        // 2) 重建新视图
        fullRebuild(unified);

        // 3) 新视图拍快照用于 add
        scheduleBaselineFromCurrentView();
    }

    /**
     * Delta 场景：更新本地视图并入队（count 与 diff 分离）
     */
    public void onDelta(ItemStack keyStack, long diff)
    {
        if (diff == 0) return;
        if (keyStack.isEmpty()) return;

        ItemStack proto = keyStack.copy();
        proto.setCount(1);

        applyDeltaToView(proto, diff);
        deltaQueue.addLast(new Delta(proto, diff));
    }

    /**
     * 把 pending clear / baseline / delta 统一冲入 RS cache。
     * filter 是 ctx.acceptsItem，用于保证 RS 侧只看见允许的物品。
     */
    public void flushToRsCache(IStorageCache<ItemStack> cache, Predicate<ItemStack> filter)
    {
        // 1) 解绑清空
        if (needClearOnce && !pendingClear.isEmpty())
        {
            boolean changed = false;
            for (ItemStack prev : pendingClear)
            {
                if (prev.isEmpty() || prev.getCount() <= 0) continue;
                if (!filter.test(prev)) continue;

                int count = prev.getCount();
                if (count > 0)
                {
                    cache.remove(prev, count, true);
                    changed = true;
                }
            }
            pendingClear.clear();
            needClearOnce = false;
            if (changed) cache.flush();
        }

        // 2) 重新绑定基线推送
        if (needBaselineOnce && !pendingBaseline.isEmpty())
        {
            boolean changed = false;
            for (ItemStack base : pendingBaseline)
            {
                if (base.isEmpty() || base.getCount() <= 0) continue;
                if (!filter.test(base)) continue;

                int count = base.getCount();
                if (count > 0)
                {
                    cache.add(base, count, false, true);
                    changed = true;
                }
            }
            pendingBaseline.clear();
            needBaselineOnce = false;
            if (changed) cache.flush();
        }

        // 3) 日常增量
        Delta d;
        boolean changed = false;
        while ((d = deltaQueue.pollFirst()) != null)
        {
            if (!filter.test(d.key)) continue;

            if (d.diff > 0)
            {
                long left = d.diff;
                while (left > 0)
                {
                    int step = (int) Math.min(left, Integer.MAX_VALUE);
                    cache.add(d.key, step, false, true);
                    left -= step;
                }
                changed = true;
            }
            else if (d.diff < 0)
            {
                long left = -d.diff;
                while (left > 0)
                {
                    int step = (int) Math.min(left, Integer.MAX_VALUE);
                    cache.remove(d.key, step, true);
                    left -= step;
                }
                changed = true;
            }
        }
        if (changed) cache.flush();
    }

    // ========= 视图维护（你原来的 applyDeltaToView 逻辑）=========

    private void applyDeltaToView(ItemStack keyStack, long diff)
    {
        if (diff == 0) return;

        ItemKey key = ItemKey.from(keyStack);
        int idx = indexByKey.getInt(key);

        if (diff > 0)
        {
            if (idx >= 0)
            {
                ItemStack cur = all.get(idx);
                long now = (long) cur.getCount() + diff;
                cur.setCount((int) Math.min(now, Integer.MAX_VALUE));
            }
            else
            {
                ItemStack add = keyStack.copy();
                add.setCount((int) Math.min(diff, Integer.MAX_VALUE));
                int newIdx = all.size();
                indexByKey.put(key, newIdx);
                keys.add(key);
                all.add(add);
            }
        }
        else
        { // diff < 0
            if (idx >= 0)
            {
                ItemStack cur = all.get(idx);
                long now = (long) cur.getCount() + diff; // 减
                if (now > 0)
                {
                    cur.setCount((int) Math.min(now, Integer.MAX_VALUE));
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
                }
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