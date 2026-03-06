package com.wintercogs.beyonddimensions.integration.module.appars;

import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.ae2.AEHelper;
import com.wintercogs.beyonddimensions.integration.module.ars.storage.SourceStackKey;
import gripe._90.arseng.me.key.SourceKey;
import gripe._90.arseng.me.key.SourceKeyType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.Optional;

@BDIntegrationModule(modId = OtherModIds.ARS_ENG)
public class AppArsModule implements IIntegrationModule
{
    @Override
    public String modId()
    {
        return OtherModIds.ARS_ENG;
    }

    @Override
    public void onBootstrap(IEventBus modBus, IEventBus gameBus)
    {
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event)
    {
        AEHelper.ISTACK_TO_AEKEY_MAP.put(SourceStackKey.ID, stackType -> Optional.of(SourceKey.KEY));
        AEHelper.AEKEY_TO_STACK_TYPE_MAP.put(SourceKeyType.TYPE, key -> Optional.of(SourceStackKey.INSTANCE));
    }
}
