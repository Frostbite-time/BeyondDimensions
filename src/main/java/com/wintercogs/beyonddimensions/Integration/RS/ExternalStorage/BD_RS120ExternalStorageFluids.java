package com.wintercogs.beyonddimensions.Integration.RS.ExternalStorage;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.storage.AccessType;
import com.refinedmods.refinedstorage.api.storage.cache.IStorageCache;
import com.refinedmods.refinedstorage.api.storage.externalstorage.IExternalStorage;
import com.refinedmods.refinedstorage.api.storage.externalstorage.IExternalStorageContext;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.api.util.IComparer;
import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.Integration.RS.Block.RSNetPathwayBlockEntity;
import com.wintercogs.beyonddimensions.Integration.RS.RSHelper;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.*;

public class BD_RS120ExternalStorageFluids implements IExternalStorage<FluidStack>
{
    /** External Storage 上下文（优先级、访问类型、过滤规则） */
    private final IExternalStorageContext ctx;

    /** External Storage 指向的坐标（用于检查宿主与 net 变化） */
    private final BlockPos targetPos;
    private final ServerLevel level;

    /** 是否处于有效绑定（决定 insert/extract/update 行为） */
    private volatile boolean active = true;

    /** 统一存储 + 订阅句柄 */
    private volatile @Nullable UnifiedStorage unified;
    private @Nullable AutoCloseable unifiedAnySub;
    private @Nullable AutoCloseable unifiedDeltaSub;

    /**
     * 视图结构（与物品版一致）：
     * indexByKey：FluidKey -> index（O(1)）
     * keys：按下标与 all 一一对应
     * all：提交给 RS 的 FluidStack（数量聚合）
     */
    private final Object2IntOpenHashMap<FluidKey> indexByKey = new Object2IntOpenHashMap<>();
    private final ArrayList<FluidKey> keys = new ArrayList<>();
    private final ArrayList<FluidStack> all = new ArrayList<>();

    /** 增量队列：update() 里批量冲入 RS 缓存 */
    private static final class Delta {
        final FluidStack key;
        final long diff; // 正加负减
        Delta(FluidStack key, long diff) { this.key = key; this.diff = diff; }
    }
    private final Deque<Delta> deltaQueue = new ArrayDeque<>();

    /** 解绑后的“待清空快照” */
    private final List<FluidStack> pendingClear = new ArrayList<>();
    private volatile boolean needClearOnce = false;

    /** 重新绑定后的“基线推送” */
    private final List<FluidStack> pendingBaseline = new ArrayList<>();
    private volatile boolean needBaselineOnce = false;

    /** 强引用的回调，防早死 */
    private @Nullable Runnable netChangeListener;

    // ========= 构造 =========

    public BD_RS120ExternalStorageFluids(IExternalStorageContext ctx, ServerLevel level, BlockPos targetPos, UnifiedStorage unified) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
        this.level = Objects.requireNonNull(level, "level");
        this.targetPos = Objects.requireNonNull(targetPos, "targetPos");
        this.unified = Objects.requireNonNull(unified, "unified");

        indexByKey.defaultReturnValue(-1);

        // 初次全量构建 + 订阅
        fullRebuild();
        subscribeUnified(unified);
    }

    /**
     * 在 provider.provide(...) 中调用，挂接到宿主方块实体：
     *   BD_RS120ExternalStorageFluids storage = new BD_RS120ExternalStorageFluids(ctx, serverLevel, pos, net.getUnifiedStorage());
     *   storage.attachTo(rsBe);
     */
    public void attachTo(RSNetPathwayBlockEntity rsBe) {
        if (this.netChangeListener == null) {
            this.netChangeListener = this::onNetChanged;
            rsBe.addNetChangeTask(this.netChangeListener);
        }
        // 宿主被移除时“最后清空”
        rsBe.addRemoveTask(this::onHostRemoved);

        // 立刻尝试同步一次（容错）
        this.onNetChanged();
    }

    // ========= IExternalStorage<FluidStack> =========

    @Override
    public void update(INetwork network) {
        IStorageCache<FluidStack> cache = network.getFluidStorageCache();

        // 1) 解绑清空
        if (needClearOnce && !pendingClear.isEmpty()) {
            for (FluidStack prev : pendingClear) {
                if (!prev.isEmpty() && prev.getAmount() > 0) {
                    cache.remove(prev, (int) Math.min(prev.getAmount(), Integer.MAX_VALUE), true);
                }
            }
            pendingClear.clear();
            needClearOnce = false;
            cache.flush();
        }

        // 2) 重新绑定基线推送
        if (needBaselineOnce && !pendingBaseline.isEmpty()) {
            for (FluidStack base : pendingBaseline) {
                if (!base.isEmpty() && base.getAmount() > 0) {
                    cache.add(base, (int) Math.min(base.getAmount(), Integer.MAX_VALUE), false, true);
                }
            }
            pendingBaseline.clear();
            needBaselineOnce = false;
            cache.flush();
        }

        // 3) 日常增量
        Delta d;
        boolean changed = false;
        while ((d = deltaQueue.pollFirst()) != null) {
            if (d.diff > 0) {
                cache.add(d.key, (int) Math.min(d.diff, Integer.MAX_VALUE), false, true);
                changed = true;
            } else if (d.diff < 0) {
                cache.remove(d.key, (int) Math.min(-d.diff, Integer.MAX_VALUE), true);
                changed = true;
            }
        }
        if (changed) cache.flush();
    }

    @Override
    public long getCapacity() {
        return Long.MAX_VALUE;
    }

    @Override
    public List<FluidStack> getStacks() {
        return Collections.unmodifiableList(all);
    }

    // 导入量
    @Override
    public FluidStack insert(FluidStack prototype, int size, Action action) {
        if (!active || unified == null) return prototype.copy(); // 未绑定
        if (prototype.isEmpty() || size <= 0) return FluidStack.EMPTY;
        if (ctx.getAccessType() == AccessType.EXTRACT) return prototype.copy(); // 只读
        if (!ctx.acceptsFluid(prototype)) return prototype.copy();

        long before = size;
        long inserted = RSHelper.fromFluidStackToIStack(prototype, size)
                .map(s -> size - RSHelper.fromFluidStackToIStack(prototype, size)
                        .map(ss -> unified.insert(ss, action == Action.SIMULATE).getStackAmount())
                        .orElse(0L)) // 这里按 items 版的含义：返回剩余/插入量，你那边实现时可直接返回“插入量”
                .orElse(0L);

        long remainder = before - inserted;
        if (remainder <= 0) return FluidStack.EMPTY;

        FluidStack rem = prototype.copy();
        rem.setAmount((int) Math.min(remainder, Integer.MAX_VALUE));
        return rem;
    }

    // 导出量
    @Override
    public FluidStack extract(FluidStack prototype, int size, int flags, Action action) {
        if (!active || unified == null) return FluidStack.EMPTY;          // 未绑定
        if (prototype.isEmpty() || size <= 0) return FluidStack.EMPTY;    // 无效请求
        if (ctx.getAccessType() == AccessType.INSERT) return FluidStack.EMPTY; // 只写，禁止提取

        var reqOpt = RSHelper.fromFluidStackToIStack(prototype, size);
        if (reqOpt.isEmpty()) return FluidStack.EMPTY;

        IStackType req = reqOpt.get();

        // 最多能拿多少(can)
        long can = unified.extract(req, true).getStackAmount();
        if (can <= 0) return FluidStack.EMPTY;

        // 处理 COMPARE_QUANTITY 语义
        boolean quantityStrict = (flags & IComparer.COMPARE_QUANTITY) == IComparer.COMPARE_QUANTITY;
        if (quantityStrict && can < size) {
            return FluidStack.EMPTY; // 全量提取，否则返回
        }

        // 3) 计算最终要拿的数量（不带数量标志时允许部分返回）
        int want = (int) Math.min(can, size);
        if (want <= 0) return FluidStack.EMPTY;

        // 4) 根据 action 返回或执行
        if (action == Action.SIMULATE) {
            FluidStack out = prototype.copy();
            out.setAmount(want);
            return out;
        }

        // 真正执行抽取（再次用精确 want 数量）
        long took = unified.extract(
                req.copyWithCount(want),
                false
        ).getStackAmount();
        if (took <= 0) return FluidStack.EMPTY;

        FluidStack out = prototype.copy();
        out.setAmount((int) Math.min(took, Integer.MAX_VALUE));
        return out;
    }

    @Override
    public int getStored() {
        // 这里同物品版：不去计算真实总量，统一返回 0
        return 0;
    }

    @Override
    public int getPriority() {
        return ctx.getPriority();
    }

    @Override
    public AccessType getAccessType() {
        return ctx.getAccessType();
    }

    @Override
    public int getCacheDelta(int storedPreInsertion, int size, @Nullable FluidStack remainder) {
        int rem = remainder == null ? 0 : remainder.getAmount();
        int delta = size - rem;
        return Math.max(0, Math.min(delta, Integer.MAX_VALUE));
    }

    // ========= 回调：net 变化 =========

    private void onNetChanged() {
        // 容错：查询当前位置宿主是否仍然有 net
        BlockEntity be = level.getBlockEntity(targetPos);
        DimensionsNet net = null;
        if (be instanceof RSNetPathwayBlockEntity rsBe) {
            net = rsBe.getNet();
        }

        if (net == null) {
            // === 解绑 ===
            active = false;

            unsubscribeUnified();
            unified = null;

            if (!all.isEmpty()) {
                pendingClear.clear();
                for (FluidStack s : all) pendingClear.add(s.copy());
                needClearOnce = true;
            }
            indexByKey.clear();
            keys.clear();
            all.clear();
            deltaQueue.clear();
        } else {
            // === 重新绑定 ===
            UnifiedStorage newUnified = net.getUnifiedStorage();
            if (newUnified != unified) {
                unsubscribeUnified();
                unified = newUnified;
                subscribeUnified(newUnified);

                fullRebuild();
                pendingBaseline.clear();
                for (FluidStack s : all) pendingBaseline.add(s.copy());
                needBaselineOnce = !pendingBaseline.isEmpty();
            }
            active = true;
        }
    }

    // ========= 本地视图维护（FluidKey 防冲突） =========

    private void fullRebuild() {
        indexByKey.clear();
        keys.clear();
        all.clear();

        UnifiedStorage u = this.unified;
        if (u == null) return;

        for (IStackType<?> s : u.getStorage()) {
            if (s.isEmpty()) continue;
            RSHelper.fromIStackToFluidStack(s).ifPresent(fs -> {
                if (!ctx.acceptsFluid(fs)) return;
                FluidKey key = FluidKey.from(fs);
                int idx = indexByKey.getInt(key);
                long amt = s.getStackAmount();
                if (idx < 0) {
                    FluidStack view = fs.copy();
                    view.setAmount((int) Math.min(amt, Integer.MAX_VALUE));
                    int newIdx = all.size();
                    indexByKey.put(key, newIdx);
                    keys.add(key);
                    all.add(view);
                } else {
                    FluidStack exist = all.get(idx);
                    long now = (long) exist.getAmount() + amt;
                    exist.setAmount((int) Math.min(now, Integer.MAX_VALUE));
                }
            });
        }
    }

    private void applyDeltaToView(FluidStack keyStack, long diff) {
        if (diff == 0) return;

        FluidKey key = FluidKey.from(keyStack);
        int idx = indexByKey.getInt(key);

        if (diff > 0) {
            if (idx >= 0) {
                FluidStack cur = all.get(idx);
                long now = (long) cur.getAmount() + diff;
                cur.setAmount((int) Math.min(now, Integer.MAX_VALUE));
            } else {
                FluidStack add = keyStack.copy();
                add.setAmount((int) Math.min(diff, Integer.MAX_VALUE));
                int newIdx = all.size();
                indexByKey.put(key, newIdx);
                keys.add(key);
                all.add(add);
            }
        } else { // diff < 0
            if (idx >= 0) {
                FluidStack cur = all.get(idx);
                long now = (long) cur.getAmount() + diff; // 减
                if (now > 0) {
                    cur.setAmount((int) Math.max(0, Math.min(now, Integer.MAX_VALUE)));
                } else {
                    // O(1) 尾删交换
                    int last = all.size() - 1;
                    if (idx != last) {
                        FluidStack tailStack = all.get(last);
                        FluidKey tailKey = keys.get(last);
                        all.set(idx, tailStack);
                        keys.set(idx, tailKey);
                        indexByKey.put(tailKey, idx);
                    }
                    all.remove(last);
                    keys.remove(last);
                    indexByKey.remove(key);
                }
            }
        }
    }

    // ========= UnifiedStorage 订阅 =========

    private void subscribeUnified(UnifiedStorage u) {
        // 任意变化：安排一次全量重建
        unifiedAnySub = u.subscribeAnyWeak(this, storageItems ->
                level.getServer().execute(this::fullRebuild));

        // 增量：只处理流体
        unifiedDeltaSub = u.subscribeDeltaWeak(this, (storageItems, type, size, insert) -> {
            RSHelper.fromIStackToFluidStack(type).ifPresent(fs -> {
                if (!ctx.acceptsFluid(fs)) return;
                long diff = insert ? size : -size;
                applyDeltaToView(fs, diff);
                deltaQueue.addLast(new Delta(fs.copy(), diff));
            });
        });
    }

    private void unsubscribeUnified() {
        if (unifiedAnySub != null) {
            try { unifiedAnySub.close(); } catch (Exception ignored) {}
            unifiedAnySub = null;
        }
        if (unifiedDeltaSub != null) {
            try { unifiedDeltaSub.close(); } catch (Exception ignored) {}
            unifiedDeltaSub = null;
        }
    }

    /** 宿主被破坏/卸载：与解绑路径一致，但不依赖 net 变更 */
    private void onHostRemoved() {
        if (!active && unified == null && all.isEmpty() && deltaQueue.isEmpty()) return;

        active = false;
        unsubscribeUnified();
        unified = null;

        if (!all.isEmpty()) {
            pendingClear.clear();
            for (FluidStack s : all) {
                if (!s.isEmpty() && s.getAmount() > 0) pendingClear.add(s.copy());
            }
            needClearOnce = true;
        }

        indexByKey.clear();
        keys.clear();
        all.clear();
        deltaQueue.clear();
    }

    // ========= 手动清理（可选） =========

    public void close() {
        unsubscribeUnified();
        netChangeListener = null;
        unified = null;
        deltaQueue.clear();
        pendingClear.clear();
        pendingBaseline.clear();
        active = false;
        indexByKey.clear();
        keys.clear();
        all.clear();
    }

    // ========= FluidKey：用规范化 NBT（忽略 Amount）保证 equals/hash =========

    /**
     * 基于 FluidStack.writeToNBT(...) 得到的完整 NBT 作为 key，并将 Amount 规范化为 1。
     * 覆盖流体类型 + NBT（如化学液、温度、附加数据等），但忽略数量差异。
     */
    private static final class FluidKey {
        private final CompoundTag keyNbt; // 规范化 NBT（Amount=1）
        private final int hash;

        private FluidKey(CompoundTag keyNbt) {
            this.keyNbt = keyNbt;
            this.hash = keyNbt.hashCode();
        }

        static FluidKey from(FluidStack stack) {
            CompoundTag tag = new CompoundTag();
            stack.writeToNBT(tag); // Forge 1.20.1：包含 Fluid、Amount、Tag
            // 规范化数量
            tag.putInt("Amount", 1);
            return new FluidKey(tag);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FluidKey other)) return false;
            return this.keyNbt.equals(other.keyNbt);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
