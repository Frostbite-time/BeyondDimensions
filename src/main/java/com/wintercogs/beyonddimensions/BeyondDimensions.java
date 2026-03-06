package com.wintercogs.beyonddimensions;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.api.capability.helper.CapabilityHelper;
import com.wintercogs.beyonddimensions.api.capability.helper.ordered.EnergyStackTypedHandler;
import com.wintercogs.beyonddimensions.api.capability.helper.ordered.FluidStackTypedHandler;
import com.wintercogs.beyonddimensions.api.capability.helper.ordered.ItemStackTypedHandler;
import com.wintercogs.beyonddimensions.api.capability.helper.ordered.ManaStackTypedHandler;
import com.wintercogs.beyonddimensions.api.capability.helper.unordered.EnergyUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.api.capability.helper.unordered.FluidUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.api.capability.helper.unordered.ItemUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.api.capability.helper.unordered.ManaUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.*;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.key.StackKeyRegistry;
import com.wintercogs.beyonddimensions.api.storage.key.impl.*;
import com.wintercogs.beyonddimensions.client.init.BDBlockRenders;
import com.wintercogs.beyonddimensions.common.init.*;
import com.wintercogs.beyonddimensions.integration.IntegrationManager;
import com.wintercogs.beyonddimensions.integration.ModPresence;
import com.wintercogs.beyonddimensions.integration.module.botania.BD_BotaniaPlugin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;


@Mod(BDConstants.MODID)
public class BeyondDimensions
{
    public static IEventBus MOD_EVENT_BUS;

    public static final String Botania_ModId = "botania"; // 植物魔法-mana兼容
    public static boolean Botania_Loaded = false;
    public static final String Create_ModId = "create";
    public static boolean Create_Loaded = false;
    public static final Logger LOGGER = LogUtils.getLogger();

//    防止某些神经整合包禁用消息输出，预留一下位子，平常不启用
//    static {
//        try {
//            org.apache.logging.log4j.core.config.Configurator.setLevel("com.wintercogs", org.apache.logging.log4j.Level.INFO);
//        } catch (Throwable ignored) {}
//    }

    // mod 类的构造函数是加载 mod 时运行的第一个代码。
    // FML 将识别一些参数类型，如 IEventBus 或 ModContainer 并自动传入它们。
    public BeyondDimensions()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MOD_EVENT_BUS = modEventBus;
        MinecraftForge.EVENT_BUS.register(this);//注册this类中所有事件
        Config.register(ModLoadingContext.get(), modEventBus);


        modEventBus.addListener(this::constructMod);
        modEventBus.addListener(this::commonSetup);
        //为存储网络的接口方块注册物品交互能力


        // 调用UIRegister的构造函数，从而注册所有UI
        BDMenus.register(modEventBus);

        // 注册创造模式菜单
        BDCreativeModeTabs.register(modEventBus);

        // 注册物品
        BDItems.register(modEventBus);

        // 注册方块
        BDBlocks.register(modEventBus);

        // 注册流体
        BDFluids.register(modEventBus);

        // 注册方块实体
        BDBlockEntities.register(modEventBus);

        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            BeyondDimensionsClient.clientInit(modEventBus, MinecraftForge.EVENT_BUS);
        }

        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            // 注册方块实体渲染
            modEventBus.addListener(BDBlockRenders::onRegisterRenderers);
        }

        IntegrationManager.bootstrapCommon(modEventBus, MinecraftForge.EVENT_BUS);
    }

    private void constructMod(final FMLConstructModEvent event)
    {
        if (ModPresence.isLoaded(Botania_ModId))
        {
            Botania_Loaded = true;
            MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, BD_BotaniaPlugin::attachBlockEntityCaps); // 为网络通道和网络接口手动注册火花附着
        }
        if (ModPresence.isLoaded(Create_ModId))
        {
            Create_Loaded = true;
        }

        BDBlockEntities.IntegrationRegister(); // 模组列表检查完成后，动态注册方块实体
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

        // 注册物品能力类型
        CapabilityHelper.ItemCapabilityMap.put(ItemStackKey.ID, ForgeCapabilities.ITEM_HANDLER);
        CapabilityHelper.ItemCapabilityMap.put(FluidStackKey.ID, ForgeCapabilities.FLUID_HANDLER_ITEM);
        CapabilityHelper.ItemCapabilityMap.put(EnergyStackKey.ID, ForgeCapabilities.ENERGY);

        // 注册网络能力，使得网络通道能暴露对应存储能力 注:能量存储无需注册，单独实现
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

        if (Botania_Loaded)
        {
            // 注册Mana（魔力）
            StackKeyRegistry.registerType(ManaStackKey.INSTANCE);
            CapabilityHelper.BlockCapabilityMap.put(ManaStackKey.ID, vazkii.botania.api.BotaniaForgeCapabilities.MANA_RECEIVER);
            CapabilityHelper.ItemCapabilityMap.put(ManaStackKey.ID, vazkii.botania.api.BotaniaForgeCapabilities.MANA_ITEM);
            CapabilityHelper.registerUSHandler(ManaStackKey.INSTANCE, ManaUnifiedStorageHandler::new);
            CapabilityHelper.registerStackTypedHandler(ManaStackKey.INSTANCE, ManaStackTypedHandler::new);
            StackHandlerWrapperHelper.stackWrappers.put(ManaStackKey.ID, ManaHandlerWrapper::new);

        }

        // 注册物品能力交互黑名单
        if (Botania_Loaded)
        {
            BD_BotaniaPlugin.registerItemCapBlackList();
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("维度网络初始化完成(服务端)");
    }

    public static ResourceLocation makeId(String path)
    {
        return new ResourceLocation(BDConstants.MODID, path);
    }

}
