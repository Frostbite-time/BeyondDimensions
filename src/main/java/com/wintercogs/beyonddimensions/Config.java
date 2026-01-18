package com.wintercogs.beyonddimensions;

import com.wintercogs.beyonddimensions.Api.DataBase.ButtonState;
import com.wintercogs.beyonddimensions.Api.config.CommonConfigRuntime;
import com.wintercogs.beyonddimensions.Api.config.ServerConfigRuntime;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config
{

    public final Config.StartUpConfig startUpConfig = new Config.StartUpConfig();
    public final Config.CommonConfig commonConfig = new Config.CommonConfig();
    public final Config.ServerConfig serverConfig = new Config.ServerConfig();

    public static Config INSTANCE;

    private Config(ModContainer container)
    {
        container.registerConfig(ModConfig.Type.STARTUP, startUpConfig.spec);
        container.registerConfig(ModConfig.Type.COMMON, commonConfig.spec);
        container.registerConfig(ModConfig.Type.SERVER, serverConfig.spec);
        container.getEventBus().addListener((ModConfigEvent.Loading evt) ->
        {
            if (evt.getConfig().getSpec() == commonConfig.spec)
            {
                commonConfig.onLoaded();
            }
            if (evt.getConfig().getSpec() == serverConfig.spec)
            {
                serverConfig.onLoaded();
            }
        });
        container.getEventBus().addListener((ModConfigEvent.Reloading evt) ->
        {
            if (evt.getConfig().getSpec() == commonConfig.spec)
            {
                commonConfig.onLoaded();
            }
            if (evt.getConfig().getSpec() == serverConfig.spec)
            {
                serverConfig.onLoaded();
            }
        });
    }

    public static void register(ModContainer container)
    {
        INSTANCE = new Config(container);
    }

    public static class StartUpConfig
    {
        public final ModConfigSpec spec;

        public StartUpConfig()
        {
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

            this.spec = builder.build();
        }
    }

    public static class CommonConfig
    {
        public final ModConfigSpec spec;

        public final ModConfigSpec.EnumValue<ButtonState> UI_SORT_BUTTON;
        public final ModConfigSpec.EnumValue<ButtonState> UI_SECOND_SORT_BUTTON;
        public final ModConfigSpec.EnumValue<ButtonState> UI_REVERSE_BUTTON;
        public final ModConfigSpec.EnumValue<ButtonState> UI_SEARCH_BUTTON;
        public final ModConfigSpec.EnumValue<ButtonState> UI_CRAFT_BUTTON;
        public final ModConfigSpec.EnumValue<ButtonState> UI_CRAFT_RETURN_BUTTON;
        public final ModConfigSpec.IntValue UI_PAGE_NUM;
        public final ModConfigSpec.ConfigValue<String> UI_SEARCH;
        public final ModConfigSpec.BooleanValue SEARCH_TEXT_WITH_JEI_EMI;

        public final ModConfigSpec.BooleanValue INTERFACE_CAN_RECEIVE_RESOURCE;
        public final ModConfigSpec.BooleanValue INTERFACE_CAN_OUTPUT_RESOURCE;
        public final ModConfigSpec.BooleanValue INTERFACE_CAN_POP_RESOURCE;
        public final ModConfigSpec.IntValue INTERFACE_USABLE_CAPACITY;

        public CommonConfig()
        {
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

            UI_SORT_BUTTON = builder
                    .comment("存储UI搜索按钮值 (除非你知道你在做什么，否则不要手动修改)")
                    .defineEnum("ui_sort_button", ButtonState.SORT_NAME);
            UI_SECOND_SORT_BUTTON = builder
                    .comment("存储UI搜索按钮值 (除非你知道你在做什么，否则不要手动修改)")
                    .defineEnum("ui_second_sort_button", ButtonState.SORT_INSERTED_TIME);
            UI_REVERSE_BUTTON = builder
                    .comment("存储UI倒序按钮值 (除非你知道你在做什么，否则不要手动修改)")
                    .defineEnum("ui_reverse_button", ButtonState.DISABLED);
            UI_SEARCH_BUTTON = builder
                    .comment("存储UI搜索按钮值 (除非你知道你在做什么，否则不要手动修改)")
                    .defineEnum("ui_search_button", ButtonState.DISABLED);
            UI_CRAFT_BUTTON = builder
                    .comment("决定打开菜单时是否显示合成槽")
                    .defineEnum("ui_craft_button", ButtonState.DISABLED);
            UI_CRAFT_RETURN_BUTTON = builder
                    .comment("决定工艺菜单关闭时，物品优先转移的方向；启用则优先向存储，关闭则优先向背包")
                    .defineEnum("ui_craft_return_button", ButtonState.DISABLED);
            UI_PAGE_NUM = builder
                    .comment("存储UI当前显示的总页数 (除非你知道你在做什么，否则不要手动修改)")
                    .defineInRange("ui_page_num", 5, 2, 99);
            UI_SEARCH = builder
                    .comment("存储UI搜索框内容 (除非你知道你在做什么，否则不要手动修改)")
                    .define("ui_search", "");
            SEARCH_TEXT_WITH_JEI_EMI = builder
                    .comment("是否与JEI或EMI同步搜索")
                    .define("search_text_with_jei_emi", true);

            INTERFACE_CAN_RECEIVE_RESOURCE = builder
                    .comment("是否允许网络接口将资源送入网络")
                    .define("interface_can_receive_resource", true);
            INTERFACE_CAN_OUTPUT_RESOURCE = builder
                    .comment("是否允许网络接口从网络提取标记的资源")
                    .define("interface_can_output_resource", true);
            INTERFACE_CAN_POP_RESOURCE = builder
                    .comment("是否允许网络接口将内容物弹出到附近容器")
                    .define("interface_can_pop_resource", true);
            INTERFACE_USABLE_CAPACITY = builder
                    .comment("网络接口有多少个槽位实际可用？")
                    .comment("注意：仅在确定需要时使用，后续版本更新会将其移除并添加其他替代方案，会保证存档兼容。")
                    .defineInRange("interface_usable_capacity", 27, 1, 27);

            this.spec = builder.build();
        }

        public void onLoaded()
        {
            CommonConfigRuntime.uiSortButton = UI_SORT_BUTTON.get();
            CommonConfigRuntime.uiSecondSortButton = UI_SECOND_SORT_BUTTON.get();
            CommonConfigRuntime.uiReverseButton = UI_REVERSE_BUTTON.get();
            CommonConfigRuntime.uiSearchButton = UI_SEARCH_BUTTON.get();
            CommonConfigRuntime.uiCraftButton = UI_CRAFT_BUTTON.get();
            CommonConfigRuntime.uiCraftReturnButton = UI_CRAFT_RETURN_BUTTON.get();
            CommonConfigRuntime.uiPageNum = UI_PAGE_NUM.get();
            CommonConfigRuntime.uiSearch = UI_SEARCH.get();
            CommonConfigRuntime.searchTextWithJEIEMI = SEARCH_TEXT_WITH_JEI_EMI.get();

            CommonConfigRuntime.interfaceCanReceiveResource = INTERFACE_CAN_RECEIVE_RESOURCE.get();
            CommonConfigRuntime.interfaceCanOutputResource = INTERFACE_CAN_OUTPUT_RESOURCE.get();
            CommonConfigRuntime.interfaceCanPopResource = INTERFACE_CAN_POP_RESOURCE.get();
            CommonConfigRuntime.interfaceUsableCapacity = INTERFACE_USABLE_CAPACITY.get();
        }
    }

    public static class ServerConfig
    {
        public final ModConfigSpec spec;

        public final ModConfigSpec.LongValue UNSTABLE_SPACE_TIME_FRAGMENT_TRANSFER_TIME;
        public final ModConfigSpec.IntValue SHATTERED_SPACE_TIME_CRYSTALLIZATION_GENERATE_TIME;

        public ServerConfig()
        {
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

            UNSTABLE_SPACE_TIME_FRAGMENT_TRANSFER_TIME = builder
                    .comment("碎片转化间隔")
                    .defineInRange("fragmentTransferTime", 3600L, 1L, Long.MAX_VALUE);
            SHATTERED_SPACE_TIME_CRYSTALLIZATION_GENERATE_TIME = builder
                    .comment("结晶生成间隔（0代表不生成）")
                    .defineInRange("crystalGenerateTime", 600, 0, Integer.MAX_VALUE);

            this.spec = builder.build();
        }

        public void onLoaded()
        {
            ServerConfigRuntime.fragmentTransferTime = UNSTABLE_SPACE_TIME_FRAGMENT_TRANSFER_TIME.get();
            ServerConfigRuntime.crystalGenerateTime = SHATTERED_SPACE_TIME_CRYSTALLIZATION_GENERATE_TIME.get();
        }
    }
}
