package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Fluid.ModFluids;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class BDCreativeModeTabs
{
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BeyondDimensions.MODID);

    public static final Supplier<CreativeModeTab> BEYOND_DIMENSIONS_ITEMS_TAB = CREATIVE_MODE_TAB.register(
            "beyond_dimensions_items_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(BDItems.NET_CREATER.get()))
                    .title(Component.translatable("creativetab.beyonddimensions.items"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(BDItems.NET_CREATER);
                        output.accept(BDItems.NET_MEMBER_INVITER);
                        output.accept(BDItems.NET_MANAGER_INVITER);
                        output.accept(BDItems.UNSTABLE_SPACE_TIME_FRAGMENT);
                        output.accept(BDItems.STABLE_SPACE_TIME_FRAGMENT);
                        output.accept(BDItems.SPACE_TIME_STABLE_FRAME);
                        output.accept(BDItems.SHATTERED_SPACE_TIME_CRYSTALLIZATION);
                        output.accept(BDItems.SPACE_TIME_BAR);
                        output.accept(BDItems.TEST_ITEM_GENERATE);
                        output.accept(BDItems.NET_TERMINAL_ITEM);
                        output.accept(BDItems.NET_GIFTER);
                        output.accept(BDItems.NET_DESTROYER);
                        output.accept(BDItems.MATTER_COMPRESS_BALL);
                        output.accept(BDItems.NET_MAGNET_ITEM);
                        output.accept(BDItems.NET_FEEDER_ITEM);
                        output.accept(BDItems.NET_RESTOCKER_ITEM);
                        output.accept(BDItems.XP_EXCHANGE_ITEM);

                        for (ModFluids.FluidEntry e : ModFluids.ALL)
                        { //注册所有桶
                            output.accept(e.bucket().get());
                        }
                    })
                    .build());

    public static final Supplier<CreativeModeTab> BEYOND_DIMENSIONS_BLOCKS_TAB = CREATIVE_MODE_TAB.register(
            "beyond_dimensions_blocks_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(BDBlocks.NET_CONTROL))
                    .withTabsBefore(Identifier.fromNamespaceAndPath(BeyondDimensions.MODID, "beyond_dimensions_items_tab"))
                    .title(Component.translatable("creativetab.beyonddimensions.blocks"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(BDBlocks.NET_CONTROL);
                        output.accept(BDBlocks.NET_INTERFACE);
                        output.accept(BDBlocks.NET_PATHWAY);
                        output.accept(BDBlocks.NET_ENERGY_PATHWAY);
                        output.accept(BDBlocks.NET_TERMINAL_BLOCK);
                        output.accept(BDBlocks.NET_PUMP_BLOCK);
                        output.accept(BDBlocks.NET_HOPPER_BLOCK);
                        output.accept(BDBlocks.NET_FURNACE_BLOCK);
                        output.accept(BDBlocks.DIMENSIONAL_CONNECT_BLOCK);
                    })
                    .build());


    public static void register(IEventBus eventBus)
    {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
