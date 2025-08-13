package com.wintercogs.beyonddimensions.Item;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Block.ModBlocks;
import com.wintercogs.beyonddimensions.Fluid.ModFluids;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs
{
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BeyondDimensions.MODID);

    public static final Supplier<CreativeModeTab> BEYOND_DIMENSIONS_ITEMS_TAB = CREATIVE_MODE_TAB.register(
            "beyond_dimensions_items_tab",
            ()->CreativeModeTab.builder()
                    .icon(()->new ItemStack(ModItems.NET_CREATER.get()))
                    .title(Component.translatable("creativetab.beyonddimensions.items"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.NET_CREATER.get());
                        output.accept(ModItems.NET_MEMBER_INVITER.get());
                        output.accept(ModItems.NET_MANAGER_INVITER.get());
                        output.accept(ModItems.UNSTABLE_SPACE_TIME_FRAGMENT.get());
                        output.accept(ModItems.STABLE_SPACE_TIME_FRAGMENT.get());
                        output.accept(ModItems.SPACE_TIME_STABLE_FRAME.get());
                        output.accept(ModItems.SHATTERED_SPACE_TIME_CRYSTALLIZATION.get());
                        output.accept(ModItems.SPACE_TIME_BAR.get());
                        output.accept(ModItems.TEST_ITEM_GENERATE.get());
                        output.accept(ModItems.NET_TERMINAL_ITEM.get());
                        output.accept(ModItems.NET_AE_STORAGE_CELL.get());
                        output.accept(ModItems.NET_GIFTER.get());
                        output.accept(ModItems.NET_DESTROYER.get());
                        output.accept(ModItems.MATTER_COMPRESS_BALL.get());
                        output.accept(ModItems.NET_MAGNET_ITEM.get());
                        output.accept(ModItems.NET_FEEDER_ITEM.get());
                        output.accept(ModItems.XP_EXCHANGE_ITEM.get());

                        for (ModFluids.FluidEntry e : ModFluids.ALL) { //注册所有桶
                            output.accept((Item)e.bucket().get());
                        }
                    })
                    .build());

    public static final Supplier<CreativeModeTab> BEYOND_DIMENSIONS_BLOCKS_TAB = CREATIVE_MODE_TAB.register(
            "beyond_dimensions_blocks_tab",
            ()->CreativeModeTab.builder()
                    .icon(()->new ItemStack(ModBlocks.NET_CONTROL.get()))
                    .withTabsBefore(ResourceLocation.tryBuild(BeyondDimensions.MODID,"beyond_dimensions_items_tab"))
                    .title(Component.translatable("creativetab.beyonddimensions.blocks"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModBlocks.NET_CONTROL.get());
                        output.accept(ModBlocks.NET_INTERFACE.get());
                        output.accept(ModBlocks.NET_PATHWAY.get());
                        output.accept(ModBlocks.NET_ENERGY_PATHWAY.get());
                        output.accept(ModBlocks.NET_TERMINAL_BLOCK.get());
                        output.accept(ModBlocks.NET_PUMP_BLOCK.get());
                        output.accept(ModBlocks.NET_HOPPER_BLOCK.get());
                        output.accept(ModBlocks.NET_FURNACE_BLOCK.get());
                        output.accept(ModBlocks.DIMENSIONAL_CONNECT_BLOCK.get());

                        if(BeyondDimensions.ARS_Loaded)
                        {
                            output.accept(ModBlocks.ARS_SOURCE_PATHWAY.get());
                        }
                        if(BeyondDimensions.Botania_Loaded)
                        {
                            output.accept(ModBlocks.MANA_POOL_PATHWAY.get());
                        }
                    })
                    .build());



    public static void register(IEventBus eventBus)
    {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
