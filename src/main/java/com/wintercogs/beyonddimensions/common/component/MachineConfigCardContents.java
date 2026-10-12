package com.wintercogs.beyonddimensions.common.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wintercogs.beyonddimensions.api.machine.MachineType;
import com.wintercogs.beyonddimensions.api.registry.BDRegistries;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * 配置卡记下的配置：复制自哪一类机器，以及那台机器导出的配置
 */
public record MachineConfigCardContents(ResourceLocation machineType, CompoundTag config)
{
    public static final Codec<MachineConfigCardContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("machine").forGetter(MachineConfigCardContents::machineType),
            CompoundTag.CODEC.fieldOf("config").forGetter(MachineConfigCardContents::config)
    ).apply(instance, MachineConfigCardContents::new));

    public static final StreamCodec<ByteBuf, MachineConfigCardContents> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, MachineConfigCardContents::machineType,
            ByteBufCodecs.COMPOUND_TAG, MachineConfigCardContents::config,
            MachineConfigCardContents::new);

    public MachineConfigCardContents
    {
        config = config.copy();
    }

    @Override
    public CompoundTag config()
    {
        return config.copy();
    }

    /**
     * 卡上记的机器类型；对应的模组已移除时为 null
     */
    public @Nullable MachineType type()
    {
        return BDRegistries.MACHINE_TYPES.get(machineType);
    }
}
