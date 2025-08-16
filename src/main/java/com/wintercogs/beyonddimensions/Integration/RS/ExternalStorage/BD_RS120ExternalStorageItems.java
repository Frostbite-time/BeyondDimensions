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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.*;

public class BD_RS120ExternalStorageItems implements IExternalStorage<ItemStack>
{

    /** 来自 External Storage 的上下文（优先级、访问类型、过滤） */
    private final IExternalStorageContext ctx;

    /** 被 RS External Storage 指向的坐标（只做容错/查询） */
    private final BlockPos targetPos;
    private final ServerLevel level;

    /** 当前是否已绑定有效 net（决定 insert/extract/update 行为） */
    private volatile boolean active = true;

    /** 统一存储本体 + 订阅句柄（可在解绑时关闭） */
    private volatile @Nullable UnifiedStorage unified;
    private @Nullable AutoCloseable unifiedAnySub;
    private @Nullable AutoCloseable unifiedDeltaSub;

    /**
     * 视图结构：
     * indexByKey：ItemKey -> index（O(1) 查找）
     * keys：按下标与 all 一一对应，便于 O(1) 尾删交换时更新映射
     * all：展示/提交给 RS 的 ItemStack（数量聚合后）
     */
    private final Object2IntOpenHashMap<ItemKey> indexByKey = new Object2IntOpenHashMap<>();
    private final ArrayList<ItemKey> keys = new ArrayList<>();
    private final ArrayList<ItemStack> all = new ArrayList<>();
    //private long stored = 0; // 总量（用于 getStored()）

    /** 增量队列（由 UnifiedStorage 的 onDelta 推入；update() 时批量冲入 RS 缓存） */
    private static final class Delta {
        final ItemStack key; // 原型（count 仅用于展示）
        final long diff;     // 正为增加，负为减少
        Delta(ItemStack key, long diff) { this.key = key; this.diff = diff; }
    }
    private final Deque<Delta> deltaQueue = new ArrayDeque<>();

    /** 解绑后待清空的快照；在下一次 update(network) 时一次性 remove */
    private final List<ItemStack> pendingClear = new ArrayList<>();
    private volatile boolean needClearOnce = false;

    /** 重新绑定后待推送的基线；在下一次 update(network) 时一次性 add */
    private final List<ItemStack> pendingBaseline = new ArrayList<>();
    private volatile boolean needBaselineOnce = false;

    /** 强引用的回调，防止“早死” */
    private @Nullable Runnable netChangeListener;

    // ========= 构造 =========

    public BD_RS120ExternalStorageItems(IExternalStorageContext ctx, ServerLevel level, BlockPos targetPos, UnifiedStorage unified) {
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
     * 在 provider.provide(...) 里调用，把回调挂到方块实体：
     *   BD_RS120ExternalStorageItems storage = new BD_RS120ExternalStorageItems(ctx, serverLevel, pos, net.getUnifiedStorage());
     *   storage.attachTo(rsBe);
     */
    public void attachTo(RSNetPathwayBlockEntity rsBe) {
        if (this.netChangeListener == null) {
            this.netChangeListener = this::onNetChanged;
            rsBe.addNetChangeTask(this.netChangeListener);
        }
        // 新增：宿主被移除时的“最后清空”钩子
        rsBe.addRemoveTask(this::onHostRemoved);

        // 立即同步一次
        this.onNetChanged();
    }

    // ========= IExternalStorage<ItemStack> =========

    @Override
    public void update(INetwork network) {
        IStorageCache<ItemStack> cache = network.getItemStorageCache();

        // 1) 解绑清空
        if (needClearOnce && !pendingClear.isEmpty()) {
            for (ItemStack prev : pendingClear) {
                if (!prev.isEmpty() && prev.getCount() > 0) {
                    cache.remove(prev, prev.getCount(), true);
                }
            }
            pendingClear.clear();
            needClearOnce = false;
            cache.flush();
        }

        // 2) 重新绑定基线推送
        if (needBaselineOnce && !pendingBaseline.isEmpty()) {
            for (ItemStack base : pendingBaseline) {
                if (!base.isEmpty() && base.getCount() > 0) {
                    cache.add(base, base.getCount(), false, true);
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
    public List<ItemStack> getStacks() {
        return Collections.unmodifiableList(all);
    }

    // 余量
    @Override
    public ItemStack insert(ItemStack prototype, int size, Action action) {
        if (!active || unified == null) return prototype.copy(); // 未绑定，全部退回
        if (prototype.isEmpty() || size <= 0) return ItemStack.EMPTY;
        if (ctx.getAccessType() == AccessType.EXTRACT) return prototype.copy(); // 只读
        if (!ctx.acceptsItem(prototype)) return prototype.copy();

        long before = size;
        long inserted = RSHelper.fromItemStackToIStack(prototype, size)
                .map(s -> size - unified.insert(s, action == Action.SIMULATE).getStackAmount())
                .orElse(0L);
        long remainder = before - inserted;

        if (remainder <= 0) return ItemStack.EMPTY;
        ItemStack rem = prototype.copy();
        rem.setCount((int) Math.min(remainder, Integer.MAX_VALUE));
        return rem;
    }

    //导出量
    @Override
    public ItemStack extract(ItemStack prototype, int size, int flags, Action action) {
        if (!active || unified == null) return ItemStack.EMPTY;          // 未绑定
        if (prototype.isEmpty() || size <= 0) return ItemStack.EMPTY;    // 无效请求
        if (ctx.getAccessType() == AccessType.INSERT) return ItemStack.EMPTY; // 只写，禁止提取

        var reqOpt = RSHelper.fromItemStackToIStack(prototype, size);
        if (reqOpt.isEmpty()) return ItemStack.EMPTY;

        IStackType req = reqOpt.get();

        // 最多能拿多少(can)
        long can = unified.extract(req, true).getStackAmount();
        if (can <= 0) return ItemStack.EMPTY;

        // 处理 COMPARE_QUANTITY 语义
        boolean quantityStrict = (flags & IComparer.COMPARE_QUANTITY) == IComparer.COMPARE_QUANTITY;
        if (quantityStrict && can < size) {
            return ItemStack.EMPTY; // 全量提取，否则返回
        }

        // 3) 计算最终要拿的数量（不带数量标志时允许部分返回）
        int want = (int) Math.min(can, size);
        if (want <= 0) return ItemStack.EMPTY;

        // 4) 根据 action 返回或执行
        if (action == Action.SIMULATE) {
            ItemStack out = prototype.copy();
            out.setCount(want);
            return out;
        }

        // 真正执行抽取（再次用精确 want 数量）
        long took = unified.extract(
                req.copyWithCount(want),
                false
        ).getStackAmount();
        if (took <= 0) return ItemStack.EMPTY;

        ItemStack out = prototype.copy();
        out.setCount((int) Math.min(took, Integer.MAX_VALUE));
        return out;
    }

    @Override
    public int getStored() {
        return 0;
        //return (int) Math.min(stored, Integer.MAX_VALUE); 就是不想计算这个，UnifiedStorage的最大上限远远大于此
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
    public int getCacheDelta(int storedPreInsertion, int size, @Nullable ItemStack remainder) {
        int rem = remainder == null ? 0 : remainder.getCount();
        int delta = size - rem;
        return Math.max(0, Math.min(delta, Integer.MAX_VALUE));
    }

    // ========= 回调：net 变化 =========

    /**
     * 由 rsBe.addNetChangeTask(listener) 触发。仅设置延迟标记，由下一次 update(network) 执行实际缓存操作。
     */
    private void onNetChanged() {
        // 查询当前位置的方块实体，判断是否仍有 net
        BlockEntity be = level.getBlockEntity(targetPos);
        DimensionsNet net = null;
        if (be instanceof RSNetPathwayBlockEntity rsBe) {
            net = rsBe.getNet();
        }

        if (net == null) {
            // === 解绑 ===
            active = false;

            // 1) 关闭旧订阅，清除统一存储引用
            unsubscribeUnified();
            unified = null;

            // 2) 捕获当前本地视图为“待清空快照”，清空本地视图与增量
            if (!all.isEmpty()) {
                pendingClear.clear();
                for (ItemStack s : all) pendingClear.add(s.copy());
                needClearOnce = true;
            }
            indexByKey.clear();
            keys.clear();
            all.clear();
            //stored = 0;
            deltaQueue.clear();
        } else {
            // === 重新绑定 ===
            UnifiedStorage newUnified = net.getUnifiedStorage();
            if (newUnified != unified) {
                // 切换订阅对象
                unsubscribeUnified();
                unified = newUnified;
                subscribeUnified(newUnified);

                // 用新 unified 重建视图，并把基线安排给下一轮 update() 上报
                fullRebuild();
                pendingBaseline.clear();
                for (ItemStack s : all) pendingBaseline.add(s.copy());
                needBaselineOnce = !pendingBaseline.isEmpty();
            }
            active = true;
        }
    }

    // ========= 本地视图维护（使用 ItemKey 防冲突） =========

    private void fullRebuild() {
        indexByKey.clear();
        keys.clear();
        all.clear();
        //stored = 0;

        UnifiedStorage u = this.unified;
        if (u == null) return;

        for (IStackType<?> s : u.getStorage()) {
            if (s.isEmpty()) continue;
            RSHelper.fromIStackToItemStack(s).ifPresent(stk -> {
                if (!ctx.acceptsItem(stk)) return;
                ItemKey key = ItemKey.from(stk);
                int idx = indexByKey.getInt(key);
                long amt = s.getStackAmount();
                if (idx < 0) {
                    ItemStack view = stk.copy();
                    view.setCount((int) Math.min(amt, Integer.MAX_VALUE));
                    int newIdx = all.size();
                    indexByKey.put(key, newIdx);
                    keys.add(key);
                    all.add(view);
                } else {
                    ItemStack exist = all.get(idx);
                    long now = (long) exist.getCount() + amt;
                    exist.setCount((int) Math.min(now, Integer.MAX_VALUE));
                }
                //stored += amt;
            });
        }
    }

    private void applyDeltaToView(ItemStack keyStack, long diff) {
        if (diff == 0) return;

        ItemKey key = ItemKey.from(keyStack);
        int idx = indexByKey.getInt(key);

        if (diff > 0) {
            if (idx >= 0) {
                ItemStack cur = all.get(idx);
                long now = (long) cur.getCount() + diff;
                cur.setCount((int) Math.min(now, Integer.MAX_VALUE));
            } else {
                ItemStack add = keyStack.copy();
                add.setCount((int) Math.min(diff, Integer.MAX_VALUE));
                int newIdx = all.size();
                indexByKey.put(key, newIdx);
                keys.add(key);
                all.add(add);
            }
            //stored += diff;
        } else { // diff < 0
            if (idx >= 0) {
                ItemStack cur = all.get(idx);
                long now = (long) cur.getCount() + diff; // 减
                if (now > 0) {
                    cur.setCount((int) Math.max(0, Math.min(now, Integer.MAX_VALUE)));
                } else {
                    // O(1) 尾删交换
                    int last = all.size() - 1;
                    if (idx != last) {
                        ItemStack tailStack = all.get(last);
                        ItemKey tailKey = keys.get(last);
                        all.set(idx, tailStack);
                        keys.set(idx, tailKey);
                        indexByKey.put(tailKey, idx);
                    }
                    all.remove(last);
                    keys.remove(last);
                    indexByKey.remove(key);
                }
                //stored = Math.max(0, stored + diff);
            }
        }
    }

    // ========= UnifiedStorage 订阅管理 =========

    private void subscribeUnified(UnifiedStorage u) {
        // 有明细走 onDelta，无明细走 onAnyChange -> 安排一次全量重建
        unifiedAnySub = u.subscribeAnyWeak(this,
                storageItems -> level.getServer().execute(this::fullRebuild));

        unifiedDeltaSub = u.subscribeDeltaWeak(this, (storageItems, type, size, insert) -> {
            RSHelper.fromIStackToItemStack(type).ifPresent(stack -> {
                if (!ctx.acceptsItem(stack)) return; // 过滤（External Storage 上的白名单等）
                long diff = insert ? size : -size;
                // 先更新本地视图
                applyDeltaToView(stack, diff);
                // 再入队，等 update(INetwork) 时一次性提交给 RS 缓存
                deltaQueue.addLast(new Delta(stack.copy(), diff));
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

    /** 宿主方块实体被破坏/卸载时调用：与“解绑”路径一致，但不再依赖 net 变更 */
    private void onHostRemoved() {
        // 已经做过就不重复
        if (!active && unified == null && all.isEmpty() && deltaQueue.isEmpty()) return;

        // 1) 关闭订阅，标记失效
        active = false;
        unsubscribeUnified();
        unified = null;

        // 2) 把当前本地视图拍成快照，交由下一次 update(network) 统一 remove
        if (!all.isEmpty()) {
            pendingClear.clear();
            for (ItemStack s : all) {
                if (!s.isEmpty() && s.getCount() > 0) pendingClear.add(s.copy());
            }
            needClearOnce = true;
        }

        // 3) 清理本地视图与增量
        indexByKey.clear();
        keys.clear();
        all.clear();
        //stored = 0;
        deltaQueue.clear();
    }

    // ========= 手动清理（可选） =========

    public void close() {
        unsubscribeUnified();
        netChangeListener = null; // 回调本身由方块实体持有，我们不强制移除
        unified = null;
        deltaQueue.clear();
        pendingClear.clear();
        pendingBaseline.clear();
        active = false;
        indexByKey.clear();
        keys.clear();
        all.clear();
        //stored = 0;
    }

    // ========= ItemKey：用规范化 NBT（忽略数量）保证 equals/hash 一致，彻底规避哈希冲突 =========

    /**
     * ItemKey 基于 ItemStack.save(...) 得到的完整 NBT 作为 key，并将 Count 规范化为 1。
     * 这样同时覆盖物品 ID、数据组件/标签、ForgeCaps 等一切会影响堆叠相等性的内容，但忽略数量。
     */
    private static final class ItemKey {
        private final CompoundTag keyNbt; // 规范化 NBT（Count=1）
        private final int hash;           // 预计算哈希

        private ItemKey(CompoundTag keyNbt) {
            this.keyNbt = keyNbt;
            this.hash = keyNbt.hashCode();
        }

        static ItemKey from(ItemStack stack) {
            CompoundTag tag = new CompoundTag();
            // 保存完整堆叠信息（包含 item id、数据组件、ForgeCaps 等）
            stack.save(tag);
            // 规范化数量：忽略 Count 的差异
            tag.putByte("Count", (byte) 1); // 放一个同名键覆盖
            return new ItemKey(tag);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ItemKey other)) return false;
            return this.keyNbt.equals(other.keyNbt);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}