package com.wintercogs.beyonddimensions.integration.ae2;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.storage.StorageCells;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.FluidStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Item.ModCreativeModeTabs;
import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.ae2.datagen.AE2ModuleDatagen;
import com.wintercogs.beyonddimensions.integration.ae2.init.AE2ModuleItems;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Optional;

@BDIntegrationModule(modId = OtherModIds.AE2)
public class AE2Module implements IIntegrationModule
{
    @Override
    public String modId()
    {
        return OtherModIds.AE2;
    }

    @Override
    public void onBootstrap(IEventBus modBus, IEventBus gameBus)
    {
        AE2ModuleItems.register(modBus);
        AE2ModuleDatagen.register();
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event)
    {
        StorageCells.addCellHandler(CellHandler.INSTANCE);

        AEHelper.ISTACK_TO_AEKEY_MAP.put(ItemStackKey.ID, stackType -> Optional.ofNullable(AEItemKey.of((ItemStack) stackType.copyStack())));
        AEHelper.ISTACK_TO_AEKEY_MAP.put(FluidStackKey.ID, stackType -> Optional.ofNullable(AEFluidKey.of((FluidStack) stackType.copyStack())));
        AEHelper.AEKEY_TO_STACK_TYPE_MAP.put(AEKeyType.items(), key -> Optional.of(new ItemStackKey(((AEItemKey) key).toStack(1))));
        AEHelper.AEKEY_TO_STACK_TYPE_MAP.put(AEKeyType.fluids(), key -> Optional.of(new FluidStackKey(((AEFluidKey) key).toStack(1))));
    }

    @Override
    public void onItemCreativeTabCollect(CreativeModeTab.ItemDisplayParameters displayParameters, CreativeModeTab.Output output)
    {
        output.accept(AE2ModuleItems.NET_AE_STORAGE_CELL);
    }
}
