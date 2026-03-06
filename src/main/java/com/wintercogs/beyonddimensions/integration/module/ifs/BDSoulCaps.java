package com.wintercogs.beyonddimensions.integration.module.ifs;

import com.buuz135.industrialforegoingsouls.capabilities.ISoulHandler;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.jetbrains.annotations.Nullable;

public class BDSoulCaps
{
    public static final ItemCapability<ISoulHandler, @Nullable Void> ITEM = ItemCapability.createVoid(ResourceLocation.fromNamespaceAndPath(BDConstants.MODID, "caps/warden_soul"), ISoulHandler.class);
}
