package com.wintercogs.beyonddimensions.Integration.AEMEK;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.GasStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.InfusionStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.PigmentStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.SlurryStackKey;
import com.wintercogs.beyonddimensions.Integration.AE.AEHelper;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import me.ramidzkh.mekae2.ae2.MekanismKeyType;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.api.chemical.slurry.SlurryStack;

import java.util.Optional;

public class BD_AEMEKPlugin
{
    public static void register()
    {
        AEHelper.ISTACK_TO_AEKEY_MAP.put(GasStackKey.ID, stackType -> Optional.of(MekanismKey.of((ChemicalStack<?>) stackType.copyStack())));
        AEHelper.ISTACK_TO_AEKEY_MAP.put(InfusionStackKey.ID, stackType -> Optional.of(MekanismKey.of((ChemicalStack<?>) stackType.copyStack())));
        AEHelper.ISTACK_TO_AEKEY_MAP.put(PigmentStackKey.ID, stackType -> Optional.of(MekanismKey.of((ChemicalStack<?>) stackType.copyStack())));
        AEHelper.ISTACK_TO_AEKEY_MAP.put(SlurryStackKey.ID, stackType -> Optional.of(MekanismKey.of((ChemicalStack<?>) stackType.copyStack())));

        AEHelper.AEKEY_TO_STACK_TYPE_MAP.put(
                MekanismKeyType.TYPE,
                (key, amount) -> {
                    MekanismKey mekKey = (MekanismKey) key;
                    ChemicalStack<?> chemical = mekKey.withAmount(amount);

                    return switch (mekKey.getForm())
                    {
                        case 0 -> Optional.of(new GasStackKey((GasStack) chemical));
                        case 1 -> Optional.of(new InfusionStackKey((InfusionStack) chemical));
                        case 2 -> Optional.of(new PigmentStackKey((PigmentStack) chemical));
                        case 3 -> Optional.of(new SlurryStackKey((SlurryStack) chemical));
                        default -> throw new UnsupportedOperationException(
                                "Unsupported chemical type: " + mekKey.getForm()
                        );
                    };
                }
        );

    }
}
