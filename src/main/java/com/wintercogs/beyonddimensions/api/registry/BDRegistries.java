package com.wintercogs.beyonddimensions.api.registry;

import com.wintercogs.beyonddimensions.api.dimensionnet.data.NetDataType;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.machine.MachineSetting;
import com.wintercogs.beyonddimensions.api.machine.MachineType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.function.Supplier;

/**
 * BD 提供给附属的注册表
 */
public final class BDRegistries
{
    /**
     * 挂接在维度网络上的数据类型，见 {@link NetDataType}
     */
    public static final ResourceKey<Registry<NetDataType<?>>> NET_DATA_TYPE_KEY =
            ResourceKey.createRegistryKey(new ResourceLocation(BDConstants.MODID, "net_data_type"));

    private static final DeferredRegister<NetDataType<?>> NET_DATA_TYPE_REGISTRY = DeferredRegister.create(NET_DATA_TYPE_KEY, BDConstants.MODID);

    public static final Supplier<IForgeRegistry<NetDataType<?>>> NET_DATA_TYPES = NET_DATA_TYPE_REGISTRY.makeRegistry(RegistryBuilder::new);

    /**
     * 机器的设置，见 {@link MachineSetting}
     */
    public static final ResourceKey<Registry<MachineSetting<?>>> MACHINE_SETTING_KEY =
            ResourceKey.createRegistryKey(new ResourceLocation(BDConstants.MODID, "machine_setting"));

    private static final DeferredRegister<MachineSetting<?>> MACHINE_SETTING_REGISTRY = DeferredRegister.create(MACHINE_SETTING_KEY, BDConstants.MODID);

    public static final Supplier<IForgeRegistry<MachineSetting<?>>> MACHINE_SETTINGS = MACHINE_SETTING_REGISTRY.makeRegistry(RegistryBuilder::new);

    /**
     * 机器类型，见 {@link MachineType}
     */
    public static final ResourceKey<Registry<MachineType>> MACHINE_TYPE_KEY =
            ResourceKey.createRegistryKey(new ResourceLocation(BDConstants.MODID, "machine_type"));

    private static final DeferredRegister<MachineType> MACHINE_TYPE_REGISTRY = DeferredRegister.create(MACHINE_TYPE_KEY, BDConstants.MODID);

    public static final Supplier<IForgeRegistry<MachineType>> MACHINE_TYPES = MACHINE_TYPE_REGISTRY.makeRegistry(RegistryBuilder::new);

    private BDRegistries()
    {
    }

    /**
     * 由 BD 在模组总线上调用
     */
    public static void register(IEventBus modBus)
    {
        NET_DATA_TYPE_REGISTRY.register(modBus);
        MACHINE_SETTING_REGISTRY.register(modBus);
        MACHINE_TYPE_REGISTRY.register(modBus);
    }
}
