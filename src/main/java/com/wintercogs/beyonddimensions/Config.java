package com.wintercogs.beyonddimensions;

import com.wintercogs.beyonddimensions.DataBase.ButtonState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;


@EventBusSubscriber(modid = BeyondDimensions.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();



    public static final ModConfigSpec.EnumValue<ButtonState> UI_SORT_BUTTON = BUILDER
            .comment("存储UI搜索按钮值 (除非你知道你在做什么，否则不要手动修改)")
            .defineEnum("ui_sort_button", ButtonState.SORT_DEFAULT);

    public static final ModConfigSpec.EnumValue<ButtonState> UI_REVERSE_BUTTON = BUILDER
            .comment("存储UI倒序按钮值 (除非你知道你在做什么，否则不要手动修改)")
            .defineEnum("ui_reverse_button", ButtonState.DISABLED);

    public static final ModConfigSpec.IntValue UI_PAGE_NUM = BUILDER
            .comment("存储UI当前显示的总页数 (除非你知道你在做什么，否则不要手动修改)")
            .defineInRange("ui_page_num", 5, 2, 99);

    public static ButtonState uiSortButton;
    public static ButtonState uiReverseButton;
    public static int uiPageNum;

    static final ModConfigSpec SPEC = BUILDER.build();



    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        uiSortButton = UI_SORT_BUTTON.get();
        uiReverseButton = UI_REVERSE_BUTTON.get();
        uiPageNum = UI_PAGE_NUM.get();

    }
}
