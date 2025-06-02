package com.wintercogs.beyonddimensions.Integration.AEMEK;

import com.wintercogs.beyonddimensions.DataBase.Stack.ChemicalStackType;
import com.wintercogs.beyonddimensions.Integration.AE.AEHelper;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.chemical.ChemicalStack;

import java.util.Optional;

public class BD_AEMEKPlugin
{
    public static void register()
    {
        AEHelper.ISTACK_TO_AEKEY_MAP.put(ChemicalStackType.ID, stackType -> Optional.of(MekanismKey.of((ChemicalStack) stackType.copyStack())));
    }
}
