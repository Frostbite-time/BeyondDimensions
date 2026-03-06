package com.wintercogs.beyonddimensions.integration.module.appifs;

import com.buuz135.soulplied_energistics.applied.SoulAEKeyType;
import com.buuz135.soulplied_energistics.applied.SoulKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.WardenSoulStackKey;
import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.ae2.AEHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.Optional;

@BDIntegrationModule(modId = OtherModIds.SOULPLIED_ENERGISTICS)
public class AppIfsModule implements IIntegrationModule
{
    @Override
    public String modId()
    {
        return OtherModIds.SOULPLIED_ENERGISTICS;
    }

    @Override
    public void onBootstrap(IEventBus modBus, IEventBus gameBus)
    {

    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event)
    {
        AEHelper.ISTACK_TO_AEKEY_MAP.put(WardenSoulStackKey.ID, stackType -> Optional.of(SoulKey.INSTANCE));
        AEHelper.AEKEY_TO_STACK_TYPE_MAP.put(SoulAEKeyType.TYPE, key -> Optional.of(WardenSoulStackKey.INSTANCE));
    }
}
