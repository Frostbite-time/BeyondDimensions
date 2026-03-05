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
import com.wintercogs.beyonddimensions.DataComponents.ModDataComponents;
import com.wintercogs.beyonddimensions.Fluid.ModFluids;
import com.wintercogs.beyonddimensions.integration.AEFlux.BD_AEFluxPlugin;
import com.wintercogs.beyonddimensions.integration.AE_Ars.BD_AE_ArsPlugin;
import com.wintercogs.beyonddimensions.integration.AE_IFS.BD_AE_IFS_Plugin;
import com.wintercogs.beyonddimensions.integration.Ars.BD_ArsCaps;
import com.wintercogs.beyonddimensions.integration.Botania.BD_BotaniaPlugin;
import com.wintercogs.beyonddimensions.integration.Botania.Block.ManaPoolPathwayBlockEntity;
import com.wintercogs.beyonddimensions.integration.Curios.BD_CuriosPlugin;
import com.wintercogs.beyonddimensions.integration.IFS.BD_SoulCaps;
import com.wintercogs.beyonddimensions.integration.IFS.Item.WardenSoulTagItem;
import com.wintercogs.beyonddimensions.integration.IntegrationManager;
import com.wintercogs.beyonddimensions.integration.RS.BD_RSPlugin;
import com.wintercogs.beyonddimensions.integration.RSMek.BD_RSMekPlugin;
import com.wintercogs.beyonddimensions.integration.RSTypes.BD_RSTypesPlugin;
import com.wintercogs.beyonddimensions.integration.botania_ae.BD_AEBotaniaPlugin;
import com.wintercogs.beyonddimensions.integration.create.blocks.entities.SchematicannonPathWayBlockEntity;
import com.wintercogs.beyonddimensions.Item.ModCreativeModeTabs;
import com.wintercogs.beyonddimensions.Item.ModItems;
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
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;
import vazkii.botania.api.BotaniaForgeCapabilities;

@Mod(BeyondDimensions.MODID)
public class BeyondDimensions
{
    public static final String MODID = "beyonddimensions";
    public static IEventBus MOD_EVENT_BUS;

    public static boolean AELoaded = false; // 用于添加存储元件
    public static final String AE2MODID = "ae2";
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

    public BeyondDimensions(IEventBus modEventBus, ModContainer modContainer)
    {
        MOD_EVENT_BUS = modEventBus;
        NeoForge.EVENT_BUS.register(this);
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

        // 分发集成模块
        IntegrationManager.bootstrapCommon(modEventBus, NeoForge.EVENT_BUS);
    }

    // 在此阶段检测模组列表
    private void constructMod(final FMLConstructModEvent event)
    {
        if (ModList.get().isLoaded(AE2MODID))
        {
            AELoaded = true;
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
            MOD_EVENT_BUS.addListener(WardenSoulTagItem::registerCapability);
        }
        if (ModList.get().isLoaded(AE_IFS_ModId))
        {
            AE_IFS_Loaded = true;
        }
        if (ModList.get().isLoaded(ARS_ModId))
        {
            ARS_Loaded = true;
            MOD_EVENT_BUS.addListener(BD_ArsCaps::registerCapability);
        }
        if (ModList.get().isLoaded(AE_ARS_ModId))
        {
            AE_ARS_Loaded = true;
        }
        if (ModList.get().isLoaded(Botania_ModId))
        {
            Botania_Loaded = true;
            MOD_EVENT_BUS.addListener(ManaPoolPathwayBlockEntity::registerCapability);
            MOD_EVENT_BUS.addListener(BD_BotaniaPlugin::registerCapability); // 为网络通道和网络接口手动注册火花附着
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
            MOD_EVENT_BUS.addListener(SchematicannonPathWayBlockEntity::registerCapability);
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
        CapabilityHelper.BlockCapabilityMap.put(ItemStackKey.ID, Capabilities.ItemHandler.BLOCK);
        CapabilityHelper.BlockCapabilityMap.put(FluidStackKey.ID, Capabilities.FluidHandler.BLOCK);
        CapabilityHelper.BlockCapabilityMap.put(EnergyStackKey.ID, Capabilities.EnergyStorage.BLOCK);
        // 注册物品能力，用于动态操作
        CapabilityHelper.ItemCapabilityMap.put(ItemStackKey.ID, Capabilities.ItemHandler.ITEM);
        CapabilityHelper.ItemCapabilityMap.put(FluidStackKey.ID, Capabilities.FluidHandler.ITEM);
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

        if (IFS_Loaded)
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

        if (ARS_Loaded)
        {
            // 注册魔源
            StackKeyRegistry.registerType(SourceStackKey.INSTANCE);
            CapabilityHelper.BlockCapabilityMap.put(SourceStackKey.ID, com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.SOURCE_CAPABILITY);
            CapabilityHelper.ItemCapabilityMap.put(SourceStackKey.ID, BD_ArsCaps.ITEM_SOURCE); // 使用的自己的魔源罐能力
            CapabilityHelper.registerUSHandler(SourceStackKey.INSTANCE, SourceUnifiedStorageHandler::new);
            CapabilityHelper.registerStackTypedHandler(SourceStackKey.INSTANCE, SourceStackTypedHandler::new);
            StackHandlerWrapperHelper.stackWrappers.put(SourceStackKey.ID, SourceHandlerWrapper::new);
        }

        if (Botania_Loaded)
        {
            // 注册Mana（魔力）
            StackKeyRegistry.registerType(ManaStackKey.INSTANCE);
            CapabilityHelper.BlockCapabilityMap.put(ManaStackKey.ID, BotaniaForgeCapabilities.MANA_RECEIVER);
            CapabilityHelper.ItemCapabilityMap.put(ManaStackKey.ID, BotaniaForgeCapabilities.MANA_ITEM);
            CapabilityHelper.registerUSHandler(ManaStackKey.INSTANCE, ManaUnifiedStorageHandler::new);
            CapabilityHelper.registerStackTypedHandler(ManaStackKey.INSTANCE, ManaStackTypedHandler::new);
            StackHandlerWrapperHelper.stackWrappers.put(ManaStackKey.ID, ManaHandlerWrapper::new);
        }

        // 为维度ME硬盘注册，其中BD_AEPlugin用于注册存储元件
        // BD_AEMEKPlugin与BD_AEFluxPlugin分别注册IStackType与AEKey之间的转换。
        // 物品、流体的转换由AEHelper的静态块负责
        if (AEFluxLoaded)
        {
            BD_AEFluxPlugin.register();
        }
        if (RS_Loaded)
        {
            BD_RSPlugin.register();
        }
        if (RS_MEK_Loaded)
        {
            BD_RSMekPlugin.register();
        }
        if (RSTypesLoaded)
        {
            BD_RSTypesPlugin.register();
        }
        if (AE_IFS_Loaded)
        {
            BD_AE_IFS_Plugin.register();
        }
        if (AE_ARS_Loaded)
        {
            BD_AE_ArsPlugin.register();
        }
        if (AE_Botania_Loaded)
        {
            BD_AEBotaniaPlugin.register();
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
}
