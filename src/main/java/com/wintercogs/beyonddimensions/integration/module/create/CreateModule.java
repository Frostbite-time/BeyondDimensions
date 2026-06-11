package com.wintercogs.beyonddimensions.integration.module.create;

import com.simibubi.create.AllTags;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.wintercogs.beyonddimensions.common.init.BDBlocks;
import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.create.contraption.NetInterfaceMountedStorageType;
import com.wintercogs.beyonddimensions.integration.module.create.contraption.NetInterfaceMovementBehaviour;
import com.wintercogs.beyonddimensions.integration.module.create.datagen.CreateModuleBlockLootTableProvider;
import com.wintercogs.beyonddimensions.integration.module.create.datagen.CreateModuleBlockStateProvider;
import com.wintercogs.beyonddimensions.integration.module.create.datagen.CreateModuleRecipeProvider;
import com.wintercogs.beyonddimensions.integration.module.create.init.CreateModuleBlockEntities;
import com.wintercogs.beyonddimensions.integration.module.create.init.CreateModuleBlocks;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.Collections;
import java.util.List;

@BDIntegrationModule(modId = OtherModIds.CREATE)
public class CreateModule implements IIntegrationModule
{
    @Override
    public String modId()
    {
        return OtherModIds.CREATE;
    }

    @Override
    public void onBootstrap(IEventBus modBus, IEventBus gameBus)
    {
        CreateModuleBlocks.register(modBus);
        CreateModuleBlockEntities.register(modBus);
        NetInterfaceMountedStorageType.register(modBus);
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            MountedItemStorageType.REGISTRY.register(
                    BDBlocks.NET_INTERFACE.get(),
                    NetInterfaceMountedStorageType.NET_INTERFACE.get()
            );
            MovementBehaviour.REGISTRY.register(
                    BDBlocks.NET_INTERFACE.get(),
                    new NetInterfaceMovementBehaviour()
            );
        });
    }

    @Override
    public void onBlockCreativeTabCollect(CreativeModeTab.ItemDisplayParameters displayParameters, CreativeModeTab.Output output)
    {
        output.accept(CreateModuleBlocks.SCHEMATICANNON_PATHWAY.get());
    }

    @Override
    public void onBlockTagDatagen(net.minecraft.core.HolderLookup.Provider provider, BlockTagAppender appender)
    {
        appender.addOptional(BlockTags.MINEABLE_WITH_PICKAXE, CreateModuleBlocks.SCHEMATICANNON_PATHWAY.getId());
    }

    @Override
    public void onDatagen(GatherDataEvent event)
    {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        generator.addProvider(event.includeServer(), new LootTableProvider(packOutput, Collections.emptySet(),
                List.of(new LootTableProvider.SubProviderEntry(CreateModuleBlockLootTableProvider::new, LootContextParamSets.BLOCK)))
        {
            @Override
            public String getName()
            {
                return "BeyondDimensions CreateModule LootTable Provider";
            }
        });
        generator.addProvider(event.includeClient(), new CreateModuleBlockStateProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeServer(), new CreateModuleRecipeProvider(packOutput));
    }
}
