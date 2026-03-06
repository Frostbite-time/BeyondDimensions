package com.wintercogs.beyonddimensions.integration.module.rsmek;

import com.refinedmods.refinedstorage.mekanism.ChemicalResource;
import com.refinedmods.refinedstorage.mekanism.ChemicalResourceType;
import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.mekanism.storage.ChemicalStackKey;
import com.wintercogs.beyonddimensions.integration.module.rs.RSHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.Optional;

@BDIntegrationModule(modId = OtherModIds.RS_MEK_INTEGRATION)
public class RSMekModule implements IIntegrationModule
{
    @Override
    public String modId()
    {
        return OtherModIds.RS_MEK_INTEGRATION;
    }

    @Override
    public void onBootstrap(IEventBus modBus, IEventBus gameBus)
    {

    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event)
    {
        RSHelper.ISTACK_TO_RSKEY_MAP.put(ChemicalStackKey.ID, stackType -> Optional.of(new ChemicalResource(((ChemicalStackKey) stackType).getSource())));

        RSHelper.RSKEY_TO_STACK_TYPE_MAP.put(ChemicalResourceType.INSTANCE, key -> Optional.of(new ChemicalStackKey(RSMekHelper.getStackFromChemical(((ChemicalResource) key).chemical()))));
    }
}
