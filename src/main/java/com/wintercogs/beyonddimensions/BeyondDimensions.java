package com.wintercogs.beyonddimensions;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.Chemicals.GasStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.Chemicals.InfusionStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.Chemicals.PigmentStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.Chemicals.SlurryStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.EnergyStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.FluidStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.ItemStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.GasStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.InfusionStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.PigmentStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.SlurryStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EnergyStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.FluidStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.Chemicals.GasHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.Chemicals.InfusionHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.Chemicals.PigmentHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.Chemicals.SlurryHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.EnergyHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.FluidHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.ItemHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.Chemicals.GasUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.Chemicals.InfusionUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.Chemicals.PigmentUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.Chemicals.SlurryUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.EnergyUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.FluidUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.ItemUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.Api.Registry.CapabilityHelper;
import com.wintercogs.beyonddimensions.Api.Registry.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.Api.Registry.StackTypeRegistry;
import com.wintercogs.beyonddimensions.Block.ModBlocks;
import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.Integration.AE.BD_AEPlugin;
import com.wintercogs.beyonddimensions.Integration.AEFlux.BD_AEFluxPlugin;
import com.wintercogs.beyonddimensions.Integration.AEMEK.BD_AEMEKPlugin;
import com.wintercogs.beyonddimensions.Integration.Curios.BD_CuriosPlugin;
import com.wintercogs.beyonddimensions.Integration.Mek.Capability.ChemicalCapabilityHelper;
import com.wintercogs.beyonddimensions.Integration.Polymorph.PolymorphPlug;
import com.wintercogs.beyonddimensions.Item.ModCreativeModeTabs;
import com.wintercogs.beyonddimensions.Item.ModItems;
import com.wintercogs.beyonddimensions.Registry.UIRegister;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;


@Mod(BeyondDimensions.MODID)
public class BeyondDimensions
{
    public static final String MODID = "beyonddimensions";
    public static IEventBus MOD_EVENT_BUS;

    public static boolean MekLoaded = false; // 用于mek化学品存储
    public static final String MekanismMODID = "mekanism";
    public static boolean AELoaded = false;
    public static final String AE2MODID = "ae2";
    public static boolean EMILoaded = false;
    public static final String EMI_MODID = "emi";
    public static boolean JEILoaded = false; // 用于JEI兼容
    public static final String JEI2MODID = "jei";
    public static boolean PolymorphLoaded = false;
    public static final String PolymorphModId = "polymorph";
    public static boolean AEMEKLoaded = false;
    public static final String AEMEK2MODID = "appmek";
    public static boolean AEFluxLoaded = false;
    public static final String AEFlux2MODID = "appflux";
    public static boolean CuriosLoaded = false;
    public static final String CuriosModId = "curios";
    public static final Logger LOGGER = LogUtils.getLogger();

    // mod 类的构造函数是加载 mod 时运行的第一个代码。
    // FML 将识别一些参数类型，如 IEventBus 或 ModContainer 并自动传入它们。
    public BeyondDimensions()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MOD_EVENT_BUS = modEventBus;

        modEventBus.addListener(this::constructMod);
        modEventBus.addListener(this::commonSetup);
        //为存储网络的接口方块注册物品交互能力


        // 注册事件
        MinecraftForge.EVENT_BUS.register(this);//注册this类中所有事件

        // 注册模组的ForgeConfigSpec以便Forge可以创建和加载配置文件
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // 调用UIRegister的构造函数，从而注册所有UI
        UIRegister.register(modEventBus);

        // 注册创造模式菜单
        ModCreativeModeTabs.register(modEventBus);

        // 注册物品
        ModItems.register(modEventBus);

        // 注册方块
        ModBlocks.register(modEventBus);

        // 注册方块实体
        ModBlockEntities.register(modEventBus);

    }

    private void constructMod(final FMLConstructModEvent event)
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
        if(ModList.get().isLoaded(JEI2MODID))
        {
            JEILoaded = true;
        }
        if(ModList.get().isLoaded(PolymorphModId))
        {
            PolymorphLoaded = true;
        }
        if(ModList.get().isLoaded(AEMEK2MODID))
        {
            AEMEKLoaded = true;
        }
        if(ModList.get().isLoaded(AEFlux2MODID))
        {
            AEFluxLoaded = true;
        }
        if(ModList.get().isLoaded(CuriosModId))
        {
            CuriosLoaded = true;
            MinecraftForge.EVENT_BUS.addGenericListener(ItemStack.class, BD_CuriosPlugin::registerCapabilities);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {

        // 注册堆叠类型，使得网络能够存储相关堆叠
        StackTypeRegistry.registerType(new ItemStackType());
        StackTypeRegistry.registerType(new FluidStackType());
        StackTypeRegistry.registerType(new EnergyStackType());

        // 注册方块能力类型，用于动态为方块注册能力
        CapabilityHelper.BlockCapabilityMap.put(ItemStackType.ID, ForgeCapabilities.ITEM_HANDLER);
        CapabilityHelper.BlockCapabilityMap.put(FluidStackType.ID,ForgeCapabilities.FLUID_HANDLER);
        CapabilityHelper.BlockCapabilityMap.put(EnergyStackType.ID,ForgeCapabilities.ENERGY);

        // 注册物品能力类型
        CapabilityHelper.ItemCapabilityMap.put(ItemStackType.ID, ForgeCapabilities.ITEM_HANDLER);
        CapabilityHelper.ItemCapabilityMap.put(FluidStackType.ID, ForgeCapabilities.FLUID_HANDLER_ITEM);
        CapabilityHelper.ItemCapabilityMap.put(EnergyStackType.ID,ForgeCapabilities.ENERGY);

        // 注册网络能力，使得网络通道能暴露对应存储能力 注:能量存储无需注册，单独实现
        UnifiedStorage.typedHandlerMap.put(ItemStackType.ID,ItemUnifiedStorageHandler::new);
        UnifiedStorage.typedHandlerMap.put(FluidStackType.ID,FluidUnifiedStorageHandler::new);
        UnifiedStorage.typedHandlerMap.put(EnergyStackType.ID, EnergyUnifiedStorageHandler::new);

        // 注册存储分化包装
        StackTypedHandler.typedHandlerMap.put(ItemStackType.ID,ItemStackTypedHandler::new);
        StackTypedHandler.typedHandlerMap.put(FluidStackType.ID,FluidStackTypedHandler::new);
        StackTypedHandler.typedHandlerMap.put(EnergyStackType.ID, EnergyStackTypedHandler::new);

        // 注册堆叠处理包装，用于动态包装来自其他模组的handler (如原版的IItemHandler)
        StackHandlerWrapperHelper.stackWrappers.put(ItemStackType.ID, ItemHandlerWrapper::new);
        StackHandlerWrapperHelper.stackWrappers.put(FluidStackType.ID, FluidHandlerWrapper::new);
        StackHandlerWrapperHelper.stackWrappers.put(EnergyStackType.ID, EnergyHandlerWrapper::new);

        if(MekLoaded)
        {
            // 注册化学品堆叠
            StackTypeRegistry.registerType(new GasStackType());
            StackTypeRegistry.registerType(new InfusionStackType());
            StackTypeRegistry.registerType(new PigmentStackType());
            StackTypeRegistry.registerType(new SlurryStackType());
            // 注册化学品方块能力
            CapabilityHelper.BlockCapabilityMap.put(GasStackType.ID, ChemicalCapabilityHelper.GAS);
            CapabilityHelper.BlockCapabilityMap.put(InfusionStackType.ID, ChemicalCapabilityHelper.INFUSION);
            CapabilityHelper.BlockCapabilityMap.put(PigmentStackType.ID,ChemicalCapabilityHelper.PIGMENT);
            CapabilityHelper.BlockCapabilityMap.put(SlurryStackType.ID, ChemicalCapabilityHelper.SLURRY);
            // 注册化学品物品能力
            CapabilityHelper.ItemCapabilityMap.put(GasStackType.ID, ChemicalCapabilityHelper.GAS);
            CapabilityHelper.ItemCapabilityMap.put(InfusionStackType.ID, ChemicalCapabilityHelper.INFUSION);
            CapabilityHelper.ItemCapabilityMap.put(PigmentStackType.ID, ChemicalCapabilityHelper.PIGMENT);
            CapabilityHelper.ItemCapabilityMap.put(SlurryStackType.ID, ChemicalCapabilityHelper.SLURRY);

            // 注册分化包装
            UnifiedStorage.typedHandlerMap.put(GasStackType.ID, GasUnifiedStorageHandler::new);
            UnifiedStorage.typedHandlerMap.put(InfusionStackType.ID, InfusionUnifiedStorageHandler::new);
            UnifiedStorage.typedHandlerMap.put(PigmentStackType.ID, PigmentUnifiedStorageHandler::new);
            UnifiedStorage.typedHandlerMap.put(SlurryStackType.ID, SlurryUnifiedStorageHandler::new);

            StackTypedHandler.typedHandlerMap.put(GasStackType.ID, GasStackTypedHandler::new);
            StackTypedHandler.typedHandlerMap.put(InfusionStackType.ID, InfusionStackTypedHandler::new);
            StackTypedHandler.typedHandlerMap.put(PigmentStackType.ID, PigmentStackTypedHandler::new);
            StackTypedHandler.typedHandlerMap.put(SlurryStackType.ID, SlurryStackTypedHandler::new);

            // 注册堆叠处理包装
            StackHandlerWrapperHelper.stackWrappers.put(GasStackType.ID, GasHandlerWrapper::new);
            StackHandlerWrapperHelper.stackWrappers.put(InfusionStackType.ID, InfusionHandlerWrapper::new);
            StackHandlerWrapperHelper.stackWrappers.put(PigmentStackType.ID, PigmentHandlerWrapper::new);
            StackHandlerWrapperHelper.stackWrappers.put(SlurryStackType.ID, SlurryHandlerWrapper::new);

        }

        // 为维度ME硬盘注册，其中BD_AEPlugin用于注册存储元件
        // BD_AEMEKPlugin与BD_AEFluxPlugin分别注册IStackType与AEKey之间的转换。
        // 物品、流体的转换由AEHelper的静态块负责
        if(AELoaded)
        {
            BD_AEPlugin.register();
        }
        if(AEMEKLoaded)
        {
            BD_AEMEKPlugin.register();
        }
        if(AEFluxLoaded)
        {
            BD_AEFluxPlugin.register();
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("维度网络初始化完成(服务端)");
    }


    // 你可以使用EventBusSubscriber来自动注册类中所有标注了@SubscribeEvent的静态方法。
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // 一些客户端初始代码
            LOGGER.info("维度网络初始化完成(客户端)");
            UIRegister.registerScreens(event);

            if(PolymorphLoaded)
            {
                PolymorphPlug.register();
            }
        }
    }
}
