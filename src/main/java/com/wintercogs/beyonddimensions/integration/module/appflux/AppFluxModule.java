package com.wintercogs.beyonddimensions.integration.module.appflux;

import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;
import com.glodblock.github.appflux.common.me.key.type.FluxKeyType;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey;
import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.ae2.AEHelper;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.Optional;

@BDIntegrationModule(modId = OtherModIds.APPFLUX)
public class AppFluxModule implements IIntegrationModule
{
    @Override
    public String modId()
    {
        return OtherModIds.APPFLUX;
    }

    @Override
    public void onBootstrap(IEventBus modBus, IEventBus gameBus)
    {
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event)
    {
        AEHelper.ISTACK_TO_AEKEY_MAP.put(EnergyStackKey.ID, stackType -> Optional.of(FluxKey.of(EnergyType.FE)));
        AEHelper.AEKEY_TO_STACK_TYPE_MAP.put(FluxKeyType.TYPE, key -> Optional.of(EnergyStackKey.INSTANCE));
    }
}
