package com.wintercogs.beyonddimensions.util;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 搜索用的资源提示框缓存（仅客户端）。
 * <p>
 * 缓存与预取队列都有上限；延迟的预取请求不持有玩家或世界。
 * 切换世界、玩家、语言或重载资源后，整个缓存进入新的世代。
 */
@EventBusSubscriber(modid = BDConstants.MODID, value = Dist.CLIENT)
public final class TooltipHelper
{
    public static final int MAX_CACHED = 2048;
    public static final int MAX_PENDING = 256;
    private static final int PREFETCH_PER_TICK = 5;

    private record Key(IStackKey<?> resource, long amount, boolean advanced) {}

    private static final AtomicLong REQUESTED_EPOCH = new AtomicLong();
    private static final LinkedHashMap<Key, List<Component>> CACHE = new LinkedHashMap<>(256, 0.75f, true);
    private static final LinkedHashMap<Key, KeyAmount> PENDING = new LinkedHashMap<>();
    private static long observedEpoch = -1;
    private static WeakReference<Level> world = new WeakReference<>(null);
    private static WeakReference<Player> player = new WeakReference<>(null);
    private static Language language;

    private TooltipHelper()
    {
    }

    private static void ensureContext()
    {
        Minecraft mc = Minecraft.getInstance();
        Language currentLanguage = Language.getInstance();
        if (observedEpoch == REQUESTED_EPOCH.get() && world.get() == mc.level && player.get() == mc.player && language == currentLanguage)
            return;
        CACHE.clear();
        PENDING.clear();
        world = new WeakReference<>(mc.level);
        player = new WeakReference<>(mc.player);
        language = currentLanguage;
        observedEpoch = REQUESTED_EPOCH.incrementAndGet();
    }

    /**
     * 当前缓存世代；世代变化说明之前得到的提示框文本已经失效
     */
    public static long epoch()
    {
        ensureContext();
        return observedEpoch;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        ensureContext();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null)
            return;
        for (int count = 0; count < PREFETCH_PER_TICK && !PENDING.isEmpty(); count++)
        {
            Iterator<Map.Entry<Key, KeyAmount>> iterator = PENDING.entrySet().iterator();
            Map.Entry<Key, KeyAmount> next = iterator.next();
            iterator.remove();
            TooltipFlag flag = next.getKey().advanced() ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
            getTooltipLines(next.getValue(), Item.TooltipContext.of(mc.level), mc.player, flag);
        }
    }

    public static List<Component> getTooltipLines(KeyAmount stack, Item.TooltipContext context, @Nullable Player currentPlayer, TooltipFlag flag)
    {
        ensureContext();
        Key key = new Key(stack.key(), stack.amount(), flag.isAdvanced());
        PENDING.remove(key);
        List<Component> cached = CACHE.get(key);
        if (cached != null)
            return cached;

        List<Component> result;
        try
        {
            result = List.copyOf(stack.key().getRender().getTooltipLines(stack.key(), stack.amount(), context, currentPlayer, flag));
        }
        catch (RuntimeException failure)
        {
            BeyondDimensions.LOGGER.warn("Could not load tooltip for resource type {}", stack.key().getTypeId(), failure);
            result = List.of();
        }
        CACHE.put(key, result);
        while (CACHE.size() > MAX_CACHED)
        {
            removeOldest(CACHE);
        }
        return result;
    }

    /**
     * 预取这些资源的提示框。搜索按单个数量匹配；队列已满时丢弃最早的请求
     */
    public static void readAsCache(List<KeyAmount> stacks, Item.TooltipContext context, @Nullable Player currentPlayer, TooltipFlag flag)
    {
        ensureContext();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.player != currentPlayer)
            return;
        for (KeyAmount stack : stacks)
        {
            if (stack.isEmpty())
                continue;
            Key key = new Key(stack.key(), 1, flag.isAdvanced());
            if (CACHE.containsKey(key))
                continue;
            PENDING.putIfAbsent(key, new KeyAmount(stack.key(), 1));
            while (PENDING.size() > MAX_PENDING)
            {
                removeOldest(PENDING);
            }
        }
    }

    public static void clearCache()
    {
        REQUESTED_EPOCH.incrementAndGet();
        if (Minecraft.getInstance().isSameThread())
            ensureContext();
    }

    private static void removeOldest(LinkedHashMap<Key, ?> map)
    {
        Iterator<Key> oldest = map.keySet().iterator();
        oldest.next();
        oldest.remove();
    }

    @EventBusSubscriber(modid = BDConstants.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Reloads
    {
        @SubscribeEvent
        public static void register(RegisterClientReloadListenersEvent event)
        {
            event.registerReloadListener((ResourceManagerReloadListener) manager -> REQUESTED_EPOCH.incrementAndGet());
        }
    }
}
