package com.wintercogs.beyonddimensions.Unit;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class TooltipHelper
{

    // 版本号 用于clear后处理
    private static final AtomicLong EPOCH = new AtomicLong(0);

    private static final Map<IStackType, List<Component>> NORMAL_CACHE   = new ConcurrentHashMap<>();
    private static final Map<IStackType, List<Component>> ADVANCED_CACHE = new ConcurrentHashMap<>();

    private static final Map<IStackType, CompletableFuture<List<Component>>> NORMAL_PENDING   = new ConcurrentHashMap<>();
    private static final Map<IStackType, CompletableFuture<List<Component>>> ADVANCED_PENDING = new ConcurrentHashMap<>();

    private static final ExecutorService TOOLTIP_EXECUTOR = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            r -> {
                Thread t = new Thread(r, "Tooltip-Loader");
                t.setDaemon(true);
                return t;
            });

    /* ---------- 统一的异步加载入口 ---------- */
    private static CompletableFuture<List<Component>> loadAsync(
            IStackType stack,
            Item.TooltipContext ctx,
            @Nullable Player player,
            TooltipFlag flag,
            Map<IStackType, List<Component>> cache,
            Map<IStackType, CompletableFuture<List<Component>>> pending
    ) {
        // 记录它生成时的 epoch
        final long taskEpoch = EPOCH.get();

        return pending.computeIfAbsent(stack, s ->
                CompletableFuture
                        .<List<Component>>supplyAsync(() -> stack.getTooltipLines(ctx, player, flag), TOOLTIP_EXECUTOR)
                        .whenComplete((tooltip, err) -> {
                            // 始终先把自己从 pending 移除，避免泄漏
                            pending.remove(stack);

                            if (err != null) {
                                ((Throwable)err).printStackTrace();
                                return;
                            }

                            // 只有 epoch 没变，才允许写回缓存
                            if (taskEpoch == EPOCH.get()) {
                                cache.put(stack, (List<Component>) tooltip);
                            }
                        })
        );
    }


    /* ---------- 对外 API ---------- */

    public static List<Component> getTooltipLines(
            IStackType stack,
            Item.TooltipContext ctx,
            @Nullable Player player,
            TooltipFlag flag
    ) {
        boolean advanced = flag.isAdvanced();
        Map<IStackType, List<Component>> cache   = advanced ? ADVANCED_CACHE   : NORMAL_CACHE;
        Map<IStackType, CompletableFuture<List<Component>>> pending = advanced ? ADVANCED_PENDING : NORMAL_PENDING;

        // ① 先查缓存
        List<Component> cached = cache.get(stack);
        if (cached != null) return cached;

        // ② 没缓存就异步加载（若已有正在加载的任务则复用）
        try {
            return loadAsync(stack, ctx, player, flag, cache, pending).get(); // 阻塞等待
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        } catch (ExecutionException ee) {
            ee.printStackTrace();
            return Collections.emptyList();
        }
    }

    /** 预读取若干 Stack 的 Tooltip，典型用在滚动列表或搜索结果批量展示前 */
    public static void readAsCache(
            List<IStackType> stacks,
            Item.TooltipContext ctx,
            @Nullable Player player,
            TooltipFlag flag
    ) {
        boolean advanced = flag.isAdvanced();
        Map<IStackType, List<Component>> cache   = advanced ? ADVANCED_CACHE   : NORMAL_CACHE;
        Map<IStackType, CompletableFuture<List<Component>>> pending = advanced ? ADVANCED_PENDING : NORMAL_PENDING;

        for (IStackType stack : stacks) {
            if (cache.containsKey(stack)) continue; // 已有缓存就略过
            // computeIfAbsent 保证同一 key 只有一个任务
            loadAsync(stack, ctx, player, flag, cache, pending);
        }
    }

    public static void clearCache()
    {
        // 修改版本号
        EPOCH.incrementAndGet();

        NORMAL_CACHE.clear();
        ADVANCED_CACHE.clear();
        NORMAL_PENDING.clear();
        ADVANCED_PENDING.clear();
    }
}


