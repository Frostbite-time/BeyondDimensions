package com.wintercogs.beyonddimensions.Integration.RSTypes;

import com.ultramega.refinedtypes.type.energy.EnergyResource;
import com.ultramega.refinedtypes.type.energy.EnergyResourceType;
import com.ultramega.refinedtypes.type.soul.SoulResource;
import com.ultramega.refinedtypes.type.soul.SoulResourceType;
import com.ultramega.refinedtypes.type.source.SourceResource;
import com.ultramega.refinedtypes.type.source.SourceResourceType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EnergyStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.SourceStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.WardenSoulStackType;
import com.wintercogs.beyonddimensions.Integration.RS.RSHelper;

import java.util.Optional;

public class BD_RSTypesPlugin
{
    public static void register()
    {
        RSHelper.ISTACK_TO_RSKEY_MAP.put(EnergyStackType.ID, stackType -> Optional.of(EnergyResource.createEnergyResource()));
        RSHelper.ISTACK_TO_RSKEY_MAP.put(SourceStackType.ID, stackType -> Optional.of(SourceResource.createSourceResource()));
        RSHelper.ISTACK_TO_RSKEY_MAP.put(WardenSoulStackType.ID, stackType -> Optional.of(SoulResource.createSoulResource()));

        RSHelper.RSKEY_TO_STACK_TYPE_MAP.put(EnergyResourceType.INSTANCE, (key, amount) -> Optional.of(new EnergyStackType(amount)));
        RSHelper.RSKEY_TO_STACK_TYPE_MAP.put(SourceResourceType.INSTANCE, (key, amount) -> Optional.of(new SourceStackType(amount)));
        RSHelper.RSKEY_TO_STACK_TYPE_MAP.put(SoulResourceType.INSTANCE, (key, amount) -> Optional.of(new WardenSoulStackType(amount)));
    }
}
