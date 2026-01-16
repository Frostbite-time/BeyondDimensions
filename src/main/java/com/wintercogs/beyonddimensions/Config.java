package com.wintercogs.beyonddimensions;

import com.wintercogs.beyonddimensions.Api.DataBase.ButtonState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = BeyondDimensions.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();


    public static final ModConfigSpec.EnumValue<ButtonState> UI_SORT_BUTTON = BUILDER
            .comment("存储UI搜索按钮值 (除非你知道你在做什么，否则不要手动修改)")
            .defineEnum("ui_sort_button", ButtonState.SORT_NAME);

    public static final ModConfigSpec.EnumValue<ButtonState> UI_SECOND_SORT_BUTTON = BUILDER
            .comment("存储UI搜索按钮值 (除非你知道你在做什么，否则不要手动修改)")
            .defineEnum("ui_second_sort_button", ButtonState.SORT_INSERTED_TIME);

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

    public static final ModConfigSpec.BooleanValue INTERFACE_CAN_RECEIVE_RESOURCE = BUILDER
            .comment("是否允许网络接口将资源送入网络")
            .define("interface_can_receive_resource", true);

    public static final ModConfigSpec.BooleanValue INTERFACE_CAN_OUTPUT_RESOURCE = BUILDER
            .comment("是否允许网络接口从网络提取标记的资源")
            .define("interface_can_output_resource", true);

    public static final ModConfigSpec.BooleanValue INTERFACE_CAN_POP_RESOURCE = BUILDER
            .comment("是否允许网络接口将内容物弹出到附近容器")
            .define("interface_can_pop_resource", true);

    public static final ModConfigSpec.IntValue INTERFACE_USABLE_CAPACITY = BUILDER
            .comment("网络接口有多少个槽位实际可用？")
            .comment("注意：仅在确定需要时使用，后续版本更新会将其移除并添加其他替代方案，会保证存档兼容。")
            .defineInRange("interface_usable_capacity", 27, 1, 27);

    public static final ModConfigSpec.BooleanValue SEARCH_TEXT_WITH_JEI_EMI = BUILDER
            .comment("是否与JEI或EMI同步搜索")
            .define("search_text_with_jei_emi", true);

    public static ButtonState uiSortButton;
    public static ButtonState uiSecondSortButton;
    public static ButtonState uiReverseButton;
    public static ButtonState uiSearchButton;
    public static ButtonState uiCraftButton;
    public static ButtonState uiCraftReturnButton;
    public static int uiPageNum;
    public static String uiSearch;
    public static boolean interfaceCanReceiveResource;
    public static boolean interfaceCanOutputResource;
    public static boolean interfaceCanPopResource;
    public static int interfaceUsableCapacity;
    public static boolean searchTextWithJEIEMI;

    // 一定放到最后进行静态初始化
    static final ModConfigSpec SPEC = BUILDER.build();


    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getType() == ModConfig.Type.COMMON) {
            uiSortButton = UI_SORT_BUTTON.get() == ButtonState.SORT_DEFAULT ? ButtonState.SORT_NAME : UI_SORT_BUTTON.get();
            uiSecondSortButton = UI_SECOND_SORT_BUTTON.get();
            uiReverseButton = UI_REVERSE_BUTTON.get();
            uiSearchButton = UI_SEARCH_BUTTON.get();
            uiPageNum = UI_PAGE_NUM.get();
            uiSearch = UI_SEARCH.get();
            uiCraftButton = UI_CRAFT_BUTTON.get();
            uiCraftReturnButton = UI_CRAFT_RETURN_BUTTON.get();
            interfaceCanReceiveResource = INTERFACE_CAN_RECEIVE_RESOURCE.get();
            interfaceCanOutputResource = INTERFACE_CAN_OUTPUT_RESOURCE.get();
            interfaceCanPopResource = INTERFACE_CAN_POP_RESOURCE.get();
            interfaceUsableCapacity = INTERFACE_USABLE_CAPACITY.get();
            searchTextWithJEIEMI = SEARCH_TEXT_WITH_JEI_EMI.get();
        }
    }
}
