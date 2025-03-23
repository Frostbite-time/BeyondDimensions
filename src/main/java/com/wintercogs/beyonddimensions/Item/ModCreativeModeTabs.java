package com.wintercogs.beyonddimensions.Item;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
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
                        output.accept(ModItems.NET_CREATER);
                        output.accept(ModItems.NET_MEMBER_INVITER);
                        output.accept(ModItems.NET_MANAGER_INVITER);
                        output.accept(ModItems.UNSTABLE_SPACE_TIME_FRAGMENT);
                        output.accept(ModItems.STABLE_SPACE_TIME_FRAGMENT);
                        output.accept(ModItems.SPACE_TIME_STABLE_FRAME);
                        output.accept(ModItems.SHATTERED_SPACE_TIME_CRYSTALLIZATION);
                        output.accept(ModItems.SPACE_TIME_BAR);
                    })
                    .build());

    public static final Supplier<CreativeModeTab> BEYOND_DIMENSIONS_BLOCKS_TAB = CREATIVE_MODE_TAB.register(
            "beyond_dimensions_blocks_tab",
            ()->CreativeModeTab.builder()
                    .icon(()->new ItemStack(ModBlocks.NET_CONTROL))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(BeyondDimensions.MODID,"beyond_dimensions_items_tab"))
                    .title(Component.translatable("creativetab.beyonddimensions.blocks"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModBlocks.NET_CONTROL);
                        output.accept(ModBlocks.NET_INTERFACE);
                        output.accept(ModBlocks.NET_PATHWAY);
                        output.accept(ModBlocks.NET_ENERGY_PATHWAY);
                    })
                    .build());



    public static void register(IEventBus eventBus)
    {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
