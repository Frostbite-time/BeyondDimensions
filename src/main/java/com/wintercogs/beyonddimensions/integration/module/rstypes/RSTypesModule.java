package com.wintercogs.beyonddimensions.integration.module.rstypes;

import com.ultramega.refinedtypes.type.energy.EnergyResource;
import com.ultramega.refinedtypes.type.energy.EnergyResourceType;
import com.ultramega.refinedtypes.type.soul.SoulResource;
import com.ultramega.refinedtypes.type.soul.SoulResourceType;
import com.ultramega.refinedtypes.type.source.SourceResource;
import com.ultramega.refinedtypes.type.source.SourceResourceType;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.SourceStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.WardenSoulStackKey;
import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.rs.RSHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.Optional;

@BDIntegrationModule(modId = OtherModIds.REFINED_TYPES)
public class RSTypesModule implements IIntegrationModule
{
    @Override
    public String modId()
    {
        return OtherModIds.REFINED_TYPES;
    }

    @Override
    public void onBootstrap(IEventBus modBus, IEventBus gameBus)
    {

    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event)
    {
        RSHelper.ISTACK_TO_RSKEY_MAP.put(EnergyStackKey.ID, stackType -> Optional.of(EnergyResource.createEnergyResource()));
        RSHelper.ISTACK_TO_RSKEY_MAP.put(SourceStackKey.ID, stackType -> Optional.of(SourceResource.createSourceResource()));
        RSHelper.ISTACK_TO_RSKEY_MAP.put(WardenSoulStackKey.ID, stackType -> Optional.of(SoulResource.createSoulResource()));

        RSHelper.RSKEY_TO_STACK_TYPE_MAP.put(EnergyResourceType.INSTANCE, key -> Optional.of(EnergyStackKey.INSTANCE));
        RSHelper.RSKEY_TO_STACK_TYPE_MAP.put(SourceResourceType.INSTANCE, key -> Optional.of(SourceStackKey.INSTANCE));
        RSHelper.RSKEY_TO_STACK_TYPE_MAP.put(SoulResourceType.INSTANCE, key -> Optional.of(WardenSoulStackKey.INSTANCE));
    }
}
