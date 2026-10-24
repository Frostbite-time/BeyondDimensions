package com.wintercogs.beyonddimensions;

import com.wintercogs.beyonddimensions.common.menu.widget.StorageSort;
import com.wintercogs.beyonddimensions.config.ClientConfigRuntime;
import com.wintercogs.beyonddimensions.config.ServerConfigRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;

/**
 * 客户端配置存放玩家的界面偏好与状态；服务端配置存放玩法规则，由加载器在玩家进入服务器时同步给客户端。
 * 通用配置暂时没有内容
 */
public class Config
{

    public final Config.ClientConfig clientConfig = new Config.ClientConfig();
    public final Config.CommonConfig commonConfig = new Config.CommonConfig();
    public final Config.ServerConfig serverConfig = new Config.ServerConfig();

    public static Config INSTANCE;

    private Config(ModLoadingContext container, IEventBus modEventBus)
    {
        container.registerConfig(ModConfig.Type.CLIENT, clientConfig.spec);
        container.registerConfig(ModConfig.Type.COMMON, commonConfig.spec);
        container.registerConfig(ModConfig.Type.SERVER, serverConfig.spec);
        // 服务端配置同步到客户端时触发的是 Reloading
        modEventBus.addListener((ModConfigEvent.Loading evt) -> onLoaded(evt.getConfig()));
        modEventBus.addListener((ModConfigEvent.Reloading evt) -> onLoaded(evt.getConfig()));
    }

    private void onLoaded(ModConfig config)
    {
        if (config.getSpec() == clientConfig.spec)
        {
            clientConfig.onLoaded();
        }
        if (config.getSpec() == commonConfig.spec)
        {
            commonConfig.onLoaded();
        }
        if (config.getSpec() == serverConfig.spec)
        {
            serverConfig.onLoaded();
        }
    }

    public static void register(ModLoadingContext container, IEventBus modEventBus)
    {
        INSTANCE = new Config(container, modEventBus);
    }

    public static class ClientConfig
    {
        public final ForgeConfigSpec spec;

        public final ForgeConfigSpec.BooleanValue DISABLE_MULTI_NETWORK_SWITCHING;

        public final ForgeConfigSpec.EnumValue<StorageSort> STORAGE_SORT;
        public final ForgeConfigSpec.BooleanValue STORAGE_USE_SECONDARY_SORT;
        public final ForgeConfigSpec.EnumValue<StorageSort> STORAGE_SECONDARY_SORT;
        public final ForgeConfigSpec.BooleanValue STORAGE_REVERSE;
        public final ForgeConfigSpec.IntValue STORAGE_ROWS;
        public final ForgeConfigSpec.IntValue STORAGE_COLUMNS;
        public final ForgeConfigSpec.BooleanValue STORAGE_KEEP_SEARCH;
        public final ForgeConfigSpec.ConfigValue<String> STORAGE_SEARCH;
        public final ForgeConfigSpec.BooleanValue STORAGE_OPEN_CRAFTING;
        public final ForgeConfigSpec.BooleanValue STORAGE_RIGHT_CLICK_MENU;

        public final ForgeConfigSpec.BooleanValue CRAFTING_RETURN_TO_STORAGE;
        public final ForgeConfigSpec.BooleanValue CRAFTING_KEEP_GRID;

        public final ForgeConfigSpec.BooleanValue RECIPE_VIEWER_SYNC_SEARCH;
        public final ForgeConfigSpec.BooleanValue RECIPE_VIEWER_EMI_NETWORK_CONTENTS;

        public ClientConfig()
        {
            ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

            DISABLE_MULTI_NETWORK_SWITCHING = builder
                    .comment("是否禁用多网络切换功能")
                    .define("disable_multi_network_switching", false);

            builder.comment("存储界面；在界面中改动时会写回这里").push("storage");
            STORAGE_SORT = builder
                    .comment("主排序方式")
                    .defineEnum("sort", StorageSort.NAME);
            STORAGE_USE_SECONDARY_SORT = builder
                    .comment("是否使用次排序")
                    .define("use_secondary_sort", true);
            STORAGE_SECONDARY_SORT = builder
                    .comment("次排序方式，主排序相同时再按它排列")
                    .defineEnum("secondary_sort", StorageSort.INSERTED_TIME);
            STORAGE_REVERSE = builder
                    .comment("是否倒序排列")
                    .define("reverse", false);
            STORAGE_ROWS = builder
                    .comment("首选的行数，界面会按屏幕高度自动收窄")
                    .defineInRange("rows", 5, 2, 99);
            STORAGE_COLUMNS = builder
                    .comment("首选的列数，界面会按屏幕宽度自动收窄")
                    .defineInRange("columns", 9, 9, 99);
            STORAGE_KEEP_SEARCH = builder
                    .comment("关闭界面后是否保留搜索框内容")
                    .define("keep_search", false);
            STORAGE_SEARCH = builder
                    .comment("保留下来的搜索框内容")
                    .define("search", "");
            STORAGE_OPEN_CRAFTING = builder
                    .comment("快捷键是否打开带合成区的界面；在界面中切换合成区时会随之改变")
                    .define("open_crafting", false);
            STORAGE_RIGHT_CLICK_MENU = builder
                    .comment("空手右键存储里的资源时是否打开菜单（取出指定数量、加入分类）；关闭此项则像原版一样取出一半")
                    .define("right_click_menu", true);
            builder.pop();

            builder.comment("合成区").push("crafting");
            CRAFTING_RETURN_TO_STORAGE = builder
                    .comment("关闭界面或清空合成格时，物品优先退回存储；关闭此项则优先退回背包")
                    .define("return_to_storage", false);
            CRAFTING_KEEP_GRID = builder
                    .comment("关闭界面后是否保留合成格里的物品；合成格按玩家保存在服务器上")
                    .define("keep_grid", false);
            builder.pop();

            builder.comment("配方查看器（JEI / EMI）").push("recipe_viewer");
            RECIPE_VIEWER_SYNC_SEARCH = builder
                    .comment("是否与 JEI 或 EMI 的搜索框同步")
                    .define("sync_search", true);
            RECIPE_VIEWER_EMI_NETWORK_CONTENTS = builder
                    .comment("是否允许 EMI 读取维度网络里的全部物品")
                    .define("emi_network_contents", false);
            builder.pop();

            this.spec = builder.build();
        }

        public void onLoaded()
        {
            ClientConfigRuntime.disableMultiNetworkSwitching = DISABLE_MULTI_NETWORK_SWITCHING.get();

            ClientConfigRuntime.storageSort = STORAGE_SORT.get();
            ClientConfigRuntime.storageUseSecondarySort = STORAGE_USE_SECONDARY_SORT.get();
            ClientConfigRuntime.storageSecondarySort = STORAGE_SECONDARY_SORT.get();
            ClientConfigRuntime.storageReverse = STORAGE_REVERSE.get();
            ClientConfigRuntime.storageRows = STORAGE_ROWS.get();
            ClientConfigRuntime.storageColumns = STORAGE_COLUMNS.get();
            ClientConfigRuntime.storageKeepSearch = STORAGE_KEEP_SEARCH.get();
            ClientConfigRuntime.storageSearch = STORAGE_SEARCH.get();
            ClientConfigRuntime.storageOpenCrafting = STORAGE_OPEN_CRAFTING.get();
            ClientConfigRuntime.storageRightClickMenu = STORAGE_RIGHT_CLICK_MENU.get();

            ClientConfigRuntime.craftingReturnToStorage = CRAFTING_RETURN_TO_STORAGE.get();
            ClientConfigRuntime.craftingKeepGrid = CRAFTING_KEEP_GRID.get();

            ClientConfigRuntime.recipeViewerSyncSearch = RECIPE_VIEWER_SYNC_SEARCH.get();
            ClientConfigRuntime.recipeViewerEmiNetworkContents = RECIPE_VIEWER_EMI_NETWORK_CONTENTS.get();
        }
    }

    public static class CommonConfig
    {
        public final ForgeConfigSpec spec;

        public CommonConfig()
        {
            ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

            this.spec = builder.build();
        }

        public void onLoaded()
        {
        }
    }

    public static class ServerConfig
    {
        public final ForgeConfigSpec spec;

        public final ForgeConfigSpec.BooleanValue INTERFACE_RECEIVE;
        public final ForgeConfigSpec.BooleanValue INTERFACE_OUTPUT;
        public final ForgeConfigSpec.BooleanValue INTERFACE_PUSH_OUT;
        public final ForgeConfigSpec.IntValue INTERFACE_CAPACITY;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> INTERFACE_BLOCKED_INPUT_TYPES;

        public final ForgeConfigSpec.LongValue FRAGMENT_TRANSFER_TIME;
        public final ForgeConfigSpec.IntValue CRYSTAL_GENERATE_TIME;

        public ServerConfig()
        {
            ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

            builder.comment("网络接口").push("interface");
            INTERFACE_RECEIVE = builder
                    .comment("是否允许网络接口将资源送入网络")
                    .define("receive", true);
            INTERFACE_OUTPUT = builder
                    .comment("是否允许网络接口从网络提取标记的资源")
                    .define("output", true);
            INTERFACE_PUSH_OUT = builder
                    .comment("是否允许网络接口将内容物弹出到附近容器")
                    .define("push_out", true);
            INTERFACE_CAPACITY = builder
                    .comment("网络接口有多少个槽位实际可用？")
                    .comment("改动只对之后放置或重新载入的接口生效；在线的玩家要重新进入服务器才会拿到新值。")
                    .comment("注意：仅在确定需要时使用，后续版本更新会将其移除并添加其他替代方案，会保证存档兼容。")
                    .defineInRange("capacity", 27, 1, 27);
            INTERFACE_BLOCKED_INPUT_TYPES = builder
                    .comment("禁止网络接口向维度网络输入的资源大类 ID；空列表表示不限制。")
                    .comment("常见资源大类 ID：")
                    .comment("物品：beyonddimensions:stack_type/item")
                    .comment("流体：beyonddimensions:stack_type/fluid")
                    .comment("能量：beyonddimensions:stack_type/energy")
                    .comment("示例：同时禁止流体和能量输入，可填写 [\"beyonddimensions:stack_type/fluid\", \"beyonddimensions:stack_type/energy\"]。")
                    .comment("查找其他资源大类 ID：在对应的 IStackKey 实现中查看 getTypeId() 的返回值，或查找其静态 ID 常量。")
                    .defineListAllowEmpty(
                            "blocked_input_types",
                            List.of(),
                            ServerConfig::isValidResourceLocation
                    );
            builder.pop();

            builder.comment("时空材料").push("space_time");
            FRAGMENT_TRANSFER_TIME = builder
                    .comment("碎片转化间隔")
                    .defineInRange("fragment_transfer_time", 3600L, 1L, Long.MAX_VALUE);
            CRYSTAL_GENERATE_TIME = builder
                    .comment("结晶生成间隔（0代表不生成）")
                    .defineInRange("crystal_generate_time", 600, 0, Integer.MAX_VALUE);
            builder.pop();

            this.spec = builder.build();
        }

        public void onLoaded()
        {
            ServerConfigRuntime.interfaceReceive = INTERFACE_RECEIVE.get();
            ServerConfigRuntime.interfaceOutput = INTERFACE_OUTPUT.get();
            ServerConfigRuntime.interfacePushOut = INTERFACE_PUSH_OUT.get();
            ServerConfigRuntime.interfaceCapacity = INTERFACE_CAPACITY.get();
            ServerConfigRuntime.setInterfaceBlockedInputStackTypes(INTERFACE_BLOCKED_INPUT_TYPES.get());

            ServerConfigRuntime.fragmentTransferTime = FRAGMENT_TRANSFER_TIME.get();
            ServerConfigRuntime.crystalGenerateTime = CRYSTAL_GENERATE_TIME.get();
        }

        private static boolean isValidResourceLocation(Object value)
        {
            return value instanceof String id && ResourceLocation.tryParse(id) != null;
        }
    }
}
