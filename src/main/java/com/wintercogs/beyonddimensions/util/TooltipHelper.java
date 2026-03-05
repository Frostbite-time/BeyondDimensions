package com.wintercogs.beyonddimensions.util;

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class TooltipHelper
{

    private static final AtomicLong EPOCH = new AtomicLong(0);

    private static final Map<IStackKey<?>, List<Component>> NORMAL_CACHE = new ConcurrentHashMap<>();
    private static final Map<IStackKey<?>, List<Component>> ADVANCED_CACHE = new ConcurrentHashMap<>();

    private static final Map<IStackKey<?>, CompletableFuture<List<Component>>> NORMAL_PENDING = new ConcurrentHashMap<>();
    private static final Map<IStackKey<?>, CompletableFuture<List<Component>>> ADVANCED_PENDING = new ConcurrentHashMap<>();

    private static final ExecutorService TOOLTIP_EXECUTOR = Executors.newFixedThreadPool(
            Math.min(4, Math.max(1, Runtime.getRuntime().availableProcessors() / 4)),
            r -> {
                Thread t = new Thread(r, "Tooltip-Loader");
                t.setDaemon(true);
                return t;
            });

    /* ---------- 统一的异步加载入口 ---------- */
    private static CompletableFuture<List<Component>> loadAsync(
            KeyAmount stack,
            Item.TooltipContext ctx,
            @Nullable Player player,
            TooltipFlag flag,
            Map<IStackKey<?>, List<Component>> cache,
            Map<IStackKey<?>, CompletableFuture<List<Component>>> pending
    )
    {
        final long taskEpoch = EPOCH.get();
        final IStackKey<?> key = stack.key();

        return pending.computeIfAbsent(key, s -> {
            CompletableFuture<List<Component>> created = CompletableFuture.supplyAsync(
                    () -> s.getRender().getTooltipLines(s, stack.amount(), ctx, player, flag),
                    TOOLTIP_EXECUTOR
            );

            created.whenCompleteAsync((tooltip, err) -> {
                pending.remove(s, created);
                if (err != null)
                {
                    BeyondDimensions.LOGGER.error("Failed to load tooltip for {}", key, err);
                    return;
                }
                if (taskEpoch == EPOCH.get())
                {
                    cache.put(s, tooltip);
                }
            }, TOOLTIP_EXECUTOR);

            return created;
        });
    }


    /* ---------- 对外 API ---------- */

    /**
     * 获取指定键的提示内容。建议在调用此函数之前先将全部key通过readAsCache进行预读。
     * 否则会堵塞等待。
     */
    public static List<Component> getTooltipLines(
            KeyAmount stack,
            Item.TooltipContext ctx,
            @Nullable Player player,
            TooltipFlag flag
    )
    {
        boolean advanced = flag.isAdvanced();
        IStackKey<?> key = stack.key();
        Map<IStackKey<?>, List<Component>> cache = advanced ? ADVANCED_CACHE : NORMAL_CACHE;
        Map<IStackKey<?>, CompletableFuture<List<Component>>> pending = advanced ? ADVANCED_PENDING : NORMAL_PENDING;

        // 先查缓存
        List<Component> cached = cache.get(key);
        if (cached != null) return cached;

        // 再查异步任务
        CompletableFuture<List<Component>> future = pending.get(key);
        if (future == null)
        {
            future = loadAsync(stack, ctx, player, flag, cache, pending);
        }

        try
        {
            return future.get();
        }
        catch (InterruptedException ie)
        {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        }
        catch (ExecutionException ee)
        {
            BeyondDimensions.LOGGER.error("Failed to load tooltip for {}", key, ee);
            return Collections.emptyList();
        }
    }

    /**
     * 预读取若干 Stack 的 Tooltip，典型用在滚动列表或搜索结果批量展示前
     */
    public static void readAsCache(
            List<KeyAmount> stacks,
            Item.TooltipContext ctx,
            @Nullable Player player,
            TooltipFlag flag
    )
    {
        boolean advanced = flag.isAdvanced();
        Map<IStackKey<?>, List<Component>> cache = advanced ? ADVANCED_CACHE : NORMAL_CACHE;
        Map<IStackKey<?>, CompletableFuture<List<Component>>> pending = advanced ? ADVANCED_PENDING : NORMAL_PENDING;

        for (KeyAmount stack : stacks)
        {
            if (cache.containsKey(stack.key())) continue;
            // loadAsync内部自动过滤重复任务
            loadAsync(stack, ctx, player, flag, cache, pending);
        }
    }

    public static void clearCache()
    {
        EPOCH.incrementAndGet();

        NORMAL_CACHE.clear();
        ADVANCED_CACHE.clear();
        NORMAL_PENDING.clear();
        ADVANCED_PENDING.clear();
    }
}


