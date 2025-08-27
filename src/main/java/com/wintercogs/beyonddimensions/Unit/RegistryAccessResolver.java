package com.wintercogs.beyonddimensions.Unit;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 统一的 RegistryAccess Provider 解析器：
 * - 覆盖尽可能多的相关事件，保证“及时更新”
 * - 引用级幂等：Provider 引用未变则不 bump，避免无谓失效
 * - forceBump 开关（默认 false）：仅对 TagsUpdatedEvent 生效；开启后每次标签/数据更新都强制 bump
 * <p>
 * - 不在 ServerStopping 阶段 clearToBuiltin，避免保存/收尾时 bump 导致哈希失配
 * - 客户端 Unload 时清回 builtin
 * - 希望这个类能按期望运行......
 */
public final class RegistryAccessResolver {

    /** 主菜单/无世界阶段所用的“可用且稳定”的只读 Provider */
    private static final HolderLookup.Provider BUILTIN_PROVIDER =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    /** 当前 Provider（可能是 builtin / server / clientLevel） */
    private static final AtomicReference<HolderLookup.Provider> CURRENT =
            new AtomicReference<>(BUILTIN_PROVIDER);

    /** 递增版本号；外部以此丢弃缓存 */
    private static final AtomicLong EPOCH = new AtomicLong(0);

    private RegistryAccessResolver() {}

    /* ============================== 外部 API ============================== */

    /** 当前 Provider（永不为 null） */
    public static HolderLookup.Provider current()
    {
        HolderLookup.Provider p = CURRENT.get();
        return p != null ? p : BUILTIN_PROVIDER;
    }

    /** 当前 epoch */
    public static long epoch()
    {
        return EPOCH.get();
    }

    /** 通用设置（引用级幂等；force=false） */
    public static void setProvider(HolderLookup.Provider provider, String reason)
    {
        setProviderInternal(provider, reason, false);
    }

    /** 清回 builtin（引用级幂等） */
    public static void clearToBuiltin(String reason)
    {
        HolderLookup.Provider prev = CURRENT.get();
        if (prev == BUILTIN_PROVIDER)
        {
            return; // 幂等：已是 builtin
        }
        CURRENT.set(BUILTIN_PROVIDER);
        bumpEpoch("执行清理：" + reason, prev, BUILTIN_PROVIDER);
    }

    /* ============================== 内部实现 ============================== */

    /** 带“强制 bump”选项的内部设置（仅给 TagsUpdatedEvent 调用 force=true） */
    private static void setProviderInternal(HolderLookup.Provider provider, String reason, boolean force)
    {
        if (provider == null) provider = BUILTIN_PROVIDER;
        HolderLookup.Provider prev = CURRENT.get();

        // 引用级幂等：引用未变且不强制 则 不变更、不 bump
        if (!force && prev == provider)
        {
            return;
        }

        // 写入（即使引用未变，但 force=true 也会 bump）
        CURRENT.set(provider);
        bumpEpoch((force ? "强制重设为:" : "设为:") + reason, prev, provider);
    }

    /** 实际 bump + 简洁日志 */
    private static void bumpEpoch(String reason, HolderLookup.Provider from, HolderLookup.Provider to)
    {
        long v = EPOCH.incrementAndGet();
        BeyondDimensions.LOGGER.info(
                "RegistryAccessResolver 版本变化 -> versions={} | reason={} | from={} -> to={}",
                v, reason, shortId(from), shortId(to)
        );
    }

    private static String shortId(HolderLookup.Provider p)
    {
        if (p == null) return "null";
        return p.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(p));
    }

    /* ============================== 服务端（Dedicated + 集成服） ============================== */
    @EventBusSubscriber(modid = BeyondDimensions.MODID, bus = EventBusSubscriber.Bus.GAME)
    public static final class ServerEvents
    {

        /** 服务器完整启动 */
        @SubscribeEvent
        public static void onServerStarted(ServerStartedEvent e)
        {
            MinecraftServer srv = e.getServer();
            setProvider(srv.registryAccess(), "服务端启动");
        }

        /** 数据包同步点 */
        @SubscribeEvent
        public static void onDatapackSync(OnDatapackSyncEvent e)
        {
            MinecraftServer srv = e.getPlayerList().getServer();
            setProvider(srv.registryAccess(), "服务端数据包同步");
        }

        /** 标签/数据更新完成 可选强制 bump */
        @SubscribeEvent
        public static void onTagsUpdated(TagsUpdatedEvent e)
        {
            setProviderInternal(
                    e.getRegistryAccess(),
                    "服务端数据包或标签更新：" + Objects.toString(e.getUpdateCause(), "unknown"),
                    false // 优先不强制，仅用幂等判断，如有问题，后续再改
            );
        }

        /** 维度加载 */
        @SubscribeEvent
        public static void onServerLevelLoad(LevelEvent.Load e)
        {
            if (e.getLevel() instanceof ServerLevel sl)
            {
                setProvider(sl.registryAccess(), "维度加载");
            }
        }

        /** 服务器完全停止后 */
        @SubscribeEvent
        public static void onServerStopped(ServerStoppedEvent e)
        {
            clearToBuiltin("server_stopped");
        }
    }

    /* ============================== 客户端 ============================== */
    @EventBusSubscriber(modid = BeyondDimensions.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static final class ClientEvents
    {

        /** 客户端开始登录 */
        @SubscribeEvent
        public static void onClientLoggingIn(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingIn e)
        {
            if (e.getPlayer() != null && e.getPlayer().level() instanceof net.minecraft.client.multiplayer.ClientLevel cl)
            {
                setProvider(cl.registryAccess(), "客户端开始登录");
            }
        }

        /** 世界/维度加载：ClientLevel 就绪 */
        @SubscribeEvent
        public static void onLevelLoad(LevelEvent.Load e)
        {
            if (e.getLevel() instanceof net.minecraft.client.multiplayer.ClientLevel cl)
            {
                setProvider(cl.registryAccess(), "客户端就绪");
            }
        }

        /** 客户端收包或本地 /reload 后：权威刷新点；可选强制 bump */
        @SubscribeEvent
        public static void onTagsUpdated(TagsUpdatedEvent e)
        {
            setProviderInternal(
                    e.getRegistryAccess(),
                    "客户端接收到数据包或标签更新：" + Objects.toString(e.getUpdateCause(), "unknown"),
                    true
            );
        }

        /** 世界卸载 */
        @SubscribeEvent
        public static void onLevelUnload(LevelEvent.Unload e)
        {
            if (e.getLevel() instanceof net.minecraft.client.multiplayer.ClientLevel)
            {
                clearToBuiltin("客户端发生世界卸载");
            }
        }

        /** 断开连接 */
        @SubscribeEvent
        public static void onClientLoggingOut(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e)
        {
            clearToBuiltin("客户端登出");
        }
    }
}
