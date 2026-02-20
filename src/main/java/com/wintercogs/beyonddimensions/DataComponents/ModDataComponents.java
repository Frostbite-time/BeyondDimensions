package com.wintercogs.beyonddimensions.DataComponents;

import com.mojang.serialization.Codec;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataComponents.Custom.ItemStackContents;
import com.wintercogs.beyonddimensions.Machine.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public class ModDataComponents
{

    public static DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, BeyondDimensions.MODID);

    // 存储维度id
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> NET_ID_DATA = register(
            "net_id", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT)
    );

    // 存储不稳定时空碎片的倒计时
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> LONG_DATA = register(
            "long_data", builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> TIME_LINE = register(
            "time_line", builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemStackContents>> CRAFT_SLOTS = register(
            "craft_slots", builder -> builder.persistent(
                    ItemStackContents.CODEC
            ).networkSynchronized(
                    ItemStackContents.STREAM_CODEC
            )
    );

    // IStackType类已经实现了hashCode和equals，直接使用即可
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<KeyAmount>>> ISTACK_SLOTS = register(
            "istack_slots", builder -> builder.persistent(
                    KeyAmount.CODEC.listOf()
            ).networkSynchronized(
                    ByteBufCodecs.collection(
                            ArrayList::new,
                            KeyAmount.STREAM_CODEC
                    )
            )
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RedStoneControlMode>> CONTROL_MODE = register(
            "control_mode", builder -> builder.persistent(
                    Codec.STRING.xmap(
                            RedStoneControlMode::valueOf,
                            RedStoneControlMode::name
                    )
            ).networkSynchronized(
                    ByteBufCodecs.STRING_UTF8.map(
                            RedStoneControlMode::valueOf,
                            RedStoneControlMode::name
                    )
            )
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FilterMode>> FILTER_MODE = register(
            "filter_mode", builder -> builder.persistent(
                    Codec.STRING.xmap(
                            FilterMode::valueOf,
                            FilterMode::name
                    )
            ).networkSynchronized(
                    ByteBufCodecs.STRING_UTF8.map(
                            FilterMode::valueOf,
                            FilterMode::name
                    )
            )
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FuzzyMode>> FUZZY_MODE = register(
            "fuzzy_mode", builder -> builder.persistent(
                    Codec.STRING.xmap(
                            FuzzyMode::valueOf,
                            FuzzyMode::name
                    )
            ).networkSynchronized(
                    ByteBufCodecs.STRING_UTF8.map(
                            FuzzyMode::valueOf,
                            FuzzyMode::name
                    )
            )
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ReceiveMode>> RECEIVE_MODE = register(
            "receive_mode", builder -> builder.persistent(
                    Codec.STRING.xmap(
                            ReceiveMode::valueOf,
                            ReceiveMode::name
                    )
            ).networkSynchronized(
                    ByteBufCodecs.STRING_UTF8.map(
                            ReceiveMode::valueOf,
                            ReceiveMode::name
                    )
            )
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<HopperNBTMode>> HOPPER_NBT_MODE = register(
            "hopper_nbt_mode", builder -> builder.persistent(
                    Codec.STRING.xmap(
                            HopperNBTMode::valueOf,
                            HopperNBTMode::name
                    )
            ).networkSynchronized(
                    ByteBufCodecs.STRING_UTF8.map(
                            HopperNBTMode::valueOf,
                            HopperNBTMode::name
                    )
            )
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<HopperFluidMode>> HOPPER_FLUID_MODE = register(
            "hopper_fluid_mode", builder -> builder.persistent(
                    Codec.STRING.xmap(
                            HopperFluidMode::valueOf,
                            HopperFluidMode::name
                    )
            ).networkSynchronized(
                    ByteBufCodecs.STRING_UTF8.map(
                            HopperFluidMode::valueOf,
                            HopperFluidMode::name
                    )
            )
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<HopperRangeMode>> HOPPER_RANGE_MODE = register(
            "hopper_range_mode", builder -> builder.persistent(
                    Codec.STRING.xmap(
                            HopperRangeMode::valueOf,
                            HopperRangeMode::name
                    )
            ).networkSynchronized(
                    ByteBufCodecs.STRING_UTF8.map(
                            HopperRangeMode::valueOf,
                            HopperRangeMode::name
                    )
            )
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FeederMode>> FEEDER_MODE = register(
            "feeder_mode", builder -> builder.persistent(
                    Codec.STRING.xmap(
                            FeederMode::valueOf,
                            FeederMode::name
                    )
            ).networkSynchronized(
                    ByteBufCodecs.STRING_UTF8.map(
                            FeederMode::valueOf,
                            FeederMode::name
                    )
            )
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<HopperItemMode>> HOPPER_ITEM_MODE = register(
            "hopper_item_mode", builder -> builder.persistent(
                    Codec.STRING.xmap(
                            HopperItemMode::valueOf,
                            HopperItemMode::name
                    )
            ).networkSynchronized(
                    ByteBufCodecs.STRING_UTF8.map(
                            HopperItemMode::valueOf,
                            HopperItemMode::name
                    )
            )
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<HopperXpMode>> HOPPER_XP_MODE = register(
            "hopper_xp_mode", builder -> builder.persistent(
                    Codec.STRING.xmap(
                            HopperXpMode::valueOf,
                            HopperXpMode::name
                    )
            ).networkSynchronized(
                    ByteBufCodecs.STRING_UTF8.map(
                            HopperXpMode::valueOf,
                            HopperXpMode::name
                    )
            )
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<XpTransferSpeedMode>> XP_TRANSFER_SPEED_MODE = register(
            "xp_transfer_speed_mode", builder -> builder.persistent(
                    Codec.STRING.xmap(
                            XpTransferSpeedMode::valueOf,
                            XpTransferSpeedMode::name
                    )
            ).networkSynchronized(
                    ByteBufCodecs.STRING_UTF8.map(
                            XpTransferSpeedMode::valueOf,
                            XpTransferSpeedMode::name
                    )
            )
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> XP_NET_KEEP_MODE = register(
            "xp_net_keep_mode", builder -> builder
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
    );

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(String name, UnaryOperator<DataComponentType.Builder<T>> builder)
    {
        return DATA_COMPONENTS.register(name, () -> builder.apply(DataComponentType.builder()).build());
    }


    public static void register(IEventBus eventBus)
    {
        DATA_COMPONENTS.register(eventBus);
    }
}
