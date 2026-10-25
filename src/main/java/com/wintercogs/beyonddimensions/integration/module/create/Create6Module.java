package com.wintercogs.beyonddimensions.integration.module.create;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.contraption.transformable.MovedBlockTransformerRegistries;
import com.wintercogs.beyonddimensions.common.init.BDBlocks;
import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.create.contraption.NetInterfaceMountedStorageType;
import com.wintercogs.beyonddimensions.integration.module.create.contraption.NetInterfaceMovementBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * 用到机械动力 6.0 起才有的接口的联动：网络接口作为动态结构的存储，以及镜像时交换机器左右两面的配置
 */
@BDIntegrationModule(modId = OtherModIds.CREATE, versionRange = "[6.0.0,)")
public class Create6Module implements IIntegrationModule
{

    @Override
    public String modId()
    {
        return OtherModIds.CREATE;
    }

    @Override
    public void onBootstrap(IEventBus modBus, IEventBus gameBus)
    {
        NetInterfaceMountedStorageType.register(modBus);
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            MountedItemStorageType.REGISTRY.register(
                    BDBlocks.NET_INTERFACE.get(),
                    NetInterfaceMountedStorageType.NET_INTERFACE.get()
            );
            MovementBehaviour.REGISTRY.register(
                    BDBlocks.NET_INTERFACE.get(),
                    new NetInterfaceMovementBehaviour()
            );
            for (BlockEntityType<?> sided : MachineSideMirroring.types())
            {
                MovedBlockTransformerRegistries.BLOCK_ENTITY_TRANSFORMERS.register(sided,
                        (be, transform) -> MachineSideMirroring.apply(be, transform.mirror));
            }
        });
    }
}
