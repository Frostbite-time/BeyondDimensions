package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.ids.BDItemIds;
import com.wintercogs.beyonddimensions.common.item.*;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BDItems
{
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BDConstants.MODID);

    // 维度创造器
    public static final RegistryObject<Item> NET_CREATER = ITEMS.register(BDItemIds.NET_CREATER,
            () -> new NetCreater(new Item.Properties()));

    // 网络成员邀请器
    public static final RegistryObject<Item> NET_MEMBER_INVITER = ITEMS.register(BDItemIds.NET_MEMBER_INVITER,
            () -> new NetMemberInviter(new Item.Properties()));

    // 网络管理员邀请器
    public static final RegistryObject<Item> NET_MANAGER_INVITER = ITEMS.register(BDItemIds.NET_MANAGER_INVITER,
            () -> new NetManagerInviter(new Item.Properties()));

    // 不稳定时空碎片
    public static final RegistryObject<Item> UNSTABLE_SPACE_TIME_FRAGMENT = ITEMS.register(BDItemIds.UNSTABLE_SPACE_TIME_FRAGMENT,
            () -> new UnstableSpaceTimeFragment(new Item.Properties()));

    // 稳态时空碎片
    public static final RegistryObject<Item> STABLE_SPACE_TIME_FRAGMENT = ITEMS.register(BDItemIds.STABLE_SPACE_TIME_FRAGMENT,
            () -> new Item(new Item.Properties()));

    // 时空稳定框架
    public static final RegistryObject<Item> SPACE_TIME_STABLE_FRAME = ITEMS.register(BDItemIds.SPACE_TIME_STABLE_FRAME,
            () -> new Item(new Item.Properties()));

    // 破碎的时空结晶
    public static final RegistryObject<Item> SHATTERED_SPACE_TIME_CRYSTALLIZATION = ITEMS.register(BDItemIds.SHATTERED_SPACE_TIME_CRYSTALLIZATION,
            () -> new Item(new Item.Properties()));

    // 时空锭
    public static final RegistryObject<Item> SPACE_TIME_BAR = ITEMS.register(BDItemIds.SPACE_TIME_BAR,
            () -> new Item(new Item.Properties()));

    // 物品终端
    public static final RegistryObject<Item> NET_TERMINAL_ITEM = ITEMS.register(BDItemIds.NET_TERMINAL_ITEM,
            () -> new NetTerminalItem(new Item.Properties()));

    // 网络赠送符
    public static final RegistryObject<Item> NET_GIFTER = ITEMS.register(BDItemIds.NET_GIFTER,
            () -> new NetGifter(new Item.Properties()));

    // 网络摧毁符
    public static final RegistryObject<Item> NET_DESTROYER = ITEMS.register(BDItemIds.NET_DESTROYER,
            () -> new NetDestroyer(new Item.Properties()));

    // 物质压缩球
    public static final RegistryObject<Item> MATTER_COMPRESS_BALL = ITEMS.register(BDItemIds.MATTER_COMPRESS_BALL,
            () -> new MatterCompressionBall(new Item.Properties()));

    // 网络磁铁
    public static final RegistryObject<Item> NET_MAGNET_ITEM = ITEMS.register(BDItemIds.NET_MAGNET_ITEM,
            () -> new NetMagnetItem(new Item.Properties()));

    // 网络喂食器
    public static final RegistryObject<Item> NET_FEEDER_ITEM = ITEMS.register(BDItemIds.NET_FEEDER_ITEM,
            () -> new NetFeederItem(new Item.Properties()));

    // 网络补货器
    public static final RegistryObject<Item> NET_RESTOCKER_ITEM = ITEMS.register(BDItemIds.NET_RESTOCKER_ITEM,
            () -> new NetRestockerItem(new Item.Properties()));

    // 经验交换棒
    public static final RegistryObject<Item> XP_EXCHANGE_ITEM = ITEMS.register(BDItemIds.XP_EXCHANGE_ITEM,
            () -> new XpExchangeItem(new Item.Properties()));

    // 测试物品 -----------------------
    // 随机物品生成器
    public static final RegistryObject<Item> TEST_ITEM_GENERATE = ITEMS.register(BDItemIds.TEST_ITEM_GENERATE,
            () -> new TestItem_ItemGenerate(new Item.Properties()));

    public static void register(IEventBus eventBus)
    {
        ITEMS.register(eventBus);
    }
}
