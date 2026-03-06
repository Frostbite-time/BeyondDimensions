package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.ids.BDItemIds;
import com.wintercogs.beyonddimensions.common.item.*;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class BDItems
{
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BDConstants.MODID);

    // 维度创造器
    public static final DeferredItem<@NotNull Item> NET_CREATER = registerItem(BDItemIds.NET_CREATER, NetCreater::new);

    // 网络成员邀请器
    public static final DeferredItem<@NotNull Item> NET_MEMBER_INVITER = registerItem(BDItemIds.NET_MEMBER_INVITER, NetMemberInviter::new);

    // 网络管理员邀请器
    public static final DeferredItem<@NotNull Item> NET_MANAGER_INVITER = registerItem(BDItemIds.NET_MANAGER_INVITER, NetManagerInviter::new);

    // 不稳定时空碎片
    public static final DeferredItem<@NotNull Item> UNSTABLE_SPACE_TIME_FRAGMENT = registerItem(BDItemIds.UNSTABLE_SPACE_TIME_FRAGMENT, UnstableSpaceTimeFragment::new);

    // 稳态时空碎片
    public static final DeferredItem<@NotNull Item> STABLE_SPACE_TIME_FRAGMENT = registerSimpleItem(BDItemIds.STABLE_SPACE_TIME_FRAGMENT);

    // 时空稳定框架
    public static final DeferredItem<@NotNull Item> SPACE_TIME_STABLE_FRAME = registerSimpleItem(BDItemIds.SPACE_TIME_STABLE_FRAME);

    // 破碎的时空结晶
    public static final DeferredItem<@NotNull Item> SHATTERED_SPACE_TIME_CRYSTALLIZATION = registerSimpleItem(BDItemIds.SHATTERED_SPACE_TIME_CRYSTALLIZATION);

    // 时空锭
    public static final DeferredItem<@NotNull Item> SPACE_TIME_BAR = registerSimpleItem(BDItemIds.SPACE_TIME_BAR);

    // 物品终端
    public static final DeferredItem<@NotNull Item> NET_TERMINAL_ITEM = registerItem(BDItemIds.NET_TERMINAL_ITEM, NetTerminalItem::new);

    // 网络赠送符
    public static final DeferredItem<@NotNull Item> NET_GIFTER = registerItem(BDItemIds.NET_GIFTER, NetGifter::new);

    // 网络摧毁符
    public static final DeferredItem<@NotNull Item> NET_DESTROYER = registerItem(BDItemIds.NET_DESTROYER, NetDestroyer::new);

    // 物质压缩球
    public static final DeferredItem<@NotNull Item> MATTER_COMPRESS_BALL = registerItem(BDItemIds.MATTER_COMPRESS_BALL, MatterCompressionBall::new);

    // 网络磁铁
    public static final DeferredItem<@NotNull Item> NET_MAGNET_ITEM = registerItem(BDItemIds.NET_MAGNET_ITEM, NetMagnetItem::new);

    // 网络喂食器
    public static final DeferredItem<@NotNull Item> NET_FEEDER_ITEM = registerItem(BDItemIds.NET_FEEDER_ITEM, NetFeederItem::new);

    // 网络补货器
    public static final DeferredItem<@NotNull Item> NET_RESTOCKER_ITEM = registerItem(BDItemIds.NET_RESTOCKER_ITEM, NetRestockerItem::new);

    // 经验交换棒
    public static final DeferredItem<@NotNull Item> XP_EXCHANGE_ITEM = registerItem(BDItemIds.XP_EXCHANGE_ITEM, XpExchangeItem::new);

    // 测试物品 -----------------------
    // 随机物品生成器
    public static final DeferredItem<@NotNull Item> TEST_ITEM_GENERATE = registerItem(BDItemIds.TEST_ITEM_GENERATE, TestItem_ItemGenerate::new);

    private static <T extends Item> DeferredItem<T> registerItem(String name, Function<Item.Properties, T> factory)
    {
        return ITEMS.registerItem(name, factory);
    }

    private static DeferredItem<@NotNull Item> registerSimpleItem(String name)
    {
        return registerItem(name, Item::new);
    }

    public static void register(IEventBus eventBus)
    {
        ITEMS.register(eventBus);
    }
}
