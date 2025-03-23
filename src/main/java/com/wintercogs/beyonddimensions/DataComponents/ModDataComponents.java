package com.wintercogs.beyonddimensions.DataComponents;

import com.mojang.serialization.Codec;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;


import java.util.function.UnaryOperator;

public class ModDataComponents {

    public static DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, BeyondDimensions.MODID);

    // 存储维度id
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<Integer>> NET_ID_DATA = register(
            "net_id", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT)
    );

    // 存储不稳定时空碎片的倒计时
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<Long>> LONG_DATA = register(
            "long_data", builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG)
    );

    public static final DeferredHolder<DataComponentType<?>,DataComponentType<Long>> TIME_LINE = register(
      "time_line", builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG)
    );

    private static <T> DeferredHolder<DataComponentType<?>,DataComponentType<T>> register(String name, UnaryOperator<DataComponentType.Builder<T>> builder) {
        return DATA_COMPONENTS.register(name,()->  builder.apply(DataComponentType.builder()).build());
    }

    public static void register(IEventBus eventBus){
        DATA_COMPONENTS.register(eventBus);
    }
}
