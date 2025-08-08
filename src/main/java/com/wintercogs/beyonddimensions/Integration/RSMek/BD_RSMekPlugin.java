package com.wintercogs.beyonddimensions.Integration.RSMek;

import com.refinedmods.refinedstorage.mekanism.ChemicalResource;
import com.refinedmods.refinedstorage.mekanism.ChemicalResourceType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ChemicalStackType;
import com.wintercogs.beyonddimensions.Integration.RS.RSHelper;
import mekanism.api.chemical.ChemicalStack;

import java.util.Optional;

public class BD_RSMekPlugin
{
    public static void register()
    {
        RSHelper.ISTACK_TO_RSKEY_MAP.put(ChemicalStackType.ID, stackType -> Optional.ofNullable(new ChemicalResource(((ChemicalStackType)stackType).getStack().getChemical())));

        RSHelper.RSKEY_TO_STACK_TYPE_MAP.put(ChemicalResourceType.INSTANCE, (key, amount) -> Optional.of(new ChemicalStackType(new ChemicalStack(((ChemicalResource)key).chemical(), amount))));
    }
}
