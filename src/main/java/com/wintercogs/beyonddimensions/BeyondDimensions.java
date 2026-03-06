package com.wintercogs.beyonddimensions;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.api.capability.helper.CapabilityHelper;
import com.wintercogs.beyonddimensions.api.capability.helper.ordered.*;
import com.wintercogs.beyonddimensions.api.capability.helper.unordered.*;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.*;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.key.StackKeyRegistry;
import com.wintercogs.beyonddimensions.api.storage.key.impl.*;
import com.wintercogs.beyonddimensions.common.block.entity.NetEnergyPathwayBlockEntity;
import com.wintercogs.beyonddimensions.common.block.entity.NetFurnaceBlockEntity;
import com.wintercogs.beyonddimensions.common.block.entity.NetInterfaceBlockEntity;
import com.wintercogs.beyonddimensions.common.block.entity.NetPathwayBlockEntity;
import com.wintercogs.beyonddimensions.common.init.*;
import com.wintercogs.beyonddimensions.integration.IntegrationManager;
import com.wintercogs.beyonddimensions.integration.module.appars.BD_AE_ArsPlugin;
import com.wintercogs.beyonddimensions.integration.module.appifs.BD_AE_IFS_Plugin;
import com.wintercogs.beyonddimensions.integration.module.ars.BD_ArsCaps;
import com.wintercogs.beyonddimensions.integration.module.botania.BD_BotaniaPlugin;
import com.wintercogs.beyonddimensions.integration.module.botania.Block.ManaPoolPathwayBlockEntity;
import com.wintercogs.beyonddimensions.integration.module.create.blocks.entities.SchematicannonPathWayBlockEntity;
import com.wintercogs.beyonddimensions.integration.module.curios.BD_CuriosPlugin;
import com.wintercogs.beyonddimensions.integration.module.ifs.BD_SoulCaps;
import com.wintercogs.beyonddimensions.integration.module.ifs.Item.WardenSoulTagItem;
import com.wintercogs.beyonddimensions.integration.module.rs.BD_RSPlugin;
import com.wintercogs.beyonddimensions.integration.module.rsmek.BD_RSMekPlugin;
import com.wintercogs.beyonddimensions.integration.module.rstypes.BD_RSTypesPlugin;
import net.minecraft.resources.ResourceLocation;
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

@Mod(BDConstants.MODID)
public class BeyondDimensions
{
    public static IEventBus MOD_EVENT_BUS;

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
        BDMenus.register(modEventBus);
        // 注册创造模式菜单
        BDCreativeModeTabs.register(modEventBus);
        // 注册物品组件
        BDDataComponents.register(modEventBus);
        // 注册物品
        BDItems.register(modEventBus);
        // 注册方块
        BDBlocks.register(modEventBus);
        // 注册流体
        BDFluids.register(modEventBus);
        // 注册方块实体
        BDBlockEntities.register(modEventBus);

        IntegrationManager.bootstrapCommon(modEventBus, NeoForge.EVENT_BUS);
    }

    // 在此阶段检测模组列表
    private void constructMod(final FMLConstructModEvent event)
    {
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
        if (ModList.get().isLoaded(RSTypesModId))
        {
            RSTypesLoaded = true;
        }
        if (ModList.get().isLoaded(Create_ModId))
        {
            Create_Loaded = true;
            MOD_EVENT_BUS.addListener(SchematicannonPathWayBlockEntity::registerCapability);
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
        return ResourceLocation.fromNamespaceAndPath(BDConstants.MODID, path);
    }
}
