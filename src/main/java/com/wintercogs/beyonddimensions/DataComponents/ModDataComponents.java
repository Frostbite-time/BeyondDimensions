package com.wintercogs.beyonddimensions.DataComponents;

import com.mojang.serialization.Codec;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Unit.CodecHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
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

    public static final DeferredHolder<DataComponentType<?>,DataComponentType<NonNullList<ItemStack>>> CRAFT_SLOTS = register(
      "craft_slots", builder -> builder.persistent(
                    CodecHelper.nonNullListMutableCodecOf(ItemStack.OPTIONAL_CODEC)
            ).networkSynchronized(
                    ByteBufCodecs.collection(
                            NonNullList::createWithCapacity,
                            ItemStack.OPTIONAL_STREAM_CODEC
                    )
            )
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<IStackType>>> ISTACK_SLOTS = register(
      "istack_slots", builder -> builder.persistent(
                IStackType.CODEC.listOf()
            ).networkSynchronized(
                    ByteBufCodecs.collection(
                            ArrayList::new,
                            new StreamCodec<RegistryFriendlyByteBuf, IStackType>()
                            {
                                @Override
                                public void encode(RegistryFriendlyByteBuf buf, IStackType stackType)
                                {
                                    stackType.serialize(buf);
                                }

                                @Override
                                public IStackType decode(RegistryFriendlyByteBuf byteBuf)
                                {
                                    return IStackType.deserializeCommon(byteBuf);
                                }
                            }
                    )
            )
    );

    private static <T> DeferredHolder<DataComponentType<?>,DataComponentType<T>> register(String name, UnaryOperator<DataComponentType.Builder<T>> builder) {
        return DATA_COMPONENTS.register(name,()->  builder.apply(DataComponentType.builder()).build());
    }


    public static void register(IEventBus eventBus){
        DATA_COMPONENTS.register(eventBus);
    }
}
