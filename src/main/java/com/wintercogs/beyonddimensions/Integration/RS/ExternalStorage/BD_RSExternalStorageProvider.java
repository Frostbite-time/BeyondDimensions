package com.wintercogs.beyonddimensions.Integration.RS.ExternalStorage;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.external.ExternalStorageProvider;
import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.Integration.RS.Block.RSNetPathwayBlockEntity;
import com.wintercogs.beyonddimensions.Integration.RS.RSHelper;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;

public class BD_RSExternalStorageProvider implements ExternalStorageProvider
{

    private final ServerLevel level;
    private final BlockPos pos;

    /** 资源 → 数量 */
    private final Object2LongOpenHashMap<ResourceKey> counts = new Object2LongOpenHashMap<>();
    /** 资源 → 在 all 的下标（-1 表示不存在） */
    private final Object2IntOpenHashMap<ResourceKey> index = new Object2IntOpenHashMap<>();
    /** 扁平列表视图（顺序不保证） */
    private final ArrayList<ResourceAmount> all = new ArrayList<>();

    /** 当前已订阅到的 UnifiedStorage（按对象身份判断），用于幂等重绑 */
    private @Nullable UnifiedStorage subscribedUnified = null;
    /** 当前订阅的关闭句柄 */
    private @Nullable AutoCloseable unifiedDeltaSub = null;
    private @Nullable AutoCloseable unifiedAnySub = null;

    /** 可选：定期全量校准（tick），0 关闭 */
    private static final int FULL_REBUILD_INTERVAL_TICKS = 0;
    private long lastFullBuildGt = Long.MIN_VALUE;

    public BD_RSExternalStorageProvider(ServerLevel level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
        index.defaultReturnValue(-1);
        counts.defaultReturnValue(0L);
        // 首次构建 + 订阅
        fullRebuildSnapshot();
        // 尽量在网络变化时触发一次重绑（但真正兜底逻辑在 iterator()）
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RSNetPathwayBlockEntity rsBe) {
            rsBe.addNetChangeTask(() -> level.getServer().execute(this::refreshBindingIfNeeded));
        }
        // 首次绑定订阅（幂等）
        refreshBindingIfNeeded();
    }

    // ================= ExternalStorageProvider =================

    @Override
    public Iterator<ResourceAmount> iterator() {
        // 兜底：每 tick 被 RS 轮询时，轻量检测是否需要重绑
        refreshBindingIfNeeded();
        maybePeriodicFullRebuild();
        return all.iterator();
    }

    @Override
    public long extract(ResourceKey resourceKey, long amount, Action action, Actor actor) {
        UnifiedStorage us = currentUnified();
        if (us == null || amount <= 0) return 0L;
        return RSHelper.fromRSKeyToIStack(resourceKey)
                .map(s -> us.extract(s,amount, action == Action.SIMULATE).amount())
                .orElse(0L);
    }

    @Override
    public long insert(ResourceKey resourceKey, long amount, Action action, Actor actor) {
        UnifiedStorage us = currentUnified();
        if (us == null || amount <= 0) return 0L;
        return RSHelper.fromRSKeyToIStack(resourceKey)
                .map(s -> amount - us.insert(s,amount, action == Action.SIMULATE).amount())
                .orElse(0L);
    }

    // ================= 绑定 / 订阅 =================

    /** 幂等重绑：当目标 UnifiedStorage 发生变化（或从/到 null）时，取消旧订阅并订阅新目标。 */
    private void refreshBindingIfNeeded() {
        UnifiedStorage current = currentUnified();

        // 若对象身份相同（含均为 null）则无需动作
        if (current == subscribedUnified) return;

        // 先解除旧订阅
        cancelUnifiedSub();

        // 记录新目标
        subscribedUnified = current;

        if (current == null) {
            // 目标消失：清空一次视图，避免 RS 继续看到旧数据
            clearSnapshot();
            return;
        }

        // 注：对于同一次改变，如果已经发送增量通知，则不会发送全量通知
        // UnifiedStorage仅会在完全确定是增量变化的情况下触发增量通知

        // 幂等重绑
        unifiedDeltaSub = current.subscribeDeltaWeak(
                this,
                (prov, type, size, insert) -> RSHelper.fromIStackToRSKey(type)
                        .ifPresent(key -> prov.applyDelta(key, insert ? size : -size))
        );
        // 订阅anychange的弱引用
        unifiedAnySub = current.subscribeAnyWeak(this, prov -> prov.level.getServer().execute(prov::fullRebuildSnapshot));

        // 为新目标做一次全量构建，确保视图与后端一致
        fullRebuildSnapshot();
    }

    /** 取消当前订阅（若存在），并把句柄置空。 */
    private void cancelUnifiedSub() {
        if (unifiedDeltaSub != null) {
            try { unifiedDeltaSub.close(); } catch (Exception ignored) {}
            unifiedDeltaSub = null;
        }
        if(unifiedAnySub != null) {
            try { unifiedAnySub.close(); } catch (Exception ignored) {}
            unifiedAnySub = null;
        }
    }

    // ================= 快照维护（O(1) 增量） =================

    private void applyDelta(ResourceKey key, long diff) {
        if (diff == 0) return;
        long old = counts.getLong(key);
        long now = old + diff;

        if (now > 0) {
            counts.put(key, now);
            int i = index.getInt(key);
            if (i >= 0) {
                all.set(i, new ResourceAmount(key, now));
            } else {
                index.put(key, all.size());
                all.add(new ResourceAmount(key, now));
            }
        } else {
            counts.removeLong(key);
            int i = index.getInt(key);
            if (i >= 0) {
                int last = all.size() - 1;
                if (i != last) {
                    ResourceAmount tail = all.get(last);
                    all.set(i, tail);
                    index.put(tail.resource(), i);
                }
                all.remove(last);
                index.removeInt(key);
            }
        }
    }

    private void clearSnapshot() {
        counts.clear();
        index.clear();
        all.clear();
        lastFullBuildGt = level.getGameTime();
    }

    /** 全量重建一次（仅在绑定/兜底/校准时调用） */
    private void fullRebuildSnapshot() {
        counts.clear();
        index.clear();
        all.clear();

        UnifiedStorage us = currentUnified();
        if (us != null) {
            for (KeyAmount s : us.getStorage()) {
                if (s.isEmpty()) continue;
                RSHelper.fromIStackToRSKey(s.key()).ifPresent(k -> {
                    long prev = counts.getLong(k);
                    long now = prev + s.amount();
                    if (prev == 0) {
                        counts.put(k, now);
                        index.put(k, all.size());
                        all.add(new ResourceAmount(k, now));
                    } else {
                        counts.put(k, now);
                        int i = index.getInt(k);
                        if (i >= 0) {
                            all.set(i, new ResourceAmount(k, now));
                        } else {
                            index.put(k, all.size());
                            all.add(new ResourceAmount(k, now));
                        }
                    }
                });
            }
        }
        lastFullBuildGt = level.getGameTime();
    }

    /** 可选：定期校准 */
    private void maybePeriodicFullRebuild() {
        if (FULL_REBUILD_INTERVAL_TICKS <= 0) return;
        long gt = level.getGameTime();
        if (gt - lastFullBuildGt >= FULL_REBUILD_INTERVAL_TICKS) {
            fullRebuildSnapshot();
        }
    }

    // ================= 辅助 =================

    private @Nullable UnifiedStorage currentUnified() {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RSNetPathwayBlockEntity rsBe) {
            DimensionsNet net = rsBe.getNet();
            if (net != null) return net.getUnifiedStorage();
        }
        return null;
    }
}
