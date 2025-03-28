package com.wintercogs.beyonddimensions.Item;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Item.Custom.NetCreater;
import com.wintercogs.beyonddimensions.Item.Custom.NetManagerInviter;
import com.wintercogs.beyonddimensions.Item.Custom.NetMemberInviter;
import com.wintercogs.beyonddimensions.Item.Custom.UnstableSpaceTimeFragment;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber
public class ModItems {

    // 维度创造器
    public static final NetCreater NET_CREATER = (NetCreater) new NetCreater()
            .setRegistryName(BeyondDimensions.MODID, "net_creater")
            .setTranslationKey("net_creater")
            .setCreativeTab(ModCreativeModeTabs.ITEMS_TAB);

    // 网络成员邀请器
    public static final NetMemberInviter NET_MEMBER_INVITER = (NetMemberInviter) new NetMemberInviter()
            .setRegistryName(BeyondDimensions.MODID, "net_member_inviter")
            .setTranslationKey("net_member_inviter")
            .setCreativeTab(ModCreativeModeTabs.ITEMS_TAB);

    // 网络管理员邀请器
    public static final NetManagerInviter NET_MANAGER_INVITER = (NetManagerInviter) new NetManagerInviter()
            .setRegistryName(BeyondDimensions.MODID, "net_manager_inviter")
            .setTranslationKey("net_manager_inviter")
            .setCreativeTab(ModCreativeModeTabs.ITEMS_TAB);

    // 不稳定时空碎片
    public static final UnstableSpaceTimeFragment UNSTABLE_SPACE_TIME_FRAGMENT = (UnstableSpaceTimeFragment) new UnstableSpaceTimeFragment()
            .setRegistryName(BeyondDimensions.MODID, "unstable_space_time_fragment")
            .setTranslationKey("unstable_space_time_fragment")
            .setCreativeTab(ModCreativeModeTabs.ITEMS_TAB);

    // 稳态时空碎片
    public static final Item STABLE_SPACE_TIME_FRAGMENT = new Item()
            .setRegistryName(BeyondDimensions.MODID, "stable_space_time_fragment")
            .setTranslationKey("stable_space_time_fragment")
            .setCreativeTab(ModCreativeModeTabs.ITEMS_TAB);

    // 时空稳定框架
    public static final Item SPACE_TIME_STABLE_FRAME = new Item()
            .setRegistryName(BeyondDimensions.MODID, "space_time_stable_frame")
            .setTranslationKey("space_time_stable_frame")
            .setCreativeTab(ModCreativeModeTabs.ITEMS_TAB);

    // 破碎的时空结晶
    public static final Item SHATTERED_SPACE_TIME_CRYSTALLIZATION = new Item()
            .setRegistryName(BeyondDimensions.MODID, "shattered_space_time_crystallization")
            .setTranslationKey("shattered_space_time_crystallization")
            .setCreativeTab(ModCreativeModeTabs.ITEMS_TAB);

    // 时空锭
    public static final Item SPACE_TIME_BAR = new Item()
            .setRegistryName(BeyondDimensions.MODID, "space_time_bar")
            .setTranslationKey("space_time_bar")
            .setCreativeTab(ModCreativeModeTabs.ITEMS_TAB);

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
                NET_CREATER,
                NET_MEMBER_INVITER,
                NET_MANAGER_INVITER,
                UNSTABLE_SPACE_TIME_FRAGMENT,
                STABLE_SPACE_TIME_FRAGMENT,
                SPACE_TIME_STABLE_FRAME,
                SHATTERED_SPACE_TIME_CRYSTALLIZATION,
                SPACE_TIME_BAR
        );
    }

    // 如果需要模型注册
//    @SubscribeEvent
//    public static void registerModels(ModelRegistryEvent event) {
//        registerModel(NET_CREATER);
//        registerModel(NET_MEMBER_INVITER);
//        registerModel(NET_MANAGER_INVITER);
//        registerModel(UNSTABLE_SPACE_TIME_FRAGMENT);
//        registerModel(STABLE_SPACE_TIME_FRAGMENT);
//        registerModel(SPACE_TIME_STABLE_FRAME);
//        registerModel(SHATTERED_SPACE_TIME_CRYSTALLIZATION);
//        registerModel(SPACE_TIME_BAR);
//    }
//
//    private static void registerModel(Item item) {
//        ModelLoader.setCustomModelResourceLocation(
//                item,
//                0,
//                new ModelResourceLocation(item.getRegistryName(), "inventory")
//        );
//    }
}
