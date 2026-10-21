package com.wintercogs.beyonddimensions.integration.module.jade;

import com.wintercogs.beyonddimensions.common.block.NetedBlock;
import com.wintercogs.beyonddimensions.common.block.entity.BaseNetFurnaceBlockEntity;
import com.wintercogs.beyonddimensions.common.block.entity.NetInterfaceBlockEntity;
import com.wintercogs.beyonddimensions.common.block.entity.NetPathwayBlockEntity;
import com.wintercogs.beyonddimensions.common.block.entity.NetedBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

import java.util.List;

@WailaPlugin
public class BDJadePlugin implements IWailaPlugin
{
    @Override
    public void register(IWailaCommonRegistration registration)
    {
        registration.registerBlockDataProvider(NetedBlockNetworkProvider.INSTANCE, NetedBlockEntity.class);
        registration.registerBlockDataProvider(DeviceContentsProvider.INSTANCE, NetedBlockEntity.class);
        // 这些设备的能力按槽位暴露存储，空槽也算一格。接口与熔炉的资源由设备内容显示，网络通道不列出整个网络的资源
        for (Class<?> target : List.of(
                NetInterfaceBlockEntity.class, BaseNetFurnaceBlockEntity.class, NetPathwayBlockEntity.class))
        {
            registration.registerItemStorage(HideStorageProvider.instance(), target);
            registration.registerFluidStorage(HideStorageProvider.instance(), target);
            registration.registerEnergyStorage(HideStorageProvider.instance(), target);
        }
    }

    @Override
    public void registerClient(IWailaClientRegistration registration)
    {
        registration.registerBlockComponent(NetedBlockNetworkProvider.INSTANCE, NetedBlock.class);
        registration.registerBlockComponent(DeviceContentsComponent.INSTANCE, NetedBlock.class);
        registration.registerBlockComponent(TankListRemover.INSTANCE, NetedBlock.class);
    }
}
