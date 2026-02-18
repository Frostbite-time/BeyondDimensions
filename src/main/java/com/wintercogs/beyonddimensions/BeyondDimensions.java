package com.wintercogs.beyonddimensions;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.Chemicals.GasStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.Chemicals.InfusionStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.Chemicals.PigmentStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.Chemicals.SlurryStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.*;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.GasStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.InfusionStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.PigmentStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.SlurryStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.*;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.Chemicals.GasHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.Chemicals.InfusionHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.Chemicals.PigmentHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.Chemicals.SlurryHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.*;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.Chemicals.GasUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.Chemicals.InfusionUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.*;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.PigmentUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.SlurryUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.Api.Registry.CapabilityHelper;
import com.wintercogs.beyonddimensions.Api.Registry.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.Api.Registry.StackKeyRegistry;
import com.wintercogs.beyonddimensions.Block.ModBlocks;
import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.BlockRender.ModBlockRenders;
import com.wintercogs.beyonddimensions.Fluid.ModFluids;
import com.wintercogs.beyonddimensions.Integration.AE.BD_AEPlugin;
import com.wintercogs.beyonddimensions.Integration.AEFlux.BD_AEFluxPlugin;
import com.wintercogs.beyonddimensions.Integration.AEMEK.BD_AEMEKPlugin;
import com.wintercogs.beyonddimensions.Integration.AE_Ars.BD_AE_ArsPlugin;
import com.wintercogs.beyonddimensions.Integration.AE_Botania.BD_AE_BotaniaPlugin;
import com.wintercogs.beyonddimensions.Integration.Ars.BD_ArsCaps;
import com.wintercogs.beyonddimensions.Integration.Botania.BD_BotaniaPlugin;
import com.wintercogs.beyonddimensions.Integration.Botania.HudOverlay.ManaPoolPathwayOverlay;
import com.wintercogs.beyonddimensions.Integration.Curios.BD_CuriosPlugin;
import com.wintercogs.beyonddimensions.Integration.Polymorph.PolymorphPlug;
import com.wintercogs.beyonddimensions.Integration.RS.BD_RSPlugin;
import com.wintercogs.beyonddimensions.Item.ModCreativeModeTabs;
import com.wintercogs.beyonddimensions.Item.ModItems;
import com.wintercogs.beyonddimensions.Registry.UIRegister;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
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
    public static boolean JECharactersLoaded = false;
    public static final String JECharactersModId = "jecharacters";
    public static final String RSModId = "refinedstorage";
    public static boolean RS_Loaded = false;
    public static final String ARS_ModId = "ars_nouveau"; // 新生魔艺-魔源兼容
    public static boolean ARS_Loaded = false;
    public static final String AE_ARS_ModId = "arseng";
    public static boolean AE_ARS_Loaded = false;
    public static final String Botania_ModId = "botania"; // 植物魔法-mana兼容
    public static boolean Botania_Loaded = false;
    public static final String AE_Botania_ModId = "appbot";// 植物魔法-ae附属
    public static boolean AE_Botania_Loaded = false;
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
        UIRegister.register(modEventBus);

        // 注册创造模式菜单
        ModCreativeModeTabs.register(modEventBus);

        // 注册物品
        ModItems.register(modEventBus);

        // 注册方块
        ModBlocks.register(modEventBus);

        // 注册流体
        ModFluids.register(modEventBus);

        // 注册方块实体
        ModBlockEntities.register(modEventBus);

        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            // 注册方块实体渲染
            modEventBus.addListener(ModBlockRenders::onRegisterRenderers);
        }

    }

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
            MinecraftForge.EVENT_BUS.addGenericListener(ItemStack.class, BD_CuriosPlugin::registerCapabilities);
        }
        if (ModList.get().isLoaded(JECharactersModId))
        {
            JECharactersLoaded = true;
        }
        if (ModList.get().isLoaded(ARS_ModId))
        {
            ARS_Loaded = true;
            BD_ArsCaps.registerCapability(MOD_EVENT_BUS);
        }
        if (ModList.get().isLoaded(AE_ARS_ModId))
        {
            AE_ARS_Loaded = true;
        }
        if (ModList.get().isLoaded(Botania_ModId))
        {
            Botania_Loaded = true;
            MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, BD_BotaniaPlugin::attachBlockEntityCaps); // 为网络通道和网络接口手动注册火花附着
        }
        if (ModList.get().isLoaded(AE_Botania_ModId))
        {
            AE_Botania_Loaded = true;
        }
        if (ModList.get().isLoaded(RSModId))
        {
            RS_Loaded = true;
        }
        if (ModList.get().isLoaded(Create_ModId))
        {
            Create_Loaded = true;
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

        if (MekLoaded)
        {
            // 注册化学品堆叠
            StackKeyRegistry.registerType(GasStackKey.EMPTY);
            StackKeyRegistry.registerType(InfusionStackKey.EMPTY);
            StackKeyRegistry.registerType(PigmentStackKey.EMPTY);
            StackKeyRegistry.registerType(SlurryStackKey.EMPTY);
            // 注册化学品方块能力
            CapabilityHelper.BlockCapabilityMap.put(GasStackKey.ID, mekanism.common.capabilities.Capabilities.GAS_HANDLER);
            CapabilityHelper.BlockCapabilityMap.put(InfusionStackKey.ID, mekanism.common.capabilities.Capabilities.INFUSION_HANDLER);
            CapabilityHelper.BlockCapabilityMap.put(PigmentStackKey.ID, mekanism.common.capabilities.Capabilities.PIGMENT_HANDLER);
            CapabilityHelper.BlockCapabilityMap.put(SlurryStackKey.ID, mekanism.common.capabilities.Capabilities.SLURRY_HANDLER);
            // 注册化学品物品能力
            CapabilityHelper.ItemCapabilityMap.put(GasStackKey.ID, mekanism.common.capabilities.Capabilities.GAS_HANDLER);
            CapabilityHelper.ItemCapabilityMap.put(InfusionStackKey.ID, mekanism.common.capabilities.Capabilities.INFUSION_HANDLER);
            CapabilityHelper.ItemCapabilityMap.put(PigmentStackKey.ID, mekanism.common.capabilities.Capabilities.PIGMENT_HANDLER);
            CapabilityHelper.ItemCapabilityMap.put(SlurryStackKey.ID, mekanism.common.capabilities.Capabilities.SLURRY_HANDLER);

            // 注册分化包装
            // 注册网络能力，使得网络通道能暴露对应存储能力 注:能量存储无需注册，单独实现
            CapabilityHelper.registerUSHandler(GasStackKey.EMPTY, GasUnifiedStorageHandler::new);
            CapabilityHelper.registerUSHandler(InfusionStackKey.EMPTY, InfusionUnifiedStorageHandler::new);
            CapabilityHelper.registerUSHandler(PigmentStackKey.EMPTY, PigmentUnifiedStorageHandler::new);
            CapabilityHelper.registerUSHandler(SlurryStackKey.EMPTY, SlurryUnifiedStorageHandler::new);

            // 注册存储分化包装
            CapabilityHelper.registerStackTypedHandler(GasStackKey.EMPTY, GasStackTypedHandler::new);
            CapabilityHelper.registerStackTypedHandler(InfusionStackKey.EMPTY, InfusionStackTypedHandler::new);
            CapabilityHelper.registerStackTypedHandler(PigmentStackKey.EMPTY, PigmentStackTypedHandler::new);
            CapabilityHelper.registerStackTypedHandler(SlurryStackKey.EMPTY, SlurryStackTypedHandler::new);

            // 注册堆叠处理包装
            StackHandlerWrapperHelper.stackWrappers.put(GasStackKey.ID, GasHandlerWrapper::new);
            StackHandlerWrapperHelper.stackWrappers.put(InfusionStackKey.ID, InfusionHandlerWrapper::new);
            StackHandlerWrapperHelper.stackWrappers.put(PigmentStackKey.ID, PigmentHandlerWrapper::new);
            StackHandlerWrapperHelper.stackWrappers.put(SlurryStackKey.ID, SlurryHandlerWrapper::new);

        }

        if (ARS_Loaded)
        {
            // 注册魔源
            StackKeyRegistry.registerType(SourceStackKey.INSTANCE);
            // 自己注册能力作为代替，随后为新生魔艺的方块做包装注册
            CapabilityHelper.BlockCapabilityMap.put(SourceStackKey.ID, BD_ArsCaps.SOURCE_CAP);
            CapabilityHelper.ItemCapabilityMap.put(SourceStackKey.ID, BD_ArsCaps.SOURCE_CAP);
            CapabilityHelper.registerUSHandler(SourceStackKey.INSTANCE, SourceUnifiedStorageHandler::new);
            CapabilityHelper.registerStackTypedHandler(SourceStackKey.INSTANCE, SourceStackTypedHandler::new);
            StackHandlerWrapperHelper.stackWrappers.put(SourceStackKey.ID, SourceHandlerWrapper::new);
        }

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

        // 为维度ME硬盘注册，其中BD_AEPlugin用于注册存储元件
        // BD_AEMEKPlugin与BD_AEFluxPlugin分别注册IStackType与AEKey之间的转换。
        // 物品、流体的转换由AEHelper的静态块负责
        if (AELoaded)
        {
            BD_AEPlugin.register();
        }
        if (AEMEKLoaded)
        {
            BD_AEMEKPlugin.register();
        }
        if (AEFluxLoaded)
        {
            BD_AEFluxPlugin.register();
        }
        if (AE_ARS_Loaded)
        {
            BD_AE_ArsPlugin.register();
        }
        if (AE_Botania_Loaded)
        {
            BD_AE_BotaniaPlugin.register();
        }

        if (RS_Loaded)
        {
            BD_RSPlugin.register();
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

            if (PolymorphLoaded)
            {
                PolymorphPlug.register();
            }
            if (Botania_Loaded)
            {
                MinecraftForge.EVENT_BUS.register(ManaPoolPathwayOverlay.class);
            }
        }
    }
}
