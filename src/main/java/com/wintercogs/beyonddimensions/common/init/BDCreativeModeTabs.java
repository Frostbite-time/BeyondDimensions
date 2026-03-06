package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.integration.IntegrationManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class BDCreativeModeTabs
{
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BDConstants.MODID);

    public static final Supplier<CreativeModeTab> BEYOND_DIMENSIONS_ITEMS_TAB = CREATIVE_MODE_TAB.register(
            "beyond_dimensions_items_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(BDItems.NET_CREATER.get()))
                    .title(Component.translatable("creativetab.beyonddimensions.items"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(BDItems.NET_CREATER.get());
                        output.accept(BDItems.NET_MEMBER_INVITER.get());
                        output.accept(BDItems.NET_MANAGER_INVITER.get());
                        output.accept(BDItems.UNSTABLE_SPACE_TIME_FRAGMENT.get());
                        output.accept(BDItems.STABLE_SPACE_TIME_FRAGMENT.get());
                        output.accept(BDItems.SPACE_TIME_STABLE_FRAME.get());
                        output.accept(BDItems.SHATTERED_SPACE_TIME_CRYSTALLIZATION.get());
                        output.accept(BDItems.SPACE_TIME_BAR.get());
                        output.accept(BDItems.TEST_ITEM_GENERATE.get());
                        output.accept(BDItems.NET_TERMINAL_ITEM.get());
                        output.accept(BDItems.NET_GIFTER.get());
                        output.accept(BDItems.NET_DESTROYER.get());
                        output.accept(BDItems.MATTER_COMPRESS_BALL.get());
                        output.accept(BDItems.NET_MAGNET_ITEM.get());
                        output.accept(BDItems.NET_FEEDER_ITEM.get());
                        output.accept(BDItems.NET_RESTOCKER_ITEM.get());
                        output.accept(BDItems.XP_EXCHANGE_ITEM.get());

                        for (BDFluids.FluidEntry e : BDFluids.ALL)
                        { //注册所有桶
                            output.accept((Item) e.bucket().get());
                        }

                        IntegrationManager.onItemCreativeTabCollect(itemDisplayParameters, output);
                    })
                    .build());

    public static final Supplier<CreativeModeTab> BEYOND_DIMENSIONS_BLOCKS_TAB = CREATIVE_MODE_TAB.register(
            "beyond_dimensions_blocks_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(BDBlocks.NET_CONTROL.get()))
                    .withTabsBefore(ResourceLocation.tryBuild(BDConstants.MODID, "beyond_dimensions_items_tab"))
                    .title(Component.translatable("creativetab.beyonddimensions.blocks"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(BDBlocks.NET_CONTROL.get());
                        output.accept(BDBlocks.NET_INTERFACE.get());
                        output.accept(BDBlocks.NET_PATHWAY.get());
                        output.accept(BDBlocks.NET_ENERGY_PATHWAY.get());
                        output.accept(BDBlocks.NET_TERMINAL_BLOCK.get());
                        output.accept(BDBlocks.NET_PUMP_BLOCK.get());
                        output.accept(BDBlocks.NET_HOPPER_BLOCK.get());
                        output.accept(BDBlocks.NET_FURNACE_BLOCK.get());
                        output.accept(BDBlocks.DIMENSIONAL_CONNECT_BLOCK.get());

                        IntegrationManager.onBlockCreativeTabCollect(itemDisplayParameters, output);
                    })
                    .build());


    public static void register(IEventBus eventBus)
    {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
