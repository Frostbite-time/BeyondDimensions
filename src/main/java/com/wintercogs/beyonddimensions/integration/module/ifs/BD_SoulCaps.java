package com.wintercogs.beyonddimensions.integration.module.ifs;

import com.buuz135.industrialforegoingsouls.capabilities.ISoulHandler;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.jetbrains.annotations.Nullable;

public class BD_SoulCaps
{
    public static final ItemCapability<ISoulHandler, @Nullable Void> ITEM = ItemCapability.createVoid(ResourceLocation.tryBuild(BeyondDimensions.MODID, "caps/warden_soul"), ISoulHandler.class);
}
