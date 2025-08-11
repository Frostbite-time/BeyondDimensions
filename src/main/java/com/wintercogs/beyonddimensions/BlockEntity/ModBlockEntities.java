package com.wintercogs.beyonddimensions.BlockEntity;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Block.ModBlocks;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.*;
import com.wintercogs.beyonddimensions.Integration.Ars.Block.SourcePathwayBlockEntity;
import com.wintercogs.beyonddimensions.Integration.RS.Block.RSNetPathwayBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities
{

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BeyondDimensions.MODID);

    public static final Supplier<BlockEntityType<NetInterfaceBlockEntity>> NET_INTERFACE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "net_interface_block_entity",
            // 使用构建器创建方块实体类型.
            () -> BlockEntityType.Builder.of(
                            // 用于构造方块实体实例的供应商.
                            NetInterfaceBlockEntity::new,
                            // 可以拥有此方块实体的方块的可变参数.
                            // 这假设引用的方块作为 DeferredBlock<Block> 存在.
                            ModBlocks.NET_INTERFACE.get()
                    )
                    // 使用 null 构建；原版对参数进行了一些数据修复操作，我们不需要.
                    .build(null)
    );

    public static final Supplier<BlockEntityType<NetPathwayBlockEntity>> NET_PATHWAY_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "net_pathway_block_entity",
            // 使用构建器创建方块实体类型.
            () -> BlockEntityType.Builder.of(
                            // 用于构造方块实体实例的供应商.
                            NetPathwayBlockEntity::new,
                            // 可以拥有此方块实体的方块的可变参数.
                            // 这假设引用的方块作为 DeferredBlock<Block> 存在.
                            ModBlocks.NET_PATHWAY.get()
                    )
                    // 使用 null 构建；原版对参数进行了一些数据修复操作，我们不需要.
                    .build(null)
    );


    public static final Supplier<BlockEntityType<NetEnergyPathwayBlockEntity>> NET_ENERGY_PATHWAY_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "net_energy_pathway_block_entity",
            // 使用构建器创建方块实体类型.
            () -> BlockEntityType.Builder.of(
                            // 用于构造方块实体实例的供应商.
                            NetEnergyPathwayBlockEntity::new,
                            // 可以拥有此方块实体的方块的可变参数.
                            // 这假设引用的方块作为 DeferredBlock<Block> 存在.
                            ModBlocks.NET_ENERGY_PATHWAY.get()
                    )
                    // 使用 null 构建；原版对参数进行了一些数据修复操作，我们不需要.
                    .build(null)
    );

    public static final Supplier<BlockEntityType<NetTerminalBlockEntity>> NET_TERMINAL_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
      "net_terminal_block_entity",
            () -> BlockEntityType.Builder.of(
                    NetTerminalBlockEntity::new,
                    ModBlocks.NET_TERMINAL_BLOCK.get()
            ).build(null)
    );

    public static final Supplier<BlockEntityType<NetPumpBlockEntity>> NET_PUMP_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "net_pump_block_entity",
            () -> BlockEntityType.Builder.of(
                    NetPumpBlockEntity::new,
                    ModBlocks.NET_PUMP_BLOCK.get()
            ).build(null)
    );

    public static final Supplier<BlockEntityType<NetHopperBlockEntity>> NET_HOPPER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "net_hopper_block_entity",
            () -> BlockEntityType.Builder.of(
                    NetHopperBlockEntity::new,
                    ModBlocks.NET_HOPPER_BLOCK.get()
            ).build(null)
    );

    public static final Supplier<BlockEntityType<NetFurnaceBlockEntity>> NET_FURNACE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "net_furnace_block_entity",
            () -> BlockEntityType.Builder.of(
                    NetFurnaceBlockEntity::new,
                    ModBlocks.NET_FURNACE_BLOCK.get()
            ).build(null)
    );

    // 精致存储2---RS维度通道
    // 仅在模组存在时才注册实体
    public static Supplier<BlockEntityType<?>> RS_NET_PATHWAY_BLOCK_ENTITY;
    public static Supplier<BlockEntityType<?>> ARS_SOURCE_PATHWAY_BLOCK_ENTITY;
    public static void IntegrationRegister()
    {
        if(BeyondDimensions.RS_Loaded)
        {
            RS_NET_PATHWAY_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
                    "rs_net_pathway_block_entity",
                    () -> BlockEntityType.Builder.of(
                            RSNetPathwayBlockEntity::new,
                            ModBlocks.RS_NET_PATHWAY.get()
                    ).build(null)
            );
        }
        if(BeyondDimensions.ARS_Loaded)
        {
            ARS_SOURCE_PATHWAY_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
                    "ars_source_pathway_block_entity",
                    () -> BlockEntityType.Builder.of(
                            SourcePathwayBlockEntity::new,
                            ModBlocks.ARS_SOURCE_PATHWAY.get()
                    ).build(null)
            );
        }
    }

    public static void register(IEventBus eventBus)
    {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
