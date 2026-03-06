package com.wintercogs.beyonddimensions.integration.module.rs.Block;

import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.common.block.entity.NetedBlockEntity;
import com.wintercogs.beyonddimensions.common.init.BDBlockEntities;
import com.wintercogs.beyonddimensions.integration.module.rs.RSHelper;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class RSNetPathwayBlockEntity extends NetedBlockEntity
{

    /**
     * ResourceKey -> amount
     */
    private final Object2LongOpenHashMap<ResourceKey> counts = new Object2LongOpenHashMap<>();
    /**
     * ResourceKey -> index in all
     */
    private final Object2IntOpenHashMap<ResourceKey> index = new Object2IntOpenHashMap<>();
    /**
     * flat view
     */
    private final ArrayList<ResourceAmount> all = new ArrayList<>();

    // ========= 订阅绑定 =========

    /**
     * 当前已订阅到的 UnifiedStorage（用对象身份幂等判断）
     */
    private @Nullable UnifiedStorage subscribedUnified = null;

    private @Nullable AutoCloseable unifiedDeltaSub = null;
    private @Nullable AutoCloseable unifiedAnySub = null;

    /**
     * 定期全量校准（0 表示关闭）
     */
    private static final int FULL_REBUILD_INTERVAL_TICKS = 0;
    private long lastFullBuildGt = Long.MIN_VALUE;

    /**
     * 避免重复注册 net-change hook
     */
    private boolean netTaskRegistered = false;

    public RSNetPathwayBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(BDBlockEntities.RS_NET_PATHWAY_BLOCK_ENTITY.get(), pos, blockState);

        index.defaultReturnValue(-1);
        counts.defaultReturnValue(0L);

        ensureNetTaskRegistered();
    }

    private void ensureNetTaskRegistered()
    {
        if (netTaskRegistered) return;
        netTaskRegistered = true;

        // netId / net 发生变化时会触发 setChanged() -> onNetChange()
        // 这里注册一次“重绑”逻辑
        addNetChangeTask(() -> runOnServerThread(this::refreshBindingIfNeeded));
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        // BE load 后立刻尝试绑定一次（避免首次 iterator 才绑定）
        refreshBindingIfNeeded();
    }

    @Override
    public void setRemoved()
    {
        super.setRemoved();
        cancelUnifiedSub();
        clearSnapshot();
    }

    @Override
    public void onChunkUnloaded()
    {
        super.onChunkUnloaded();
        cancelUnifiedSub();
        clearSnapshot();
    }

    // ================= 供 Provider 调用 =================

    /**
     * iterator() 数据源：这里会做轻量“重绑兜底 + 可选定期校准”
     */
    public Iterator<ResourceAmount> rsExternalSnapshotIterator()
    {
        refreshBindingIfNeeded();
        maybePeriodicFullRebuild();
        if (all.isEmpty())
        {
            return Collections.emptyIterator();
        }
        return all.iterator();
    }

    /**
     * insert/extract 用：返回当前 unified（可能为 null）
     */
    public @Nullable UnifiedStorage getUnifiedStorageForRsOrNull()
    {
        DimensionsNet net = getNet();
        if (net == null) return null;
        return net.getUnifiedStorage();
    }

    // ================= 绑定 / 订阅（核心迁移点） =================

    /**
     * 幂等重绑：当 unified 身份变化（或从/到 null）时，取消旧订阅并订阅新目标
     */
    private void refreshBindingIfNeeded()
    {
        UnifiedStorage current = getUnifiedStorageForRsOrNull();

        if (current == subscribedUnified) return;

        cancelUnifiedSub();
        subscribedUnified = current;

        if (current == null)
        {
            // 目标消失：清空一次视图，避免 RS 继续看到旧数据
            clearSnapshot();
            return;
        }

        // delta：O(1) 更新快照
        unifiedDeltaSub = current.subscribeDeltaWeak(
                this,
                (be, type, size, insert) -> RSHelper.fromIStackToRSKey(type)
                        .ifPresent(key -> be.applyDelta(key, insert ? size : -size))
        );

        // any：安排一次全量重建（在主线程执行）
        unifiedAnySub = current.subscribeAnyWeak(
                this,
                be -> be.runOnServerThread(((RSNetPathwayBlockEntity) be)::fullRebuildSnapshot)
        );

        // 新目标：做一次全量构建对齐
        fullRebuildSnapshot();
    }

    private void cancelUnifiedSub()
    {
        if (unifiedDeltaSub != null)
        {
            try
            {
                unifiedDeltaSub.close();
            }
            catch (Exception ignored)
            {
            }
            unifiedDeltaSub = null;
        }
        if (unifiedAnySub != null)
        {
            try
            {
                unifiedAnySub.close();
            }
            catch (Exception ignored)
            {
            }
            unifiedAnySub = null;
        }
    }

    // ================= 快照维护（O(1) 增量） =================

    private void applyDelta(ResourceKey key, long diff)
    {
        if (diff == 0) return;

        long old = counts.getLong(key);
        long now = old + diff;

        if (now > 0)
        {
            counts.put(key, now);
            int i = index.getInt(key);
            if (i >= 0)
            {
                all.set(i, new ResourceAmount(key, now));
            }
            else
            {
                index.put(key, all.size());
                all.add(new ResourceAmount(key, now));
            }
        }
        else
        {
            counts.removeLong(key);
            int i = index.getInt(key);
            if (i >= 0)
            {
                int last = all.size() - 1;
                if (i != last)
                {
                    ResourceAmount tail = all.get(last);
                    all.set(i, tail);
                    index.put(tail.resource(), i);
                }
                all.remove(last);
                index.removeInt(key);
            }
        }
    }

    private void clearSnapshot()
    {
        counts.clear();
        index.clear();
        all.clear();
        lastFullBuildGt = getGameTimeSafe();
    }

    private void fullRebuildSnapshot()
    {
        counts.clear();
        index.clear();
        all.clear();

        UnifiedStorage us = getUnifiedStorageForRsOrNull();
        if (us != null)
        {
            for (KeyAmount s : us.getStorage())
            {
                if (s == null || s.isEmpty()) continue;

                RSHelper.fromIStackToRSKey(s.key()).ifPresent(k -> {
                    long prev = counts.getLong(k);
                    long now = prev + s.amount();
                    if (prev == 0)
                    {
                        counts.put(k, now);
                        index.put(k, all.size());
                        all.add(new ResourceAmount(k, now));
                    }
                    else
                    {
                        counts.put(k, now);
                        int i = index.getInt(k);
                        if (i >= 0)
                        {
                            all.set(i, new ResourceAmount(k, now));
                        }
                        else
                        {
                            index.put(k, all.size());
                            all.add(new ResourceAmount(k, now));
                        }
                    }
                });
            }
        }

        lastFullBuildGt = getGameTimeSafe();
    }

    private void maybePeriodicFullRebuild()
    {
        if (FULL_REBUILD_INTERVAL_TICKS <= 0) return;
        long gt = getGameTimeSafe();
        if (gt - lastFullBuildGt >= FULL_REBUILD_INTERVAL_TICKS)
        {
            fullRebuildSnapshot();
        }
    }

    private long getGameTimeSafe()
    {
        if (this.level == null) return 0L;
        return this.level.getGameTime();
    }

    private void runOnServerThread(Runnable r)
    {
        if (this.level == null) return;
        MinecraftServer server = this.level.getServer();
        if (server == null) return;
        server.execute(r);
    }
}