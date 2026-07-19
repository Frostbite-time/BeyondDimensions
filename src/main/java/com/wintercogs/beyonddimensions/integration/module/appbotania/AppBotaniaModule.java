package com.wintercogs.beyonddimensions.integration.module.appbotania;

import appbot.ae2.ManaKey;
import appbot.ae2.ManaKeyType;
import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.ae2.AEHelper;
import com.wintercogs.beyonddimensions.integration.module.botania.storage.ManaStackKey;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.Optional;

@BDIntegrationModule(modId = OtherModIds.APPBOT)
public class AppBotaniaModule implements IIntegrationModule
{
    @Override
    public String modId()
    {
        return OtherModIds.APPBOT;
    }

    @Override
    public void onBootstrap(IEventBus modBus, IEventBus gameBus)
    {
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event)
    {
        AEHelper.ISTACK_TO_AEKEY_MAP.put(ManaStackKey.ID, stackType -> Optional.of(ManaKey.KEY));
        AEHelper.AEKEY_TO_STACK_TYPE_MAP.put(ManaKeyType.TYPE, key -> Optional.of(ManaStackKey.INSTANCE));
    }
}
