package com.wintercogs.beyonddimensions.BlockEntity;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Block.ModBlocks;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetEnergyPathwayBlockEntity;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetInterfaceBlockEntity;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetPathwayBlockEntity;
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


    public static void register(IEventBus eventBus)
    {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
