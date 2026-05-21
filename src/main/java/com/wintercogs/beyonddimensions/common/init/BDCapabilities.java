package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.common.block.entity.BaseNetFurnaceBlockEntity;
import com.wintercogs.beyonddimensions.common.block.entity.NetEnergyPathwayBlockEntity;
import com.wintercogs.beyonddimensions.common.block.entity.NetInterfaceBlockEntity;
import com.wintercogs.beyonddimensions.common.block.entity.NetPathwayBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = BDConstants.MODID)
public class BDCapabilities
{
    @SubscribeEvent
    public static void onRegisterCaps(RegisterCapabilitiesEvent event)
    {
        BaseNetFurnaceBlockEntity.registerItemHandlerCapability(event, BDBlockEntities.NET_FURNACE_BLOCK_ENTITY);
        BaseNetFurnaceBlockEntity.registerItemHandlerCapability(event, BDBlockEntities.NET_BLAST_FURNACE_BLOCK_ENTITY);
        BaseNetFurnaceBlockEntity.registerItemHandlerCapability(event, BDBlockEntities.NET_SMOKER_BLOCK_ENTITY);
        NetInterfaceBlockEntity.registerCapability(event);
        NetPathwayBlockEntity.registerCapability(event);
        NetEnergyPathwayBlockEntity.registerCapability(event);
    }
}
