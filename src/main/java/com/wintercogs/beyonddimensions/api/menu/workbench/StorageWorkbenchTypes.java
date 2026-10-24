package com.wintercogs.beyonddimensions.api.menu.workbench;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 存储界面工作区里的工作台，下拉框按注册顺序列出。BD 自带的合成台、铁砧、锻造台与切石机也在这里注册，排在最前。
 * 客户端与服务端都要注册，在通用初始化中完成：
 * <pre>{@code
 * StorageWorkbenchTypes.register(ResourceLocation.fromNamespaceAndPath("example", "loom"), new StorageWorkbenchType()
 * {
 *     public MenuWidget createWidget(DimensionsNetMenu menu)
 *     {
 *         return new LoomWidget(menu);
 *     }
 * });
 * }</pre>
 */
public final class StorageWorkbenchTypes
{
    public static final ResourceLocation CRAFTING = ResourceLocation.fromNamespaceAndPath("beyonddimensions", "crafting");
    public static final ResourceLocation ANVIL = ResourceLocation.fromNamespaceAndPath("beyonddimensions", "anvil");
    public static final ResourceLocation SMITHING = ResourceLocation.fromNamespaceAndPath("beyonddimensions", "smithing");
    public static final ResourceLocation STONECUTTER = ResourceLocation.fromNamespaceAndPath("beyonddimensions", "stonecutter");

    private record Entry(ResourceLocation id, StorageWorkbenchType type)
    {
    }

    private static volatile List<Entry> entries = List.of();

    private StorageWorkbenchTypes()
    {
    }

    /**
     * 注册一个工作台，排在已注册的之后
     */
    public static synchronized void register(ResourceLocation id, StorageWorkbenchType type)
    {
        if (get(id) != null)
            throw new IllegalStateException("Storage workbench " + id + " is already registered. Registered, in order: " + ids());
        List<Entry> next = new ArrayList<>(entries);
        next.add(new Entry(id, type));
        entries = List.copyOf(next);
    }

    /**
     * 已注册的工作台，按下拉框的顺序
     */
    public static List<ResourceLocation> ids()
    {
        return entries.stream().map(Entry::id).toList();
    }

    public static @Nullable StorageWorkbenchType get(ResourceLocation id)
    {
        for (Entry entry : entries)
        {
            if (entry.id().equals(id))
                return entry.type();
        }
        return null;
    }

    /**
     * 这个玩家能用的工作台，按注册顺序；打开存储界面时在服务端调用一次
     */
    @ApiStatus.Internal
    public static List<ResourceLocation> availableFor(ServerPlayer player)
    {
        List<ResourceLocation> available = new ArrayList<>();
        for (Entry entry : entries)
        {
            if (entry.type().isAvailable(player))
                available.add(entry.id());
        }
        return List.copyOf(available);
    }
}
