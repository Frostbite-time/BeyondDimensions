package com.wintercogs.beyonddimensions.integration.RS.Tags;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class RSTags
{
    // 富铁石英
    public static final TagKey<Item> RS_QUARTZ_ENRICHED_IRON =
            TagKey.create(Registries.ITEM, ResourceLocation.tryBuild(BeyondDimensions.MODID, "rs/quartz_enriched_iron"));
    // 机器框架
    public static final TagKey<Item> RS_MACHINE_CASING =
            TagKey.create(Registries.ITEM, ResourceLocation.tryBuild(BeyondDimensions.MODID, "rs/machine_casing"));

    public static final String QUARTZ_ENRICHED_IRON_NAME = "quartz_enriched_iron";
    public static final String MACHINE_CASING_NAME = "machine_casing";
}
