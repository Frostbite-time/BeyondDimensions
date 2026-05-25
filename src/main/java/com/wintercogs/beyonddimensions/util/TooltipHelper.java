package com.wintercogs.beyonddimensions.util;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Mod.EventBusSubscriber(modid = BDConstants.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class TooltipHelper
{

    private static final int MAX_LOADS_PER_TICK = 5;

    private static final AtomicLong CACHE_EPOCH = new AtomicLong(0);

    private static final Map<IStackKey<?>, List<Component>> NORMAL_CACHE = new HashMap<>();
    private static final Map<IStackKey<?>, List<Component>> ADVANCED_CACHE = new HashMap<>();

    private static final Set<IStackKey<?>> NORMAL_QUEUED = new HashSet<>();
    private static final Set<IStackKey<?>> ADVANCED_QUEUED = new HashSet<>();

    private static final Queue<TooltipRequest> PENDING = new ArrayDeque<>();

    private static Map<IStackKey<?>, List<Component>> cacheFor(TooltipFlag flag)
    {
        return flag.isAdvanced() ? ADVANCED_CACHE : NORMAL_CACHE;
    }

    private static Set<IStackKey<?>> queuedFor(TooltipFlag flag)
    {
        return flag.isAdvanced() ? ADVANCED_QUEUED : NORMAL_QUEUED;
    }

    private static void queueTooltip(
            KeyAmount stack,
            @Nullable Player player,
            TooltipFlag flag
    )
    {
        IStackKey<?> key = stack.key();
        Map<IStackKey<?>, List<Component>> cache = cacheFor(flag);
        Set<IStackKey<?>> queued = queuedFor(flag);

        if (cache.containsKey(key)) return;
        if (queued.add(key))
        {
            PENDING.offer(new TooltipRequest(stack, player, flag, CACHE_EPOCH.get()));
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
        {
            drainQueue(MAX_LOADS_PER_TICK);
        }
    }

    private static void drainQueue(int maxLoads)
    {
        int loaded = 0;
        TooltipRequest request;
        while (loaded < maxLoads && (request = PENDING.poll()) != null)
        {
            if (request.epoch() != CACHE_EPOCH.get()) continue;
            getTooltipLines(request.stack(), request.player(), request.flag());
            loaded++;
        }
    }


    /* ---------- 对外 API ---------- */

    /**
     * 获取指定键的提示内容。建议在调用此函数之前先将全部key通过readAsCache进行预读
     */
    public static List<Component> getTooltipLines(
            KeyAmount stack,
            @Nullable Player player,
            TooltipFlag flag
    )
    {
        IStackKey<?> key = stack.key();
        Map<IStackKey<?>, List<Component>> cache = cacheFor(flag);
        Set<IStackKey<?>> queued = queuedFor(flag);

        List<Component> cached = cache.get(key);
        if (cached != null)
        {
            queued.remove(key);
            return cached;
        }

        queued.remove(key);
        try
        {
            List<Component> tooltip = key.getRender().getTooltipLines(key, stack.amount(), player, flag);
            cache.put(key, tooltip);
            return tooltip;
        }
        catch (Throwable err)
        {
            BeyondDimensions.LOGGER.error("Failed to load tooltip for {}", key, err);
            List<Component> emptyTooltip = Collections.emptyList();
            cache.put(key, emptyTooltip);
            return emptyTooltip;
        }
    }

    /**
     * 预读取若干 Stack 的 Tooltip
     */
    public static void readAsCache(
            List<KeyAmount> stacks,
            @Nullable Player player,
            TooltipFlag flag
    )
    {
        for (KeyAmount stack : stacks)
        {
            queueTooltip(stack, player, flag);
        }
    }

    public static void clearCache()
    {
        CACHE_EPOCH.incrementAndGet();

        NORMAL_CACHE.clear();
        ADVANCED_CACHE.clear();
        NORMAL_QUEUED.clear();
        ADVANCED_QUEUED.clear();
        PENDING.clear();
    }

    private record TooltipRequest(
            KeyAmount stack,
            @Nullable Player player,
            TooltipFlag flag,
            long epoch
    )
    {
    }
}
