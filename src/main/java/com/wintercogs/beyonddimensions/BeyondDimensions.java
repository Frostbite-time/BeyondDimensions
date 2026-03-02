package com.wintercogs.beyonddimensions;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.EnergyStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.FluidStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.ItemStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EmptyStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EnergyStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.FluidStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.EnergyHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.FluidHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.ItemHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.EnergyUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.FluidUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.ItemUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.Api.Registry.CapabilityHelper;
import com.wintercogs.beyonddimensions.Api.Registry.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.Api.Registry.StackKeyRegistry;
import com.wintercogs.beyonddimensions.common.init.BDBlocks;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetEnergyPathwayBlockEntity;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetFurnaceBlockEntity;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetInterfaceBlockEntity;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetPathwayBlockEntity;
import com.wintercogs.beyonddimensions.common.init.BDBlockEntities;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.Fluid.ModFluids;
import com.wintercogs.beyonddimensions.Integration.Ars.BD_ArsCaps;
import com.wintercogs.beyonddimensions.Integration.Curios.BD_CuriosPlugin;
import com.wintercogs.beyonddimensions.Integration.IFS.Item.WardenSoulTagItem;
import com.wintercogs.beyonddimensions.Integration.create.blocks.entities.SchematicannonPathWayBlockEntity;
import com.wintercogs.beyonddimensions.common.init.BDCreativeModeTabs;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.Registry.UIRegister;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

@Mod(BeyondDimensions.MODID)
public class BeyondDimensions
{
    public static final String MODID = "beyonddimensions";
    public static IEventBus MOD_EVENT_BUS;

    public static boolean MekLoaded = false; // 用于mek化学品存储
    public static final String MekanismMODID = "mekanism";
    public static boolean AELoaded = false; // 用于添加存储元件
    public static final String AE2MODID = "ae2";
    public static boolean EMILoaded = false; // 用于EMI兼容
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
    public static boolean JECharactersLoaded = false;
    public static final String JECharactersModId = "jecharacters";
    public static final String RSModId = "refinedstorage";
    public static boolean RS_Loaded = false;
    public static final String RS_MEK_MODID = "refinedstorage_mekanism_integration";
    public static boolean RS_MEK_Loaded = false;
    public static final String IFS_ModId = "industrialforegoingsouls"; //工业先锋-灵魂涌动
    public static boolean IFS_Loaded = false;
    public static final String AE_IFS_ModId = "soulplied_energistics"; // 工业先锋-灵魂涌动-AE附属
    public static boolean AE_IFS_Loaded = false;
    public static final String ARS_ModId = "ars_nouveau"; // 新生魔艺-魔源兼容
    public static boolean ARS_Loaded = false;
    public static final String AE_ARS_ModId = "arseng";
    public static boolean AE_ARS_Loaded = false;
    public static final String Botania_ModId = "botania"; // 植物魔法-mana兼容
    public static boolean Botania_Loaded = false;
    public static final String AE_Botania_ModId = "appbot";
    public static boolean AE_Botania_Loaded = false;
    public static final String RSTypesModId = "refinedtypes";
    public static boolean RSTypesLoaded = false;
    public static final String Create_ModId = "create";
    public static boolean Create_Loaded = false;
    public static final Logger LOGGER = LogUtils.getLogger();

    // mod 类的构造函数是加载 mod 时运行的第一个代码。
    // FML 将识别一些参数类型，如 IEventBus 或 ModContainer 并自动传入它们。
    public BeyondDimensions(IEventBus modEventBus, ModContainer modContainer)
    {
        MOD_EVENT_BUS = modEventBus;
        NeoForge.EVENT_BUS.register(this);//注册this类中所有事件
        Config.register(modContainer);

        modEventBus.addListener(this::constructMod);
        modEventBus.addListener(this::commonSetup);
        //为存储网络的接口方块注册物品交互能力

        modEventBus.addListener(NetInterfaceBlockEntity::registerCapability);
        modEventBus.addListener(NetPathwayBlockEntity::registerCapability);
        modEventBus.addListener(NetEnergyPathwayBlockEntity::registerCapability);
        modEventBus.addListener(NetFurnaceBlockEntity::registerCapability);

        // 调用UIRegister的构造函数，从而注册所有UI
        UIRegister.register(modEventBus);

        // 注册创造模式菜单
        BDCreativeModeTabs.register(modEventBus);

        // 注册物品组件
        BDDataComponents.register(modEventBus);

        // 注册物品
        BDItems.register(modEventBus);

        // 注册方块
        BDBlocks.register(modEventBus);

        // 注册流体
        ModFluids.register(modEventBus);

        // 注册方块实体
        BDBlockEntities.register(modEventBus);


    }

    // 在此阶段检测模组列表
    private void constructMod(final FMLConstructModEvent event)
    {
        if (ModList.get().isLoaded(MekanismMODID))
        {
            MekLoaded = true;
        }
        if (ModList.get().isLoaded(AE2MODID))
        {
            AELoaded = true;
        }
        if (ModList.get().isLoaded(EMI_MODID))
        {
            EMILoaded = true;
        }
        if (ModList.get().isLoaded(JEI2MODID))
        {
            JEILoaded = true;
        }
        if (ModList.get().isLoaded(PolymorphModId))
        {
            PolymorphLoaded = true;
        }
        if (ModList.get().isLoaded(AEMEK2MODID))
        {
            AEMEKLoaded = true;
        }
        if (ModList.get().isLoaded(AEFlux2MODID))
        {
            AEFluxLoaded = true;
        }
        if (ModList.get().isLoaded(CuriosModId))
        {
            CuriosLoaded = true;
            MOD_EVENT_BUS.addListener(BD_CuriosPlugin::registerCapabilities);
        }
        if (ModList.get().isLoaded(JECharactersModId))
        {
            JECharactersLoaded = true;
        }
        if (ModList.get().isLoaded(RSModId))
        {
            RS_Loaded = true;
        }
        if (ModList.get().isLoaded(RS_MEK_MODID))
        {
            RS_MEK_Loaded = true;
        }
        if (ModList.get().isLoaded(IFS_ModId))
        {
            IFS_Loaded = true;
        }
        if (ModList.get().isLoaded(AE_IFS_ModId))
        {
            AE_IFS_Loaded = true;
        }
        if (ModList.get().isLoaded(ARS_ModId))
        {
            ARS_Loaded = true;
        }
        if (ModList.get().isLoaded(AE_ARS_ModId))
        {
            AE_ARS_Loaded = true;
        }
        if (ModList.get().isLoaded(Botania_ModId))
        {
            Botania_Loaded = true;
        }
        if (ModList.get().isLoaded(AE_Botania_ModId))
        {
            AE_Botania_Loaded = true;
        }
        if (ModList.get().isLoaded(RSTypesModId))
        {
            RSTypesLoaded = true;
        }
        if (ModList.get().isLoaded(Create_ModId))
        {
            Create_Loaded = true;
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {

        // 注册堆叠类型，使得网络能够存储相关堆叠
        StackKeyRegistry.registerType(EmptyStackKey.INSTANCE); // 全空堆叠，用于避免使用null
        StackKeyRegistry.registerType(ItemStackKey.EMPTY);
        StackKeyRegistry.registerType(FluidStackKey.EMPTY);
        StackKeyRegistry.registerType(EnergyStackKey.INSTANCE);

        // 注册方块能力类型，用于动态为方块注册能力
        CapabilityHelper.BlockCapabilityMap.put(ItemStackKey.ID, Capabilities.Item.BLOCK);
        CapabilityHelper.BlockCapabilityMap.put(FluidStackKey.ID, Capabilities.Fluid.BLOCK);
        CapabilityHelper.BlockCapabilityMap.put(EnergyStackKey.ID, Capabilities.Energy.BLOCK);
        // 注册物品能力，用于动态操作
        CapabilityHelper.ItemCapabilityMap.put(ItemStackKey.ID, Capabilities.Item.ITEM);
        CapabilityHelper.ItemCapabilityMap.put(FluidStackKey.ID, Capabilities.Fluid.ITEM);
        CapabilityHelper.ItemCapabilityMap.put(EnergyStackKey.ID, Capabilities.Energy.ITEM);

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
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("维度网络初始化完成(服务端)");
    }

    @SubscribeEvent
    public void onServerStared(ServerStartedEvent event)
    {
        //GameTester.OnSeverStartTester(event.getServer());
    }
}
