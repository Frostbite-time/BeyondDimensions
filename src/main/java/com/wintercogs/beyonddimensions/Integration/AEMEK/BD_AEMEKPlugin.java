package com.wintercogs.beyonddimensions.Integration.AEMEK;

import com.wintercogs.beyonddimensions.DataBase.Stack.Chemicals.GasStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.Chemicals.InfusionStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.Chemicals.PigmentStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.Chemicals.SlurryStackType;
import com.wintercogs.beyonddimensions.Integration.AE.AEHelper;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.chemical.ChemicalStack;

import java.util.Optional;

public class BD_AEMEKPlugin
{
    public static void register()
    {
        AEHelper.ISTACK_TO_AEKEY_MAP.put(GasStackType.ID, stackType -> Optional.of(MekanismKey.of((ChemicalStack<?>) stackType.copyStack())));
        AEHelper.ISTACK_TO_AEKEY_MAP.put(InfusionStackType.ID, stackType -> Optional.of(MekanismKey.of((ChemicalStack<?>) stackType.copyStack())));
        AEHelper.ISTACK_TO_AEKEY_MAP.put(PigmentStackType.ID, stackType -> Optional.of(MekanismKey.of((ChemicalStack<?>) stackType.copyStack())));
        AEHelper.ISTACK_TO_AEKEY_MAP.put(SlurryStackType.ID, stackType -> Optional.of(MekanismKey.of((ChemicalStack<?>) stackType.copyStack())));
    }
}
