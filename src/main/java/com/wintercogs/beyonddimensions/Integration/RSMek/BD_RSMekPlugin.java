package com.wintercogs.beyonddimensions.Integration.RSMek;

import com.refinedmods.refinedstorage.mekanism.ChemicalResource;
import com.refinedmods.refinedstorage.mekanism.ChemicalResourceType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ChemicalStackKey;
import com.wintercogs.beyonddimensions.Integration.RS.RSHelper;
import mekanism.api.chemical.ChemicalStack;

import java.util.Optional;

public class BD_RSMekPlugin
{
    public static void register()
    {
        RSHelper.ISTACK_TO_RSKEY_MAP.put(ChemicalStackKey.ID, stackType -> Optional.of(new ChemicalResource(((ChemicalStackKey)stackType).getSource())));

        RSHelper.RSKEY_TO_STACK_TYPE_MAP.put(ChemicalResourceType.INSTANCE, key -> Optional.of(new ChemicalStackKey(new ChemicalStack(((ChemicalResource)key).chemical(),1))));
    }
}
