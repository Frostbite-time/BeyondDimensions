package com.wintercogs.beyonddimensions.integration.module.rs;

import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import com.refinedmods.refinedstorage.common.support.resource.ResourceTypes;
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.rs.datagen.RSModuleBlockLootTableProvider;
import com.wintercogs.beyonddimensions.integration.module.rs.datagen.RSModuleBlockStateProvider;
import com.wintercogs.beyonddimensions.integration.module.rs.datagen.RSModuleRecipeProvider;
import com.wintercogs.beyonddimensions.integration.module.rs.init.RSModuleBlockEntities;
import com.wintercogs.beyonddimensions.integration.module.rs.init.RSModuleBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@BDIntegrationModule(modId = OtherModIds.REFINED_STORAGE)
public class RSModule implements IIntegrationModule
{
    @Override
    public String modId()
    {
        return OtherModIds.REFINED_STORAGE;
    }

    @Override
    public void onBootstrap(IEventBus modBus, IEventBus gameBus)
    {
        RSModuleBlocks.register(modBus);
        RSModuleBlockEntities.register(modBus);
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event)
    {
        RSHelper.ISTACK_TO_RSKEY_MAP.put(ItemStackKey.ID, stackType -> Optional.of(ItemResource.ofItemStack((ItemStack) stackType.copyStack())));
        RSHelper.ISTACK_TO_RSKEY_MAP.put(FluidStackKey.ID, stackType -> {
            FluidStack stack = (FluidStack) stackType.copyStack();
            return Optional.of(new FluidResource(stack.getFluid(), stack.getComponentsPatch()));
        });

        RSHelper.RSKEY_TO_STACK_TYPE_MAP.put(ResourceTypes.ITEM, key -> Optional.of(new ItemStackKey(((ItemResource) key).toItemStack())));
        RSHelper.RSKEY_TO_STACK_TYPE_MAP.put(ResourceTypes.FLUID, key -> {
            FluidResource fluidKey = (FluidResource) key;
            FluidStack stack = new FluidStack(BuiltInRegistries.FLUID.wrapAsHolder(fluidKey.fluid()), 1, fluidKey.components());
            return Optional.of(new FluidStackKey(stack));
        });

        BDRSPlugin.register();
    }

    @Override
    public void onBlockCreativeTabCollect(CreativeModeTab.ItemDisplayParameters displayParameters, CreativeModeTab.Output output)
    {
        output.accept(RSModuleBlocks.RS_NET_PATHWAY);
    }

    @Override
    public void onBlockTagDatagen(HolderLookup.Provider provider, BlockTagAppender appender)
    {
        appender.add(BlockTags.MINEABLE_WITH_PICKAXE, RSModuleBlocks.RS_NET_PATHWAY.get());
    }

    @Override
    public void onDatagen(GatherDataEvent event)
    {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(event.includeServer(), new LootTableProvider(packOutput, Collections.emptySet(),
                List.of(new LootTableProvider.SubProviderEntry(RSModuleBlockLootTableProvider::new, LootContextParamSets.BLOCK)), lookupProvider)
        {
            @Override
            public @NotNull String getName()
            {
                return "BeyondDimensions RSModule LootTable Provider";
            }
        });
        generator.addProvider(event.includeClient(), new RSModuleBlockStateProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeServer(), new RSModuleRecipeProvider(packOutput, lookupProvider));
    }
}
