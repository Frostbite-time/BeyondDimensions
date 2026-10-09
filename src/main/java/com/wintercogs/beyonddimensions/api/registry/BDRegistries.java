package com.wintercogs.beyonddimensions.api.registry;

import com.wintercogs.beyonddimensions.api.dimensionnet.data.NetDataType;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * BD 提供给附属的注册表
 */
public final class BDRegistries
{
    /**
     * 挂接在维度网络上的数据类型，见 {@link NetDataType}
     */
    public static final ResourceKey<Registry<NetDataType<?>>> NET_DATA_TYPE_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(BDConstants.MODID, "net_data_type"));

    public static final Registry<NetDataType<?>> NET_DATA_TYPES = new RegistryBuilder<>(NET_DATA_TYPE_KEY).create();

    private BDRegistries()
    {
    }

    /**
     * 由 BD 在模组总线上调用
     */
    public static void register(NewRegistryEvent event)
    {
        event.register(NET_DATA_TYPES);
    }
}
