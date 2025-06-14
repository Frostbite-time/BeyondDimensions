package com.wintercogs.beyonddimensions.Integration.AEFlux;

import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;
import com.glodblock.github.appflux.common.me.key.type.FluxKeyType;
import com.wintercogs.beyonddimensions.DataBase.Stack.EnergyStackType;
import com.wintercogs.beyonddimensions.Integration.AE.AEHelper;

import java.util.Optional;

public class BD_AEFluxPlugin
{
    public static void register()
    {
        AEHelper.ISTACK_TO_AEKEY_MAP.put(EnergyStackType.ID, stackType -> Optional.of(FluxKey.of(EnergyType.FE)));

        AEHelper.AEKEY_TO_STACK_TYPE_MAP.put(FluxKeyType.TYPE, (key, amount) -> Optional.of(new EnergyStackType(amount)));
    }
}
