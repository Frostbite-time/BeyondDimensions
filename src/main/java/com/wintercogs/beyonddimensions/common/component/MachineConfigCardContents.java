package com.wintercogs.beyonddimensions.common.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wintercogs.beyonddimensions.api.machine.MachineType;
import com.wintercogs.beyonddimensions.api.registry.BDRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
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
     * 配置卡物品 NBT 中的内容；读不出时为 null
     */
    public static @Nullable MachineConfigCardContents fromTag(Tag tag)
    {
        return CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(null);
    }

    /**
     * 写进配置卡物品 NBT 的内容
     */
    public Tag toTag()
    {
        return CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseThrow();
    }

    /**
     * 卡上记的机器类型；对应的模组已移除时为 null
     */
    public @Nullable MachineType type()
    {
        return BDRegistries.MACHINE_TYPES.get().getValue(machineType);
    }
}
