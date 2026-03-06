package com.wintercogs.beyonddimensions.integration;

import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.ArrayList;
import java.util.List;

public final class IntegrationManager
{
    private static final List<IIntegrationModule> ACTIVE_COMMON_MODULES = new ArrayList<>();
    private static final List<IIntegrationClientModule> ACTIVE_CLIENT_MODULES = new ArrayList<>();

    private static boolean commonBootstrapped = false;
    private static boolean clientBootstrapped = false;

    private IntegrationManager()
    {
    }

    public static void bootstrapCommon(IEventBus modBus, IEventBus gameBus)
    {
        if (commonBootstrapped)
        {
            return;
        }
        commonBootstrapped = true;

        ACTIVE_COMMON_MODULES.clear();
        for (ModuleSpec spec : ModuleRegistry.commonModules())
        {
            if (!ModPresence.isLoaded(spec.modId()))
            {
                continue;
            }

            IIntegrationModule module = OptionalClassLoader.instantiate(spec.implClassName(), IIntegrationModule.class);
            if (module == null)
            {
                continue;
            }

            ACTIVE_COMMON_MODULES.add(module);
            module.onBootstrap(modBus, gameBus);
        }

        modBus.addListener(IntegrationManager::onCommonSetup);
    }

    public static void bootstrapClient(IEventBus modBus, IEventBus gameBus)
    {
        if (clientBootstrapped)
        {
            return;
        }
        clientBootstrapped = true;

        ACTIVE_CLIENT_MODULES.clear();
        for (ModuleSpec spec : ModuleRegistry.clientModules())
        {
            if (!ModPresence.isLoaded(spec.modId()))
            {
                continue;
            }

            IIntegrationClientModule module = OptionalClassLoader.instantiate(spec.implClassName(), IIntegrationClientModule.class);
            if (module == null)
            {
                continue;
            }

            ACTIVE_CLIENT_MODULES.add(module);
            module.onBootstrapClient(modBus, gameBus);
        }

        modBus.addListener(IntegrationManager::onClientSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event)
    {
        for (IIntegrationModule module : ACTIVE_COMMON_MODULES)
        {
            module.onCommonSetup(event);
        }
    }

    private static void onClientSetup(FMLClientSetupEvent event)
    {
        for (IIntegrationClientModule module : ACTIVE_CLIENT_MODULES)
        {
            module.onClientSetup(event);
        }
    }

    public static void onItemCreativeTabCollect(CreativeModeTab.ItemDisplayParameters displayParameters, CreativeModeTab.Output output)
    {
        for (IIntegrationModule module : ACTIVE_COMMON_MODULES)
        {
            module.onItemCreativeTabCollect(displayParameters, output);
        }
    }

    public static void onBlockCreativeTabCollect(CreativeModeTab.ItemDisplayParameters displayParameters, CreativeModeTab.Output output)
    {
        for (IIntegrationModule module : ACTIVE_COMMON_MODULES)
        {
            module.onBlockCreativeTabCollect(displayParameters, output);
        }
    }

    public static void onDatagen(GatherDataEvent.Client event)
    {
        for (IIntegrationModule module : ACTIVE_COMMON_MODULES)
        {
            module.onDatagen(event);
        }
    }
}
