package com.wintercogs.beyonddimensions.api.machine;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * 能写进配置卡、也能从配置卡读回的配置。机器的默认实现导出它的设置，
 * 有更多配置的机器重写这两个方法、先调用默认实现，再写入或读回自己的数据，例如标记槽
 */
public interface Configurable
{
    /**
     * 把配置写进 config；配置卡把它连同机器类型一起保存
     */
    void exportConfig(CompoundTag config, HolderLookup.Provider registries);

    /**
     * 应用配置卡上的配置。卡上的配置来自同一类型的机器，但可能来自更早或更晚的版本：读不出或不合法的部分跳过
     */
    void applyConfig(CompoundTag config, HolderLookup.Provider registries);
}
