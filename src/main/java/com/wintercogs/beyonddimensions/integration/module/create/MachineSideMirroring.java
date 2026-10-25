package com.wintercogs.beyonddimensions.integration.module.create;

import com.wintercogs.beyonddimensions.api.machine.Machine;
import com.wintercogs.beyonddimensions.common.init.BDBlockEntities;
import com.wintercogs.beyonddimensions.common.machine.MachineSides;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 机械动力移动或放置机器（动态结构、蓝图）时的面配置：面配置跟着方块的正面走，转动后不用改；
 * 镜像后左右两面互换，仍连着原来的相邻方块
 */
public final class MachineSideMirroring
{
    private MachineSideMirroring()
    {
    }

    /**
     * 有面配置的机器的方块实体类型
     */
    public static List<BlockEntityType<?>> types()
    {
        return List.of(
                BDBlockEntities.NET_INTERFACE_BLOCK_ENTITY.get(),
                BDBlockEntities.NET_ENERGY_PATHWAY_BLOCK_ENTITY.get(),
                BDBlockEntities.NET_PUMP_BLOCK_ENTITY.get(),
                BDBlockEntities.NET_FURNACE_BLOCK_ENTITY.get(),
                BDBlockEntities.NET_BLAST_FURNACE_BLOCK_ENTITY.get(),
                BDBlockEntities.NET_SMOKER_BLOCK_ENTITY.get());
    }

    public static void apply(BlockEntity be, @Nullable Mirror mirror)
    {
        if (be instanceof Machine machine && mirror != null && mirror != Mirror.NONE)
            machine.setSettings(MachineSides.mirrored(machine));
    }
}
