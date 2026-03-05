package com.wintercogs.beyonddimensions.integration.RSMek;

import com.refinedmods.refinedstorage.mekanism.ChemicalResource;
import com.refinedmods.refinedstorage.mekanism.ChemicalResourceType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ChemicalStackKey;
import com.wintercogs.beyonddimensions.integration.RS.RSHelper;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public class BD_RSMekPlugin
{
    public static void register()
    {
        RSHelper.ISTACK_TO_RSKEY_MAP.put(ChemicalStackKey.ID, stackType -> Optional.of(new ChemicalResource(((ChemicalStackKey) stackType).getSource())));

        RSHelper.RSKEY_TO_STACK_TYPE_MAP.put(ChemicalResourceType.INSTANCE, key -> Optional.of(new ChemicalStackKey(getStackFromChemical(((ChemicalResource) key).chemical()))));
    }

    private static ChemicalStack getStackFromChemical(Chemical chemical)
    {
        ResourceLocation rl = MekanismAPI.CHEMICAL_REGISTRY.getKey(chemical);
        ResourceKey<Chemical> rkey = ResourceKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, rl);
        return MekanismAPI.CHEMICAL_REGISTRY.getHolder(rkey)
                .map(chemicalReference -> new ChemicalStack(chemicalReference, 1))
                .orElse(new ChemicalStack(MekanismAPI.EMPTY_CHEMICAL_HOLDER, 1));
    }
}
