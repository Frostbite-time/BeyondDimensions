package com.wintercogs.beyonddimensions.Integration.AEMEK;

import com.wintercogs.beyonddimensions.DataBase.Stack.Chemicals.GasStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.Chemicals.InfusionStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.Chemicals.PigmentStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.Chemicals.SlurryStackType;
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
        AEHelper.ISTACK_TO_AEKEY_MAP.put(GasStackType.ID, stackType -> Optional.of(MekanismKey.of((ChemicalStack<?>) stackType.copyStack())));
        AEHelper.ISTACK_TO_AEKEY_MAP.put(InfusionStackType.ID, stackType -> Optional.of(MekanismKey.of((ChemicalStack<?>) stackType.copyStack())));
        AEHelper.ISTACK_TO_AEKEY_MAP.put(PigmentStackType.ID, stackType -> Optional.of(MekanismKey.of((ChemicalStack<?>) stackType.copyStack())));
        AEHelper.ISTACK_TO_AEKEY_MAP.put(SlurryStackType.ID, stackType -> Optional.of(MekanismKey.of((ChemicalStack<?>) stackType.copyStack())));

        AEHelper.AEKEY_TO_STACK_TYPE_MAP.put(
                MekanismKeyType.TYPE,
                (key, amount) -> {
                    MekanismKey mekKey = (MekanismKey) key;
                    ChemicalStack<?> chemical = mekKey.withAmount(amount);

                    return switch (mekKey.getForm())
                    {
                        case 0 -> Optional.of(new GasStackType((GasStack) chemical));
                        case 1 -> Optional.of(new InfusionStackType((InfusionStack) chemical));
                        case 2 -> Optional.of(new PigmentStackType((PigmentStack) chemical));
                        case 3 -> Optional.of(new SlurryStackType((SlurryStack) chemical));
                        default -> throw new UnsupportedOperationException(
                                "Unsupported chemical type: " + mekKey.getForm()
                        );
                    };
                }
        );

    }
}
