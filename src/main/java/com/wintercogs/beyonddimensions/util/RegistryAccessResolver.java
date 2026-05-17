package com.wintercogs.beyonddimensions.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

/**
 * - 希望这个类能按期望运行......
 */
public final class RegistryAccessResolver
{

    private static final HolderLookup.Provider BUILTIN =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    /**
     * 获取一个最合适的注册表来源
     */
    public static @NotNull HolderLookup.Provider resolve()
    {
        // 专用服或集成服
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv != null && srv.isSameThread()) return srv.registryAccess();

        // 客户端走Connection或当前level
        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            var mc = Minecraft.getInstance();
            var conn = mc.getConnection();
            if (conn != null) return conn.registryAccess();
            if (mc.level != null) return mc.level.registryAccess();
        }

        // 内建表回退
        return BUILTIN;
    }
}
