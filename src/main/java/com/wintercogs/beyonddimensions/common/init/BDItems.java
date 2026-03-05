package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
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
    public static final DeferredItem<@NotNull Item> NET_CREATER = registerItem("net_creater", NetCreater::new);

    // 网络成员邀请器
    public static final DeferredItem<@NotNull Item> NET_MEMBER_INVITER = registerItem("net_member_inviter", NetMemberInviter::new);

    // 网络管理员邀请器
    public static final DeferredItem<@NotNull Item> NET_MANAGER_INVITER = registerItem("net_manager_inviter", NetManagerInviter::new);

    // 不稳定时空碎片
    public static final DeferredItem<@NotNull Item> UNSTABLE_SPACE_TIME_FRAGMENT = registerItem("unstable_space_time_fragment", UnstableSpaceTimeFragment::new);

    // 稳态时空碎片
    public static final DeferredItem<@NotNull Item> STABLE_SPACE_TIME_FRAGMENT = registerSimpleItem("stable_space_time_fragment");

    // 时空稳定框架
    public static final DeferredItem<@NotNull Item> SPACE_TIME_STABLE_FRAME = registerSimpleItem("space_time_stable_frame");

    // 破碎的时空结晶
    public static final DeferredItem<@NotNull Item> SHATTERED_SPACE_TIME_CRYSTALLIZATION = registerSimpleItem("shattered_space_time_crystallization");

    // 时空锭
    public static final DeferredItem<@NotNull Item> SPACE_TIME_BAR = registerSimpleItem("space_time_bar");

    // 物品终端
    public static final DeferredItem<@NotNull Item> NET_TERMINAL_ITEM = registerItem("net_terminal_item", NetTerminalItem::new);

    // 网络赠送符
    public static final DeferredItem<@NotNull Item> NET_GIFTER = registerItem("net_gifter", NetGifter::new);

    // 网络摧毁符
    public static final DeferredItem<@NotNull Item> NET_DESTROYER = registerItem("net_destroyer", NetDestroyer::new);

    // 物质压缩球
    public static final DeferredItem<@NotNull Item> MATTER_COMPRESS_BALL = registerItem("matter_compress_ball", MatterCompressionBall::new);

    // 网络磁铁
    public static final DeferredItem<@NotNull Item> NET_MAGNET_ITEM = registerItem("net_magnet_item", NetMagnetItem::new);

    // 网络喂食器
    public static final DeferredItem<@NotNull Item> NET_FEEDER_ITEM = registerItem("net_feeder_item", NetFeederItem::new);

    // 网络补货器
    public static final DeferredItem<@NotNull Item> NET_RESTOCKER_ITEM = registerItem("net_restocker_item", NetRestockerItem::new);

    // 经验交换棒
    public static final DeferredItem<@NotNull Item> XP_EXCHANGE_ITEM = registerItem("xp_exchange_item", XpExchangeItem::new);

    // 测试物品 -----------------------
    // 随机物品生成器
    public static final DeferredItem<@NotNull Item> TEST_ITEM_GENERATE = registerItem("test_item_generate", TestItem_ItemGenerate::new);

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
