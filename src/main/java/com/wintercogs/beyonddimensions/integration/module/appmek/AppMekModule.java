package com.wintercogs.beyonddimensions.integration.module.appmek;

import com.wintercogs.beyonddimensions.api.storage.key.impl.GasStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.InfusionStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.PigmentStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.SlurryStackKey;
import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.ae2.AEHelper;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import me.ramidzkh.mekae2.ae2.MekanismKeyType;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.api.chemical.slurry.SlurryStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.Optional;

@BDIntegrationModule(modId = OtherModIds.APPMEK)
public class AppMekModule implements IIntegrationModule
{
    @Override
    public String modId()
    {
        return OtherModIds.APPMEK;
    }

    @Override
    public void onBootstrap(IEventBus modBus, IEventBus gameBus)
    {
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event)
    {
        AEHelper.ISTACK_TO_AEKEY_MAP.put(GasStackKey.ID, stackType -> Optional.ofNullable(MekanismKey.of((GasStack) stackType.copyStack())));
        AEHelper.ISTACK_TO_AEKEY_MAP.put(InfusionStackKey.ID, stackType -> Optional.ofNullable(MekanismKey.of((InfusionStack) stackType.copyStack())));
        AEHelper.ISTACK_TO_AEKEY_MAP.put(PigmentStackKey.ID, stackType -> Optional.ofNullable(MekanismKey.of((PigmentStack) stackType.copyStack())));
        AEHelper.ISTACK_TO_AEKEY_MAP.put(SlurryStackKey.ID, stackType -> Optional.ofNullable(MekanismKey.of((SlurryStack) stackType.copyStack())));

        AEHelper.AEKEY_TO_STACK_TYPE_MAP.put(
                MekanismKeyType.TYPE,
                (key) -> {
                    MekanismKey mekKey = (MekanismKey) key;
                    ChemicalStack<?> chemical = mekKey.withAmount(1);

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
