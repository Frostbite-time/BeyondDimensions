package com.wintercogs.beyonddimensions.Unit;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 统一的 RegistryAccess Provider 解析器：
 * - 覆盖尽可能多的相关事件，保证“及时更新”
 * - 引用级幂等：Provider 引用未变则不 bump，避免无谓失效
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
        bumpEpoch((force ? "强制重设:" : "重设:") + reason, prev, provider);
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
        Class<?> c = p.getClass();
        String n = c.getSimpleName();
        if (n == null || n.isEmpty()) {
            String full = c.getName();
            int i = full.lastIndexOf('.');
            n = (i >= 0 && i < full.length() - 1) ? full.substring(i + 1) : full;
        }
        return n + "@" + Integer.toHexString(System.identityHashCode(p));
    }

    /* ============================== 服务端（Dedicated + 集成服） ============================== */

    private static boolean onServerThread() {
        var srv = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        // srv == null 表示当前并不处在服务器生命周期上下文（比如纯客户端菜单）
        return srv != null && srv.isSameThread();
    }
    private static boolean isDedicatedProcess() {
        return FMLEnvironment.dist == Dist.DEDICATED_SERVER; // 物理侧：专用服进程 or 客户端进程
    }
    private static String serverFlavorLabel() {
        return isDedicatedProcess() ? "[服务端-专用服务器]" : "[服务端-集成服务器]";
    }
    private static String clientLabel() { return "[客户端]"; }

    @EventBusSubscriber(modid = BeyondDimensions.MODID, bus = EventBusSubscriber.Bus.GAME)
    public static final class ServerEvents
    {

        /** 服务器完整启动（专服）或集成服完成启动（单人局） */
        @SubscribeEvent
        public static void onServerStarted(ServerStartedEvent e)
        {
            if (!onServerThread()) return;
            setProvider(e.getServer().registryAccess(), serverFlavorLabel() + "服务器启动完成：切换为当前服务器 RegistryAccess");
        }

        /** 维度加载（仅逻辑服务器线程处理） */
        @SubscribeEvent
        public static void onServerLevelLoad(LevelEvent.Load e)
        {
            if (!onServerThread()) return;
            if (e.getLevel() instanceof ServerLevel sl)
            {
                setProvider(sl.registryAccess(), serverFlavorLabel() + "维度加载完成：切换为该维度所在的 RegistryAccess");
            }
        }

        /** 标签/数据更新（仅逻辑服务器线程处理，强制 bump 更稳妥） */
        @SubscribeEvent
        public static void onTagsUpdated(net.neoforged.neoforge.event.TagsUpdatedEvent e) {
            if (!onServerThread()) return; // 过滤掉客户端线程上的同名事件
            setProviderInternal(
                    e.getRegistryAccess(),
                    serverFlavorLabel() + "标签已更新（原因：" + String.valueOf(e.getUpdateCause()) + "）",
                    true // 引用不变也 bump，避免漏失效
            );
        }

        /** 服务器完全停止（专服）或集成服关闭 */
        @SubscribeEvent
        public static void onServerStopped(ServerStoppedEvent e) {
            if (!onServerThread()) return;
            clearToBuiltin(serverFlavorLabel() + "服务器停止：清回内建 RegistryAccess");
        }

    }

    /* ============================== 客户端 ============================== */
    @EventBusSubscriber(modid = BeyondDimensions.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static final class ClientEvents
    {
        private static final java.util.concurrent.atomic.AtomicBoolean CLIENT_CONNECTED = new java.util.concurrent.atomic.AtomicBoolean(false);

        /** 客户端开始登录 */
        @SubscribeEvent
        public static void onClientLoggingIn(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingIn e)
        {
            CLIENT_CONNECTED.set(true);
            if (e.getPlayer() != null && e.getPlayer().level() instanceof net.minecraft.client.multiplayer.ClientLevel cl)
            {
                setProvider(cl.registryAccess(), clientLabel() + "客户端开始登录");
            }
        }

        /** 世界/维度加载：ClientLevel 就绪 */
        @SubscribeEvent
        public static void onLevelLoad(LevelEvent.Load e)
        {
            if (e.getLevel() instanceof net.minecraft.client.multiplayer.ClientLevel cl)
            {
                setProvider(cl.registryAccess(), clientLabel() + "就绪：ClientLevel 已加载");
            }
        }

        /** 客户端收包或本地 /reload 强制 bump */
        @SubscribeEvent
        public static void onTagsUpdated(TagsUpdatedEvent e)
        {
            // 仅接收包时强制更新
            boolean force = (e.getUpdateCause() == net.neoforged.neoforge.event.TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED);
            setProviderInternal(
                    e.getRegistryAccess(),
                    clientLabel() + "标签已更新（原因：" + String.valueOf(e.getUpdateCause()) + "）",
                    force
            );
        }

        /** 客户端登出时清回 builtin */
        @SubscribeEvent
        public static void onClientLoggingOut(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e)
        {
            if (CLIENT_CONNECTED.compareAndSet(true, false)) {
                clearToBuiltin(clientLabel() + "登出：清回内建 RegistryAccess");
            }
        }
    }
}
