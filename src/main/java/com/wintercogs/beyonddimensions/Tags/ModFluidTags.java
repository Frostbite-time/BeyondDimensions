package com.wintercogs.beyonddimensions.Tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public class ModFluidTags
{
    public static final TagKey<Fluid> C_EXPERIENCE =
            TagKey.create(Registries.FLUID, ResourceLocation.tryBuild("c", "experience"));
}
