package com.wintercogs.beyonddimensions;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = BeyondDimensions.MODID)
public class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec SPEC;

    private final static ModConfigSpec.LongValue UNSTABLE_SPACE_TIME_FRAGMENT_TRANSFER_TIME;
    private final static ModConfigSpec.IntValue SHATTERED_SPACE_TIME_CRYSTALLIZATION_GENERATE_TIME;

    static {
        UNSTABLE_SPACE_TIME_FRAGMENT_TRANSFER_TIME =
                BUILDER.comment("碎片转化间隔")
                        .defineInRange("fragmentTransferTime", 3600L, 1L, Long.MAX_VALUE);

        SHATTERED_SPACE_TIME_CRYSTALLIZATION_GENERATE_TIME =
                BUILDER.comment("结晶生成间隔（0代表不生成）")
                        .defineInRange("crystalGenerateTime", 600, 0, Integer.MAX_VALUE);

        SPEC = BUILDER.build();
    }

    public static Long FRAGMENT_TRANSFER_TIME = UNSTABLE_SPACE_TIME_FRAGMENT_TRANSFER_TIME.getDefault();
    public static Integer CRYSTAL_GENERATE_TIME = SHATTERED_SPACE_TIME_CRYSTALLIZATION_GENERATE_TIME.getDefault();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getType() == ModConfig.Type.SERVER) {
            FRAGMENT_TRANSFER_TIME = UNSTABLE_SPACE_TIME_FRAGMENT_TRANSFER_TIME.get();
            CRYSTAL_GENERATE_TIME = SHATTERED_SPACE_TIME_CRYSTALLIZATION_GENERATE_TIME.get();
        }
    }
}
