package com.wintercogs.beyonddimensions.common.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public class BDFluidTags
{
    public static final TagKey<Fluid> C_EXPERIENCE = FluidTags.create(new ResourceLocation("forge", "experience"));
}
