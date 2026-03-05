package com.wintercogs.beyonddimensions.integration;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.integration.curios.BD_CuriosPlugin;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;

public class IntegrationMods
{
    public static final String MekanismMODID = "mekanism";
    public static final String AE2MODID = "ae2";
    public static final String EMI_MODID = "emi";
    public static final String JEI2MODID = "jei";
    public static final String PolymorphModId = "polymorph";
    public static final String AEMEK2MODID = "appmek";
    public static final String AEFlux2MODID = "appflux";
    public static final String CuriosModId = "curios";
    public static final String JECharactersModId = "jecharacters";
    public static final String RSModId = "refinedstorage";
    public static final String RS_MEK_MODID = "refinedstorage_mekanism_integration";
    public static final String IFS_ModId = "industrialforegoingsouls";
    public static final String AE_IFS_ModId = "soulplied_energistics";
    public static final String ARS_ModId = "ars_nouveau";
    public static final String AE_ARS_ModId = "arseng";
    public static final String Botania_ModId = "botania";
    public static final String AE_Botania_ModId = "appbot";
    public static final String RSTypesModId = "refinedtypes";
    public static final String Create_ModId = "create";

    public static boolean MekLoaded = false;
    public static boolean AELoaded = false;
    public static boolean EMILoaded = false;
    public static boolean JEILoaded = false;
    public static boolean PolymorphLoaded = false;
    public static boolean AEMEKLoaded = false;
    public static boolean AEFluxLoaded = false;
    public static boolean CuriosLoaded = false;
    public static boolean JECharactersLoaded = false;
    public static boolean RS_Loaded = false;
    public static boolean RS_MEK_Loaded = false;
    public static boolean IFS_Loaded = false;
    public static boolean AE_IFS_Loaded = false;
    public static boolean ARS_Loaded = false;
    public static boolean AE_ARS_Loaded = false;
    public static boolean Botania_Loaded = false;
    public static boolean AE_Botania_Loaded = false;
    public static boolean RSTypesLoaded = false;
    public static boolean Create_Loaded = false;

    // 在此阶段检测模组列表
    public static void constructMod(final FMLConstructModEvent event)
    {
        if (ModList.get().isLoaded(MekanismMODID))
        {
            MekLoaded = true;
        }
        if (ModList.get().isLoaded(AE2MODID))
        {
            AELoaded = true;
        }
        if (ModList.get().isLoaded(EMI_MODID))
        {
            EMILoaded = true;
        }
        if (ModList.get().isLoaded(JEI2MODID))
        {
            JEILoaded = true;
        }
        if (ModList.get().isLoaded(PolymorphModId))
        {
            PolymorphLoaded = true;
        }
        if (ModList.get().isLoaded(AEMEK2MODID))
        {
            AEMEKLoaded = true;
        }
        if (ModList.get().isLoaded(AEFlux2MODID))
        {
            AEFluxLoaded = true;
        }
        if (ModList.get().isLoaded(CuriosModId))
        {
            CuriosLoaded = true;
            BeyondDimensions.MOD_EVENT_BUS.addListener(BD_CuriosPlugin::registerCapabilities);
        }
        if (ModList.get().isLoaded(JECharactersModId))
        {
            JECharactersLoaded = true;
        }
        if (ModList.get().isLoaded(RSModId))
        {
            RS_Loaded = true;
        }
        if (ModList.get().isLoaded(RS_MEK_MODID))
        {
            RS_MEK_Loaded = true;
        }
        if (ModList.get().isLoaded(IFS_ModId))
        {
            IFS_Loaded = true;
        }
        if (ModList.get().isLoaded(AE_IFS_ModId))
        {
            AE_IFS_Loaded = true;
        }
        if (ModList.get().isLoaded(ARS_ModId))
        {
            ARS_Loaded = true;
        }
        if (ModList.get().isLoaded(AE_ARS_ModId))
        {
            AE_ARS_Loaded = true;
        }
        if (ModList.get().isLoaded(Botania_ModId))
        {
            Botania_Loaded = true;
        }
        if (ModList.get().isLoaded(AE_Botania_ModId))
        {
            AE_Botania_Loaded = true;
        }
        if (ModList.get().isLoaded(RSTypesModId))
        {
            RSTypesLoaded = true;
        }
        if (ModList.get().isLoaded(Create_ModId))
        {
            Create_Loaded = true;
        }
    }
}
