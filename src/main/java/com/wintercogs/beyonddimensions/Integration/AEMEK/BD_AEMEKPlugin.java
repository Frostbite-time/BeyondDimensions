package com.wintercogs.beyonddimensions.Integration.AEMEK;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ChemicalStackKey;
import com.wintercogs.beyonddimensions.Integration.AE.AEHelper;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import me.ramidzkh.mekae2.ae2.MekanismKeyType;
import mekanism.api.chemical.ChemicalStack;

import java.util.Optional;

public class BD_AEMEKPlugin
{
    public static void register()
    {
        AEHelper.ISTACK_TO_AEKEY_MAP.put(ChemicalStackKey.ID, stackType -> Optional.of(MekanismKey.of((ChemicalStack) stackType.copyStack())));

        AEHelper.AEKEY_TO_STACK_TYPE_MAP.put(MekanismKeyType.TYPE, key -> Optional.of(new ChemicalStackKey(((MekanismKey) key).withAmount(1))));
    }
}
