package com.wintercogs.beyonddimensions.integration.module.rs.block;

import com.refinedmods.refinedstorage.api.storage.cache.IStorageCache;
import com.refinedmods.refinedstorage.api.storage.externalstorage.IExternalStorageContext;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.common.block.entity.NetedBlockEntity;
import com.wintercogs.beyonddimensions.integration.module.rs.RSHelper;
import com.wintercogs.beyonddimensions.integration.module.rs.init.RSModuleBlockEntities;
import com.wintercogs.beyonddimensions.integration.module.rs.storage.BD_RS120ExternalStorageFluidsMirror;
import com.wintercogs.beyonddimensions.integration.module.rs.storage.BD_RS120ExternalStorageItemsMirror;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RSNetPathwayBlockEntity extends NetedBlockEntity
{
    private final CopyOnWriteArrayList<Runnable> onRemoveTasks = new CopyOnWriteArrayList<>();
    private volatile boolean removalFired = false;

    // ====== mirrors（视图 + delta 队列）======
    private final BD_RS120ExternalStorageItemsMirror itemMirror = new BD_RS120ExternalStorageItemsMirror();
    private final BD_RS120ExternalStorageFluidsMirror fluidMirror = new BD_RS120ExternalStorageFluidsMirror();

    // ====== unified + 订阅句柄（只在 BE 持有）======
    private volatile @Nullable UnifiedStorage unified;
    private @Nullable AutoCloseable unifiedAnySub;
    private @Nullable AutoCloseable unifiedDeltaSub;

    private volatile boolean netTaskRegistered = false;

    public RSNetPathwayBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(RSModuleBlockEntities.RS_NET_PATHWAY_BLOCK_ENTITY.get(), pos, blockState);
        ensureNetTaskRegistered();
    }

    private void ensureNetTaskRegistered()
    {
        if (!netTaskRegistered)
        {
            netTaskRegistered = true;
            addNetChangeTask(this::handleDimNetChanged);
        }
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        handleDimNetChanged();
    }

    // ====== remove hook ======
    public void addRemoveTask(Runnable r)
    {
        if (r != null) onRemoveTasks.add(r);
    }

    public void removeRemoveTask(Runnable r)
    {
        if (r != null) onRemoveTasks.remove(r);
    }

    private void fireRemoveTasksOnce()
    {
        if (removalFired) return;
        removalFired = true;

        // 优先解绑 unified，断开引用链
        unbindUnified();

        for (Runnable r : onRemoveTasks)
        {
            try
            {
                r.run();
            }
            catch (Throwable ignored)
            {
            }
        }
        onRemoveTasks.clear();
    }

    @Override
    public void setRemoved()
    {
        super.setRemoved();
        fireRemoveTasksOnce();
    }

    @Override
    public void onChunkUnloaded()
    {
        super.onChunkUnloaded();
        fireRemoveTasksOnce();
    }

    // ====== adapter 使用的 API ======

    public @Nullable UnifiedStorage getUnifiedStorageOrNull()
    {
        DimensionsNet net = getNet();
        if (net == null) return null;

        UnifiedStorage us = net.getUnifiedStorage();
        if (us == null) return null;

        if (us.slotMaxSize == 0 && us.slotCapacity == 0) return null;

        return us;
    }

    // --- items ---
    public List<ItemStack> getItemsForContext()
    {
        List<ItemStack> base = itemMirror.getAllView();
        if (base.isEmpty()) return Collections.emptyList();
        return base;
    }

    public void flushItemsToRsCache(IStorageCache<ItemStack> cache, IExternalStorageContext ctx)
    {
        itemMirror.flushToRsCache(cache, ctx::acceptsItem, unified);
    }

    // --- fluids ---
    public List<FluidStack> getFluidsForContext()
    {
        List<FluidStack> base = fluidMirror.getAllView();
        if (base.isEmpty()) return Collections.emptyList();
        return base;
    }

    public void flushFluidsToRsCache(IStorageCache<FluidStack> cache, IExternalStorageContext ctx)
    {
        fluidMirror.flushToRsCache(cache, ctx::acceptsFluid, unified);
    }

    // ====== unified bind/unbind + subscriptions ======

    private void handleDimNetChanged()
    {
        DimensionsNet net = getNet();
        UnifiedStorage newUnified = (net != null) ? net.getUnifiedStorage() : null;

        if (newUnified != null && newUnified.slotMaxSize == 0 && newUnified.slotCapacity == 0)
        {
            newUnified = null;
        }

        if (newUnified == null)
        {
            if (unified != null || unifiedAnySub != null || unifiedDeltaSub != null)
            {
                unbindUnified();
            }
            else
            {
                // 未绑定：请求一次重同步，让 flush 把可能残留的已上报内容差量清空（目标为空）
                itemMirror.requestResync();
                fluidMirror.requestResync();
            }
            return;
        }

        if (newUnified != unified)
        {
            bindUnified(newUnified);
        }
    }

    private void bindUnified(@NotNull UnifiedStorage u)
    {
        // 仅关闭旧订阅；保留镜像快照(all)，以便对“旧→新”做差量过渡（重绑场景）
        closeSubscriptions();

        unified = u;

        // 整体重同步：在下一次 adapter.update() 时对 live 目标(u)与当前快照做差量
        itemMirror.requestResync();
        fluidMirror.requestResync();

        // AnyChange：整体变化 -> 请求一次差量重同步
        unifiedAnySub = unified.subscribeAnyWeak(this, be -> {
            be.runOnServerThread(() -> {
                be.itemMirror.requestResync();
                be.fluidMirror.requestResync();
            });
        });

        // Delta：把变化分发到 itemMirror / fluidMirror（仅入队，flush 时 lockstep 应用+推送）
        unifiedDeltaSub = unified.subscribeDeltaWeak(this, (be, type, size, insert) -> {
            be.runOnServerThread(() -> {
                if (be.unified == null) return;

                long diff = insert ? size : -size;

                // 物品
                RSHelper.fromIStackToItemStack(new KeyAmount(type, size)).ifPresent(stk -> be.itemMirror.onDelta(stk, diff));

                // 流体
                RSHelper.fromIStackToFluidStack(new KeyAmount(type, size)).ifPresent(fs -> be.fluidMirror.onDelta(fs, diff));
            });
        });
    }

    private void unbindUnified()
    {
        closeSubscriptions();
        unified = null;

        // 目标变为空：请求重同步，让下一次 flush 以差量方式移除已上报内容。
        // 若本 BE 即将被移除/卸载，RS 会在自身 invalidate 重建时直接丢弃本存储，
        // 这里的重同步即便不被 flush 也不会造成残留。
        itemMirror.requestResync();
        fluidMirror.requestResync();
    }

    private void closeSubscriptions()
    {
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
    }

    private void runOnServerThread(Runnable r)
    {
        if (this.level == null) return;
        MinecraftServer server = this.level.getServer();
        if (server == null) return;
        server.execute(r);
    }
}
