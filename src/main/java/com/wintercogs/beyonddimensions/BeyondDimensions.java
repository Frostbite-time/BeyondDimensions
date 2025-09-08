package com.wintercogs.beyonddimensions;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.*;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.*;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.*;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.*;
import com.wintercogs.beyonddimensions.Api.Registry.CapabilityHelper;
import com.wintercogs.beyonddimensions.Api.Registry.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.Api.Registry.StackKeyRegistry;
import com.wintercogs.beyonddimensions.Block.ModBlocks;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetEnergyPathwayBlockEntity;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetFurnaceBlockEntity;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetInterfaceBlockEntity;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetPathwayBlockEntity;
import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.BlockRender.ModBlockRenders;
import com.wintercogs.beyonddimensions.DataComponents.ModDataComponents;
import com.wintercogs.beyonddimensions.Fluid.ModFluids;
import com.wintercogs.beyonddimensions.Integration.AE.BD_AEPlugin;
import com.wintercogs.beyonddimensions.Integration.AEFlux.BD_AEFluxPlugin;
import com.wintercogs.beyonddimensions.Integration.AEMEK.BD_AEMEKPlugin;
import com.wintercogs.beyonddimensions.Integration.AE_Ars.BD_AE_ArsPlugin;
import com.wintercogs.beyonddimensions.Integration.AE_IFS.BD_AE_IFS_Plugin;
import com.wintercogs.beyonddimensions.Integration.Ars.BD_ArsCaps;
import com.wintercogs.beyonddimensions.Integration.Botania.BD_BotaniaPlugin;
import com.wintercogs.beyonddimensions.Integration.Botania.Block.ManaPoolPathwayBlockEntity;
import com.wintercogs.beyonddimensions.Integration.Botania.HudOverlay.ManaPoolPathwayOverlay;
import com.wintercogs.beyonddimensions.Integration.Curios.BD_CuriosPlugin;
import com.wintercogs.beyonddimensions.Integration.IFS.BD_SoulCaps;
import com.wintercogs.beyonddimensions.Integration.IFS.Item.WardenSoulTagItem;
import com.wintercogs.beyonddimensions.Integration.Mek.Capability.ChemicalCapabilityHelper;
import com.wintercogs.beyonddimensions.Integration.Polymorph.PolymorphPlug;
import com.wintercogs.beyonddimensions.Integration.RS.BD_RSPlugin;
import com.wintercogs.beyonddimensions.Integration.RSMek.BD_RSMekPlugin;
import com.wintercogs.beyonddimensions.Integration.RSTypes.BD_RSTypesPlugin;
import com.wintercogs.beyonddimensions.Item.ModCreativeModeTabs;
import com.wintercogs.beyonddimensions.Item.ModItems;
import com.wintercogs.beyonddimensions.Registry.UIRegister;
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
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
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
    public static final String RSTypesModId = "refinedtypes";
    public static boolean RSTypesLoaded = false;
    public static final Logger LOGGER = LogUtils.getLogger();

    // mod 类的构造函数是加载 mod 时运行的第一个代码。
    // FML 将识别一些参数类型，如 IEventBus 或 ModContainer 并自动传入它们。
    public BeyondDimensions(IEventBus modEventBus, ModContainer modContainer)
    {
        MOD_EVENT_BUS = modEventBus;

        modEventBus.addListener(this::constructMod);
        modEventBus.addListener(this::commonSetup);
        //为存储网络的接口方块注册物品交互能力

        modEventBus.addListener(NetInterfaceBlockEntity::registerCapability);
        modEventBus.addListener(NetPathwayBlockEntity::registerCapability);
        modEventBus.addListener(NetEnergyPathwayBlockEntity::registerCapability);
        modEventBus.addListener(NetFurnaceBlockEntity::registerCapability);

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

        // 注册流体
        ModFluids.register(modEventBus);

        // 注册方块实体
        ModBlockEntities.register(modEventBus);

        if(FMLEnvironment.dist == Dist.CLIENT)
        {
            // 注册方块实体渲染
            modEventBus.addListener(ModBlockRenders::onRegisterRenderers);
        }

    }

    // 在此阶段检测模组列表
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
        if(ModList.get().isLoaded(IFS_ModId))
        {
            IFS_Loaded = true;
            MOD_EVENT_BUS.addListener(WardenSoulTagItem::registerCapability);
        }
        if(ModList.get().isLoaded(AE_IFS_ModId))
        {
            AE_IFS_Loaded = true;
        }
        if(ModList.get().isLoaded(ARS_ModId))
        {
            ARS_Loaded = true;
            MOD_EVENT_BUS.addListener(BD_ArsCaps::registerCapability);
        }
        if(ModList.get().isLoaded(AE_ARS_ModId))
        {
            AE_ARS_Loaded = true;
        }
        if(ModList.get().isLoaded(Botania_ModId))
        {
            Botania_Loaded = true;
            MOD_EVENT_BUS.addListener(ManaPoolPathwayBlockEntity::registerCapability);
            MOD_EVENT_BUS.addListener(BD_BotaniaPlugin::registerCapability); // 为网络通道和网络接口手动注册火花附着
        }
        if(ModList.get().isLoaded(RSTypesModId))
        {
            RSTypesLoaded = true;
        }

        ModBlockEntities.IntegrationRegister(); // 模组列表检查完成后，动态注册方块实体
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {

        // 注册堆叠类型，使得网络能够存储相关堆叠
        StackKeyRegistry.registerType(EmptyStackKey.INSTANCE); // 全空堆叠，用于避免使用null
        StackKeyRegistry.registerType(ItemStackKey.EMPTY);
        StackKeyRegistry.registerType(FluidStackKey.EMPTY);
        StackKeyRegistry.registerType(EnergyStackKey.INSTANCE);

        // 注册方块能力类型，用于动态为方块注册能力
        CapabilityHelper.BlockCapabilityMap.put(ItemStackKey.ID,Capabilities.ItemHandler.BLOCK);
        CapabilityHelper.BlockCapabilityMap.put(FluidStackKey.ID,Capabilities.FluidHandler.BLOCK);
        CapabilityHelper.BlockCapabilityMap.put(EnergyStackKey.ID, Capabilities.EnergyStorage.BLOCK);
        // 注册物品能力，用于动态操作
        CapabilityHelper.ItemCapabilityMap.put(ItemStackKey.ID,Capabilities.ItemHandler.ITEM);
        CapabilityHelper.ItemCapabilityMap.put(FluidStackKey.ID,Capabilities.FluidHandler.ITEM);
        CapabilityHelper.ItemCapabilityMap.put(EnergyStackKey.ID, Capabilities.EnergyStorage.ITEM);

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

        if(MekLoaded)
        {
            // 注册化学品堆叠
            StackKeyRegistry.registerType(ChemicalStackKey.EMPTY);
            // 注册化学品方块能力
            CapabilityHelper.BlockCapabilityMap.put(ChemicalStackKey.ID, ChemicalCapabilityHelper.CHEMICAL_BLOCK);
            // 注册化学品物品能力
            CapabilityHelper.ItemCapabilityMap.put(ChemicalStackKey.ID, ChemicalCapabilityHelper.CHEMICAL_ITEM);
            // 注册分化包装
            CapabilityHelper.registerUSHandler(ChemicalStackKey.EMPTY, ChemicalUnifiedStorageHandler::new);
            CapabilityHelper.registerStackTypedHandler(ChemicalStackKey.EMPTY, ChemicalStackTypedHandler::new);

            // 注册堆叠处理包装
            StackHandlerWrapperHelper.stackWrappers.put(ChemicalStackKey.ID, ChemicalHandlerWrapper::new);

        }

        if(IFS_Loaded)
        {
            // 注册监守者之魂
            StackKeyRegistry.registerType(WardenSoulStackKey.INSTANCE);
            CapabilityHelper.BlockCapabilityMap.put(WardenSoulStackKey.ID, com.buuz135.industrialforegoingsouls.capabilities.SoulCapabilities.BLOCK);
            // 此处为自定义物品能力，因为原模组未提供物品能力
            CapabilityHelper.ItemCapabilityMap.put(WardenSoulStackKey.ID, BD_SoulCaps.ITEM);
            // 注册分化包装
            CapabilityHelper.registerUSHandler(WardenSoulStackKey.INSTANCE, WardenSoulUnifiedStorageHandler::new);
            CapabilityHelper.registerStackTypedHandler(WardenSoulStackKey.INSTANCE, WardenSoulStackTypedHandler::new);
            // 注册堆叠处理包装
            StackHandlerWrapperHelper.stackWrappers.put(WardenSoulStackKey.ID, WardenSoulHandlerWrapper::new);
        }

        if(ARS_Loaded)
        {
            // 注册魔源
            StackKeyRegistry.registerType(SourceStackKey.INSTANCE);
            CapabilityHelper.BlockCapabilityMap.put(SourceStackKey.ID, com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.SOURCE_CAPABILITY);
            CapabilityHelper.ItemCapabilityMap.put(SourceStackKey.ID, BD_ArsCaps.ITEM_SOURCE); // 使用的自己的魔源罐能力
            CapabilityHelper.registerUSHandler(SourceStackKey.INSTANCE, SourceUnifiedStorageHandler::new);
            CapabilityHelper.registerStackTypedHandler(SourceStackKey.INSTANCE, SourceStackTypedHandler::new);
            StackHandlerWrapperHelper.stackWrappers.put(SourceStackKey.ID, SourceHandlerWrapper::new);
        }

        if(Botania_Loaded)
        {
            // 注册Mana（魔力）
            StackKeyRegistry.registerType(ManaStackKey.INSTANCE);
            CapabilityHelper.BlockCapabilityMap.put(ManaStackKey.ID, vazkii.botania.api.BotaniaForgeCapabilities.MANA_RECEIVER);
            CapabilityHelper.ItemCapabilityMap.put(ManaStackKey.ID, vazkii.botania.api.BotaniaForgeCapabilities.MANA_ITEM);
            CapabilityHelper.registerUSHandler(ManaStackKey.INSTANCE, ManaUnifiedStorageHandler::new);
            CapabilityHelper.registerStackTypedHandler(ManaStackKey.INSTANCE, ManaStackTypedHandler::new);
            StackHandlerWrapperHelper.stackWrappers.put(ManaStackKey.ID, ManaHandlerWrapper::new);

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
        if(RS_Loaded)
        {
            BD_RSPlugin.register();
        }
        if(RS_MEK_Loaded)
        {
            BD_RSMekPlugin.register();
        }
        if(RSTypesLoaded)
        {
            BD_RSTypesPlugin.register();
        }
        if(AE_IFS_Loaded)
        {
            BD_AE_IFS_Plugin.register();
        }
        if(AE_ARS_Loaded)
        {
            BD_AE_ArsPlugin.register();
        }

        // 注册物品能力交互黑名单
        if(Botania_Loaded)
        {
            BD_BotaniaPlugin.registerItemCapBlackList();
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("维度网络初始化完成(服务端)");
        //GameTester.OnSeverStartTester(event.getServer());
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


            if(PolymorphLoaded)
            {
                PolymorphPlug.register();
            }
            if(Botania_Loaded)
            {
                NeoForge.EVENT_BUS.register(ManaPoolPathwayOverlay.class);
            }
        }
    }
}
