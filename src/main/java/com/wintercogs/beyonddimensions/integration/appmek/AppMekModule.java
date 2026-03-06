package com.wintercogs.beyonddimensions.integration.appmek;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ChemicalStackKey;
import com.wintercogs.beyonddimensions.integration.AE.AEHelper;
import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import me.ramidzkh.mekae2.ae2.MekanismKeyType;
import mekanism.api.chemical.ChemicalStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.Optional;

@BDIntegrationModule(modId = OtherModIds.APPMEK)
public class AppMekModule implements IIntegrationModule
{
    @Override
    public String modId()
    {
        return OtherModIds.APPMEK;
    }

    @Override
    public void onBootstrap(IEventBus modBus, IEventBus gameBus)
    {

    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event)
    {
        AEHelper.ISTACK_TO_AEKEY_MAP.put(ChemicalStackKey.ID, stackType -> Optional.of(MekanismKey.of((ChemicalStack) stackType.copyStack())));
        AEHelper.AEKEY_TO_STACK_TYPE_MAP.put(MekanismKeyType.TYPE, key -> Optional.of(new ChemicalStackKey(((MekanismKey) key).withAmount(1))));
    }
}
