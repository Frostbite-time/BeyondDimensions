package com.wintercogs.beyonddimensions.Item;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Integration.AE.Item.NetAEStorageCell;
import com.wintercogs.beyonddimensions.Integration.IFS.Item.WardenSoulTagItem;
import com.wintercogs.beyonddimensions.Item.Custom.*;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems
{
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BeyondDimensions.MODID);

    // 维度创造器
    public static final DeferredItem<Item> NET_CREATER = ITEMS.register("net_creater",
            () -> new NetCreater(new Item.Properties()));

    // 网络成员邀请器
    public static final DeferredItem<Item> NET_MEMBER_INVITER = ITEMS.register("net_member_inviter",
            () -> new NetMemberInviter(new Item.Properties()));

    // 网络管理员邀请器
    public static final DeferredItem<Item> NET_MANAGER_INVITER = ITEMS.register("net_manager_inviter",
            () -> new NetManagerInviter(new Item.Properties()));

    // 不稳定时空碎片
    public static final DeferredItem<Item> UNSTABLE_SPACE_TIME_FRAGMENT = ITEMS.register("unstable_space_time_fragment",
            () -> new UnstableSpaceTimeFragment(new Item.Properties()));

    // 稳态时空碎片
    public static final DeferredItem<Item> STABLE_SPACE_TIME_FRAGMENT = ITEMS.register("stable_space_time_fragment",
            () -> new Item(new Item.Properties()));

    // 时空稳定框架
    public static final DeferredItem<Item> SPACE_TIME_STABLE_FRAME = ITEMS.register("space_time_stable_frame",
            () -> new Item(new Item.Properties()));

    // 破碎的时空结晶
    public static final DeferredItem<Item> SHATTERED_SPACE_TIME_CRYSTALLIZATION = ITEMS.register("shattered_space_time_crystallization",
            () -> new Item(new Item.Properties()));

    // 时空锭
    public static final DeferredItem<Item> SPACE_TIME_BAR = ITEMS.register("space_time_bar",
            () -> new Item(new Item.Properties()));

    // 物品终端
    public static final DeferredItem<Item> NET_TERMINAL_ITEM = ITEMS.register("net_terminal_item",
            () -> new NetTerminalItem(new Item.Properties()));

    // 网络赠送符
    public static final DeferredItem<Item> NET_GIFTER = ITEMS.register("net_gifter",
            () -> new NetGifter(new Item.Properties()));

    // 网络摧毁符
    public static final DeferredItem<Item> NET_DESTROYER = ITEMS.register("net_destroyer",
            () -> new NetDestroyer(new Item.Properties()));

    // 物质压缩球
    public static final DeferredItem<Item> MATTER_COMPRESS_BALL = ITEMS.register("matter_compress_ball",
            () -> new MatterCompressionBall(new Item.Properties()));

    // 网络磁铁
    public static final DeferredItem<Item> NET_MAGNET_ITEM = ITEMS.register("net_magnet_item",
            () -> new NetMagnetItem(new Item.Properties()));

    // 网络喂食器
    public static final DeferredItem<Item> NET_FEEDER_ITEM = ITEMS.register("net_feeder_item",
            () -> new NetFeederItem(new Item.Properties()));

    // 经验交换棒
    public static final DeferredItem<Item> XP_EXCHANGE_ITEM = ITEMS.register("xp_exchange_item",
            () -> new XpExchangeItem(new Item.Properties()));

    // 工业先锋：灵魂涌动联动 - 灵魂标记器
    public static final DeferredItem<Item> WARDEN_SOUL_TAG_ITEM = ITEMS.register("warden_soul_tag_item",
            () -> {
                if (BeyondDimensions.IFS_Loaded)
                    return new WardenSoulTagItem(new Item.Properties());
                else
                    return new Item(new Item.Properties());
            }
    );

    // AE存储磁盘 对于一个没有安装AE的游戏
    public static final DeferredItem<Item> NET_AE_STORAGE_CELL = ITEMS.register("net_ae_storage_cell",
            () -> {
                if (BeyondDimensions.AELoaded)
                    return new NetAEStorageCell(new Item.Properties());
                else
                    return new Item(new Item.Properties());
            }
    );

    // 测试物品 -----------------------
    // 随机物品生成器
    public static final DeferredItem<Item> TEST_ITEM_GENERATE = ITEMS.register("test_item_generate",
            () -> new TestItem_ItemGenerate(new Item.Properties()));

    public static void register(IEventBus eventBus)
    {
        ITEMS.register(eventBus);
    }
}
