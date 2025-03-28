package com.wintercogs.beyonddimensions;

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
import com.wintercogs.beyonddimensions.Integration.Mek.Capability.ChemicalCapabilityHelper;
import com.wintercogs.beyonddimensions.Registry.ShortCutKeyRegister;
import com.wintercogs.beyonddimensions.Registry.StackTypeRegistry;
import com.wintercogs.beyonddimensions.Unit.CapabilityHelper;
import com.wintercogs.beyonddimensions.Unit.StackHandlerWrapperHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.items.CapabilityItemHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

@Mod(modid = BeyondDimensions.MODID, name = "Beyond Dimensions", version = "0.1.6")
public class BeyondDimensions
{
    // 模组主类的单例
    @Mod.Instance(BeyondDimensions.MODID)
    public static BeyondDimensions instance;

    public static final String MODID = "beyonddimensions";
    public static boolean MekLoaded = false; // 用于mek化学品存储
    public static final String MekanismMODID = "mekanism";
    public static boolean AELoaded = false;
    public static final String AE2MODID = "ae2";
    public static boolean EMILoaded = false;
    public static final String EMI_MODID = "emi";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    // mod 类的构造函数是加载 mod 时运行的第一个代码。
    // FML 将识别一些参数类型，如 IEventBus 或 ModContainer 并自动传入它们。
    public BeyondDimensions()
    {
        EventBus modEventBus = MinecraftForge.EVENT_BUS;

        // 1.12.2注册快捷键
        ShortCutKeyRegister.registerKeys();

        // 注册事件
        MinecraftForge.EVENT_BUS.register(this);//注册this类中所有事件

    }

    @Mod.EventHandler
    public void commonSetup(final FMLPostInitializationEvent event)
    {
        List<ModContainer> modList = Loader.instance().getModList();

        for (ModContainer mod : modList)
        {
            if (mod.getModId().equals(MekanismMODID))
                MekLoaded = true;
            if (mod.getModId().equals(AE2MODID))
                AELoaded = true;
            // 1.12.2版本暂无EMI,考虑替换为JEI
        }


        // 注册堆叠类型，使得网络能够存储相关堆叠
        StackTypeRegistry.registerType(new ItemStackType());
        StackTypeRegistry.registerType(new FluidStackType());

        // 注册方块能力类型，用于动态为方块注册能力
        CapabilityHelper.BlockCapabilityMap.put(ItemStackType.ID, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
        CapabilityHelper.BlockCapabilityMap.put(FluidStackType.ID, CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY);

        // 注册网络能力，使得网络通道能暴露对应存储能力 注:能量存储无需注册，单独实现
        UnifiedStorage.typedHandlerMap.put(ItemStackType.ID, ItemUnifiedStorageHandler::new);
        UnifiedStorage.typedHandlerMap.put(FluidStackType.ID, FluidUnifiedStorageHandler::new);

        // 注册存储分化包装
        StackTypedHandler.typedHandlerMap.put(ItemStackType.ID, ItemStackTypedHandler::new);
        StackTypedHandler.typedHandlerMap.put(FluidStackType.ID, FluidStackTypedHandler::new);

        // 注册堆叠处理包装，用于动态包装来自其他模组的handler (如原版的IItemHandler)
        StackHandlerWrapperHelper.stackWrappers.put(ItemStackType.ID, ItemHandlerWrapper::new);
        StackHandlerWrapperHelper.stackWrappers.put(FluidStackType.ID, FluidHandlerWrapper::new);

        if (MekLoaded)
        {
            // 注册化学品堆叠
            StackTypeRegistry.registerType(new ChemicalStackType());
            // 注册化学品方块能力
            CapabilityHelper.BlockCapabilityMap.put(ChemicalStackType.ID, ChemicalCapabilityHelper.CHEMICAL);
            // 注册分化包装
            UnifiedStorage.typedHandlerMap.put(ChemicalStackType.ID, ChemicalUnifiedStorageHandler::new);
            StackTypedHandler.typedHandlerMap.put(ChemicalStackType.ID, ChemicalStackTypedHandler::new);

            // 注册堆叠处理包装
            StackHandlerWrapperHelper.stackWrappers.put(ChemicalStackType.ID, ChemicalHandlerWrapper::new);

        }
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartedEvent event)
    {
        LOGGER.info("维度网络初始化完成(服务端)");
    }


    // 你可以使用EventBusSubscriber来自动注册类中所有标注了@SubscribeEvent的静态方法。
    @Mod.EventBusSubscriber(modid = MODID, value = Side.CLIENT)
    public static class ClientModEvents
    {
        @Mod.EventHandler
        public static void onClientSetup(FMLLoadCompleteEvent event)
        {
            // 一些客户端初始代码
        }
    }
}
