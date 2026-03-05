package com.wintercogs.beyonddimensions.integration.RSTypes;

import com.ultramega.refinedtypes.type.energy.EnergyResource;
import com.ultramega.refinedtypes.type.energy.EnergyResourceType;
import com.ultramega.refinedtypes.type.soul.SoulResource;
import com.ultramega.refinedtypes.type.soul.SoulResourceType;
import com.ultramega.refinedtypes.type.source.SourceResource;
import com.ultramega.refinedtypes.type.source.SourceResourceType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EnergyStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.SourceStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.WardenSoulStackKey;
import com.wintercogs.beyonddimensions.integration.RS.RSHelper;

import java.util.Optional;

public class BD_RSTypesPlugin
{
    public static void register()
    {
        RSHelper.ISTACK_TO_RSKEY_MAP.put(EnergyStackKey.ID, stackType -> Optional.of(EnergyResource.createEnergyResource()));
        RSHelper.ISTACK_TO_RSKEY_MAP.put(SourceStackKey.ID, stackType -> Optional.of(SourceResource.createSourceResource()));
        RSHelper.ISTACK_TO_RSKEY_MAP.put(WardenSoulStackKey.ID, stackType -> Optional.of(SoulResource.createSoulResource()));

        RSHelper.RSKEY_TO_STACK_TYPE_MAP.put(EnergyResourceType.INSTANCE, key -> Optional.of(EnergyStackKey.INSTANCE));
        RSHelper.RSKEY_TO_STACK_TYPE_MAP.put(SourceResourceType.INSTANCE, key -> Optional.of(SourceStackKey.INSTANCE));
        RSHelper.RSKEY_TO_STACK_TYPE_MAP.put(SoulResourceType.INSTANCE, key -> Optional.of(WardenSoulStackKey.INSTANCE));
    }
}
