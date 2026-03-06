package com.wintercogs.beyonddimensions.integration.module.ars;

import com.hollingsworth.arsnouveau.api.source.ISourceCap;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.integration.module.ars.caps.ItemSourceContentAdp;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

public class BDArsCaps
{
    public static final ItemCapability<ISourceCap, @Nullable Void> ITEM_SOURCE = ItemCapability.createVoid(ResourceLocation.fromNamespaceAndPath(BDConstants.MODID, "caps/ars_source"), ISourceCap.class);

    public static void registerCapability(RegisterCapabilitiesEvent event)
    {
        event.registerItem(ITEM_SOURCE,
                (stack, ctx) -> new ItemSourceContentAdp(stack),
                BlockRegistry.SOURCE_JAR);

        event.registerItem(ITEM_SOURCE,
                (stack, ctx) -> new ItemSourceContentAdp(stack),
                BlockRegistry.CREATIVE_SOURCE_JAR);
    }
}
