package com.wintercogs.beyonddimensions;

import com.wintercogs.beyonddimensions.DataBase.ButtonState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;


@EventBusSubscriber(modid = BeyondDimensions.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();



    public static final ModConfigSpec.EnumValue<ButtonState> UI_SORT_BUTTON = BUILDER
            .comment("存储UI搜索按钮值 (除非你知道你在做什么，否则不要手动修改)")
            .defineEnum("ui_sort_button", ButtonState.SORT_NAME);

    public static final ModConfigSpec.EnumValue<ButtonState> UI_REVERSE_BUTTON = BUILDER
            .comment("存储UI倒序按钮值 (除非你知道你在做什么，否则不要手动修改)")
            .defineEnum("ui_reverse_button", ButtonState.DISABLED);

    public static final ModConfigSpec.EnumValue<ButtonState> UI_SEARCH_BUTTON = BUILDER
            .comment("存储UI搜索按钮值 (除非你知道你在做什么，否则不要手动修改)")
            .defineEnum("ui_search_button", ButtonState.DISABLED);

    public static final ModConfigSpec.EnumValue<ButtonState> UI_CRAFT_BUTTON = BUILDER
            .comment("决定打开菜单时是否显示合成槽")
            .defineEnum("ui_craft_button", ButtonState.DISABLED);

    public static final ModConfigSpec.EnumValue<ButtonState> UI_CRAFT_RETURN_BUTTON = BUILDER
            .comment("决定工艺菜单关闭时，物品优先转移的方向；启用则优先向存储，关闭则优先向背包")
            .defineEnum("ui_craft_return_button", ButtonState.DISABLED);

    public static final ModConfigSpec.IntValue UI_PAGE_NUM = BUILDER
            .comment("存储UI当前显示的总页数 (除非你知道你在做什么，否则不要手动修改)")
            .defineInRange("ui_page_num", 5, 2, 99);

    public static final ModConfigSpec.ConfigValue<String> UI_SEARCH = BUILDER
            .comment("存储UI搜索框内容 (除非你知道你在做什么，否则不要手动修改)")
            .define("ui_search",
                    "");

    public static ButtonState uiSortButton;
    public static ButtonState uiReverseButton;
    public static ButtonState uiSearchButton;
    public static ButtonState uiCraftButton;
    public static ButtonState uiCraftReturnButton;
    public static int uiPageNum;
    public static String uiSearch;

    // 一定放到最后进行静态初始化
    static final ModConfigSpec SPEC = BUILDER.build();



    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        uiSortButton = UI_SORT_BUTTON.get();
        uiReverseButton = UI_REVERSE_BUTTON.get();
        uiSearchButton = UI_SEARCH_BUTTON.get();
        uiPageNum = UI_PAGE_NUM.get();
        uiSearch = UI_SEARCH.get();
        uiCraftButton = UI_CRAFT_BUTTON.get();
        uiCraftReturnButton = UI_CRAFT_RETURN_BUTTON.get();

    }
}
