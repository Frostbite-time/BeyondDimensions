package com.wintercogs.beyonddimensions.integration.module.jade;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.List;

/**
 * 不列出存储：先于 Jade 的通用存储生效并返回空列表，Jade 就不显示这类存储。
 * 作用同新版 Jade 的 HideThingsExtensionProvider，Jade 11 没有提供
 */
final class HideStorageProvider<IN, OUT> implements IServerExtensionProvider<IN, OUT>
{
    private static final HideStorageProvider<?, ?> INSTANCE = new HideStorageProvider<>();
    private static final ResourceLocation UID = new ResourceLocation(BDConstants.MODID, "hide_storage");

    private HideStorageProvider()
    {
    }

    @SuppressWarnings("unchecked")
    static <IN, OUT> HideStorageProvider<IN, OUT> instance()
    {
        return (HideStorageProvider<IN, OUT>) INSTANCE;
    }

    @Override
    public List<ViewGroup<OUT>> getGroups(ServerPlayer player, ServerLevel level, IN target, boolean showDetails)
    {
        return List.of();
    }

    @Override
    public ResourceLocation getUid()
    {
        return UID;
    }
}
