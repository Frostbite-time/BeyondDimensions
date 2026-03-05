package com.wintercogs.beyonddimensions.integration.AEFlux;

import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;
import com.glodblock.github.appflux.common.me.key.type.FluxKeyType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EnergyStackKey;
import com.wintercogs.beyonddimensions.integration.ae2.AEHelper;

import java.util.Optional;

public class BD_AEFluxPlugin
{
    public static void register()
    {
        AEHelper.ISTACK_TO_AEKEY_MAP.put(EnergyStackKey.ID, stackType -> Optional.of(FluxKey.of(EnergyType.FE)));

        AEHelper.AEKEY_TO_STACK_TYPE_MAP.put(FluxKeyType.TYPE, key -> Optional.of(EnergyStackKey.INSTANCE));
    }
}
