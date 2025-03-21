package com.wintercogs.beyonddimensions;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.Block.ModBlocks;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetEnergyPathwayBlockEntity;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetInterfaceBlockEntity;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetPathwayBlockEntity;
import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.DataBase.Handler.ChemicalStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Handler.FluidStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Handler.ItemStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.ChemicalStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.FluidStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.DataBase.StackHandlerWrapper.ChemicalHandlerWrapper;
import com.wintercogs.beyonddimensions.DataBase.StackHandlerWrapper.FluidHandlerWrapper;
import com.wintercogs.beyonddimensions.DataBase.StackHandlerWrapper.ItemHandlerWrapper;
import com.wintercogs.beyonddimensions.DataBase.Storage.ChemicalUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.DataBase.Storage.FluidUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.DataBase.Storage.ItemUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.DataComponents.ModDataComponents;
import com.wintercogs.beyonddimensions.Integration.Mek.Capability.ChemicalCapabilityHelper;
import com.wintercogs.beyonddimensions.Item.ModCreativeModeTabs;
import com.wintercogs.beyonddimensions.Item.ModItems;
import com.wintercogs.beyonddimensions.Registry.StackTypeRegistry;
import com.wintercogs.beyonddimensions.Registry.UIRegister;
import com.wintercogs.beyonddimensions.Unit.CapabilityHelper;
import com.wintercogs.beyonddimensions.Unit.StackHandlerWrapperHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

@Mod(BeyondDimensions.MODID)
public class BeyondDimensions
{
    public static final String MODID = "beyonddimensions";
    public static boolean MekLoaded = false; // 用于mek化学品存储
    public static final String MekanismMODID = "mekanism";
    public static boolean AELoaded = false;
    public static final String AE2MODID = "ae2";
    public static boolean EMILoaded = false;
    public static final String EMI_MODID = "emi";
    public static final Logger LOGGER = LogUtils.getLogger();

    // mod 类的构造函数是加载 mod 时运行的第一个代码。
    // FML 将识别一些参数类型，如 IEventBus 或 ModContainer 并自动传入它们。
    public BeyondDimensions(IEventBus modEventBus, ModContainer modContainer)
    {

        modEventBus.addListener(this::commonSetup);
        //为存储网络的接口方块注册物品交互能力

        modEventBus.addListener(NetInterfaceBlockEntity::registerCapability);
        modEventBus.addListener(NetPathwayBlockEntity::registerCapability);
        modEventBus.addListener(NetEnergyPathwayBlockEntity::registerCapability);

        // 注册事件
        NeoForge.EVENT_BUS.register(this);//注册this类中所有事件

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // 调用UIRegister的构造函数，从而注册所有UI
        UIRegister.register(modEventBus);

        // 注册创造模式菜单
        ModCreativeModeTabs.register(modEventBus);

        // 注册物品组件
        ModDataComponents.register(modEventBus);

        // 注册物品
        ModItems.register(modEventBus);

        // 注册方块
        ModBlocks.register(modEventBus);

        // 注册方块实体
        ModBlockEntities.register(modEventBus);

    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        if(ModList.get().isLoaded(MekanismMODID))
        {
            MekLoaded = true;
        }
        if(ModList.get().isLoaded(AE2MODID))
        {
            AELoaded = true;
        }
        if(ModList.get().isLoaded(EMI_MODID))
        {
            EMILoaded = true;
        }

        // 注册堆叠类型，使得网络能够存储相关堆叠
        StackTypeRegistry.registerType(new ItemStackType());
        StackTypeRegistry.registerType(new FluidStackType());

        // 注册方块能力类型，用于动态为方块注册能力
        CapabilityHelper.BlockCapabilityMap.put(ItemStackType.ID,Capabilities.ItemHandler.BLOCK);
        CapabilityHelper.BlockCapabilityMap.put(FluidStackType.ID,Capabilities.FluidHandler.BLOCK);

        // 注册网络能力，使得网络通道能暴露对应存储能力 注:能量存储无需注册，单独实现
        UnifiedStorage.typedHandlerMap.put(ItemStackType.ID,ItemUnifiedStorageHandler::new);
        UnifiedStorage.typedHandlerMap.put(FluidStackType.ID,FluidUnifiedStorageHandler::new);

        // 注册存储分化包装
        StackTypedHandler.typedHandlerMap.put(ItemStackType.ID,ItemStackTypedHandler::new);
        StackTypedHandler.typedHandlerMap.put(FluidStackType.ID,FluidStackTypedHandler::new);

        // 注册堆叠处理包装，用于动态包装来自其他模组的handler (如原版的IItemHandler)
        StackHandlerWrapperHelper.stackWrappers.put(ItemStackType.ID, ItemHandlerWrapper::new);
        StackHandlerWrapperHelper.stackWrappers.put(FluidStackType.ID, FluidHandlerWrapper::new);

        if(MekLoaded)
        {
            // 注册化学品堆叠
            StackTypeRegistry.registerType(new ChemicalStackType());
            // 注册化学品方块能力
            CapabilityHelper.BlockCapabilityMap.put(ChemicalStackType.ID, ChemicalCapabilityHelper.CHEMICAL);
            // 注册分化包装
            UnifiedStorage.typedHandlerMap.put(ChemicalStackType.ID,ChemicalUnifiedStorageHandler::new);
            StackTypedHandler.typedHandlerMap.put(ChemicalStackType.ID,ChemicalStackTypedHandler::new);

            // 注册堆叠处理包装
            StackHandlerWrapperHelper.stackWrappers.put(ChemicalStackType.ID, ChemicalHandlerWrapper::new);

        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("维度网络初始化完成(服务端)");
    }


    // 你可以使用EventBusSubscriber来自动注册类中所有标注了@SubscribeEvent的静态方法。
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // 一些客户端初始代码
            LOGGER.info("维度网络初始化完成(客户端)");
        }
    }
}
