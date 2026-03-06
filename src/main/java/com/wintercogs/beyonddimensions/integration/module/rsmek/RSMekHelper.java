package com.wintercogs.beyonddimensions.integration.module.rsmek;

import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class RSMekHelper
{

    protected static ChemicalStack getStackFromChemical(Chemical chemical)
    {
        ResourceLocation rl = MekanismAPI.CHEMICAL_REGISTRY.getKey(chemical);
        ResourceKey<Chemical> rkey = ResourceKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, rl);
        return MekanismAPI.CHEMICAL_REGISTRY.getHolder(rkey)
                .map(chemicalReference -> new ChemicalStack(chemicalReference, 1))
                .orElse(new ChemicalStack(MekanismAPI.EMPTY_CHEMICAL_HOLDER, 1));
    }
}
