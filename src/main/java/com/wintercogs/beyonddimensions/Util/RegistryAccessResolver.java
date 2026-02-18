package com.wintercogs.beyonddimensions.Util;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.NotNull;

/**
 * - 希望这个类能按期望运行......
 */
public final class RegistryAccessResolver
{

    private static final HolderLookup.Provider BUILTIN =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    public static @NotNull HolderLookup.Provider resolve()
    {
        // 1) 若当前在服务端逻辑线程（专服或集成服）
        var srv = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (srv != null && srv.isSameThread()) return srv.registryAccess();

        // 2) 客户端优先用 Connection（与网络来的 Holder 同 owner）
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT)
        {
            var mc = net.minecraft.client.Minecraft.getInstance();
            var conn = mc.getConnection();
            if (conn != null) return conn.registryAccess();
            if (mc.level != null) return mc.level.registryAccess();
        }

        // 3) 主菜单/离线兜底
        return BUILTIN;
    }
}
