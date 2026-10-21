package com.wintercogs.beyonddimensions;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.api.capability.helper.CapabilityHelper;
import com.wintercogs.beyonddimensions.api.capability.helper.ordered.EnergyStackTypedHandler;
import com.wintercogs.beyonddimensions.api.capability.helper.ordered.FluidStackTypedHandler;
import com.wintercogs.beyonddimensions.api.capability.helper.ordered.ItemStackTypedHandler;
import com.wintercogs.beyonddimensions.api.capability.helper.unordered.EnergyUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.api.capability.helper.unordered.FluidUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.api.capability.helper.unordered.ItemUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.EnergyHandlerWrapper;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.FluidHandlerWrapper;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.ItemHandlerWrapper;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.registry.BDRegistries;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.AbstractUnorderedStackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.StackKeyRegistry;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EmptyStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.init.*;
import com.wintercogs.beyonddimensions.integration.IntegrationManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(BDConstants.MODID)
public class BeyondDimensions
{
    public static IEventBus MOD_EVENT_BUS;

    public static final Logger LOGGER = LogUtils.getLogger();

//    防止某些神经整合包禁用消息输出，预留一下位子，平常不启用
//    static {
//        try {
//            org.apache.logging.log4j.core.config.Configurator.setLevel("com.wintercogs", org.apache.logging.log4j.Level.INFO);
//        } catch (Throwable ignored) {}
//    }


    public BeyondDimensions()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MOD_EVENT_BUS = modEventBus;
        MinecraftForge.EVENT_BUS.register(this);
        Config.register(ModLoadingContext.get(), modEventBus);

        modEventBus.addListener(this::commonSetup);
        BDRegistries.register(modEventBus);
        // 所有已载入网络共用一个刻处理；标签重载后各存储的标签索引按需重建
        MinecraftForge.EVENT_BUS.addListener(DimensionsNet::tickLoaded);
        MinecraftForge.EVENT_BUS.addListener(DimensionsNet::forgetLoaded);
        MinecraftForge.EVENT_BUS.addListener((TagsUpdatedEvent event) -> AbstractUnorderedStackHandler.invalidateTagIndexes());

        BDMenus.register(modEventBus);
        BDCreativeModeTabs.register(modEventBus);
        BDItems.register(modEventBus);
        BDBlocks.register(modEventBus);
        BDFluids.register(modEventBus);
        BDBlockEntities.register(modEventBus);
        BDAttachments.register(modEventBus);
        BDMachineSettings.register(modEventBus);
        BDMachineTypes.register(modEventBus);
        BDNetDataTypes.register(modEventBus);
        BDNetTasks.register();
        BDStorageRules.register();

        // 分发集成模块
        IntegrationManager.bootstrapCommon(modEventBus, MinecraftForge.EVENT_BUS);
        // 1.20.1 没有只在客户端加载的模组入口，客户端的初始化从这里进入
        if (FMLEnvironment.dist == Dist.CLIENT)
            BeyondDimensionsClient.clientInit(modEventBus, MinecraftForge.EVENT_BUS);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {

        // 注册堆叠类型，使得网络能够存储相关堆叠
        StackKeyRegistry.registerType(EmptyStackKey.INSTANCE); // 全空堆叠，用于避免使用null
        StackKeyRegistry.registerType(ItemStackKey.EMPTY);
        StackKeyRegistry.registerType(FluidStackKey.EMPTY);
        StackKeyRegistry.registerType(EnergyStackKey.INSTANCE);

        // 注册方块能力类型，用于动态为方块注册能力
        CapabilityHelper.BlockCapabilityMap.put(ItemStackKey.ID, ForgeCapabilities.ITEM_HANDLER);
        CapabilityHelper.BlockCapabilityMap.put(FluidStackKey.ID, ForgeCapabilities.FLUID_HANDLER);
        CapabilityHelper.BlockCapabilityMap.put(EnergyStackKey.ID, ForgeCapabilities.ENERGY);
        // 注册物品能力，用于动态操作
        CapabilityHelper.ItemCapabilityMap.put(ItemStackKey.ID, ForgeCapabilities.ITEM_HANDLER);
        CapabilityHelper.ItemCapabilityMap.put(FluidStackKey.ID, ForgeCapabilities.FLUID_HANDLER_ITEM);
        CapabilityHelper.ItemCapabilityMap.put(EnergyStackKey.ID, ForgeCapabilities.ENERGY);

        // 注册网络能力，使得网络通道能暴露对应存储能力
        CapabilityHelper.registerUSHandler(ItemStackKey.EMPTY, ItemUnifiedStorageHandler::new);
        CapabilityHelper.registerUSHandler(FluidStackKey.EMPTY, FluidUnifiedStorageHandler::new);
        CapabilityHelper.registerUSHandler(EnergyStackKey.INSTANCE, EnergyUnifiedStorageHandler::new);

        // 注册存储分化包装
        CapabilityHelper.registerStackTypedHandler(ItemStackKey.EMPTY, ItemStackTypedHandler::new);
        CapabilityHelper.registerStackTypedHandler(FluidStackKey.EMPTY, FluidStackTypedHandler::new);
        CapabilityHelper.registerStackTypedHandler(EnergyStackKey.INSTANCE, EnergyStackTypedHandler::new);

        // 注册堆叠处理包装，用于动态包装来自其他模组的handler (如原版的IItemHandler)
        StackHandlerWrapperHelper.stackWrappers.put(ItemStackKey.ID, ItemHandlerWrapper::new);
        StackHandlerWrapperHelper.stackWrappers.put(FluidStackKey.ID, FluidHandlerWrapper::new);
        StackHandlerWrapperHelper.stackWrappers.put(EnergyStackKey.ID, EnergyHandlerWrapper::new);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("Beyond Dimensions initialized (server)");
    }

    public static ResourceLocation makeId(String path)
    {
        return new ResourceLocation(BDConstants.MODID, path);
    }
}
