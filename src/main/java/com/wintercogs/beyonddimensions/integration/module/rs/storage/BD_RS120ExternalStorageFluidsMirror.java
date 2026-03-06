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

    private final List<FluidStack> pendingClear = new ArrayList<>();
    private volatile boolean needClearOnce = false;

    private final List<FluidStack> pendingBaseline = new ArrayList<>();
    private volatile boolean needBaselineOnce = false;

    public BD_RS120ExternalStorageFluidsMirror()
    {
        indexByKey.defaultReturnValue(-1);
    }

    public List<FluidStack> getAllView()
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

    public void scheduleClearFromCurrentView()
    {
        if (!all.isEmpty())
        {
            pendingClear.clear();
            for (FluidStack s : all) pendingClear.add(s.copy());
            needClearOnce = true;
        }
    }

    public void scheduleBaselineFromCurrentView()
    {
        if (!all.isEmpty())
        {
            pendingBaseline.clear();
            for (FluidStack s : all) pendingBaseline.add(s.copy());
            needBaselineOnce = true;
        }
        else
        {
            pendingBaseline.clear();
            needBaselineOnce = false;
        }
    }

    public void fullRebuild(@Nullable UnifiedStorage unified)
    {
        indexByKey.clear();
        keys.clear();
        all.clear();
        deltaQueue.clear();

        if (unified == null) return;

        for (KeyAmount ka : unified.getStorage())
        {
            if (ka == null || ka.isEmpty()) continue;

            RSHelper.fromIStackToFluidStack(ka).ifPresent(fs -> {
                if (fs.isEmpty()) return;

                long amt = ka.amount();
                FluidKey key = FluidKey.from(fs);
                int idx = indexByKey.getInt(key);

                if (idx < 0)
                {
                    FluidStack view = fs.copy();
                    view.setAmount((int) Math.min(amt, Integer.MAX_VALUE));
                    int newIdx = all.size();
                    indexByKey.put(key, newIdx);
                    keys.add(key);
                    all.add(view);
                }
                else
                {
                    FluidStack exist = all.get(idx);
                    long now = (long) exist.getAmount() + amt;
                    exist.setAmount((int) Math.min(now, Integer.MAX_VALUE));
                }
            });
        }
    }

    public void resyncFromUnified(@Nullable UnifiedStorage unified)
    {
        scheduleClearFromCurrentView();
        fullRebuild(unified);
        scheduleBaselineFromCurrentView();
    }

    public void onDelta(FluidStack keyStack, long diff)
    {
        if (diff == 0) return;
        if (keyStack.isEmpty()) return;

        FluidStack proto = keyStack.copy();
        proto.setAmount(1);

        applyDeltaToView(proto, diff);
        deltaQueue.addLast(new Delta(proto, diff));
    }

    public void flushToRsCache(IStorageCache<FluidStack> cache, Predicate<FluidStack> filter)
    {
        // 1) clear
        if (needClearOnce && !pendingClear.isEmpty())
        {
            boolean changed = false;
            for (FluidStack prev : pendingClear)
            {
                if (prev.isEmpty() || prev.getAmount() <= 0) continue;
                if (!filter.test(prev)) continue;

                int amount = prev.getAmount();
                if (amount > 0)
                {
                    cache.remove(prev, amount, true);
                    changed = true;
                }
            }
            pendingClear.clear();
            needClearOnce = false;
            if (changed) cache.flush();
        }

        // 2) baseline
        if (needBaselineOnce && !pendingBaseline.isEmpty())
        {
            boolean changed = false;
            for (FluidStack base : pendingBaseline)
            {
                if (base.isEmpty() || base.getAmount() <= 0) continue;
                if (!filter.test(base)) continue;

                int amount = base.getAmount();
                if (amount > 0)
                {
                    cache.add(base, amount, false, true);
                    changed = true;
                }
            }
            pendingBaseline.clear();
            needBaselineOnce = false;
            if (changed) cache.flush();
        }

        // 3) delta
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

    private void applyDeltaToView(FluidStack keyStack, long diff)
    {
        FluidKey key = FluidKey.from(keyStack);
        int idx = indexByKey.getInt(key);

        if (diff > 0)
        {
            if (idx >= 0)
            {
                FluidStack cur = all.get(idx);
                long now = (long) cur.getAmount() + diff;
                cur.setAmount((int) Math.min(now, Integer.MAX_VALUE));
            }
            else
            {
                FluidStack add = keyStack.copy();
                add.setAmount((int) Math.min(diff, Integer.MAX_VALUE));
                int newIdx = all.size();
                indexByKey.put(key, newIdx);
                keys.add(key);
                all.add(add);
            }
        }
        else
        {
            if (idx >= 0)
            {
                FluidStack cur = all.get(idx);
                long now = (long) cur.getAmount() + diff;
                if (now > 0)
                {
                    cur.setAmount((int) Math.max(0, Math.min(now, Integer.MAX_VALUE)));
                }
                else
                {
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
                }
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