package com.wintercogs.beyonddimensions.integration.module.botania;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.util.CapCtx;
import com.wintercogs.beyonddimensions.common.init.BDBlockEntities;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.ItemCapInteractionBlackList;
import com.wintercogs.beyonddimensions.integration.module.botania.storage.ManaStackTypedHandler;
import com.wintercogs.beyonddimensions.integration.module.botania.storage.ManaUnifiedStorageHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import vazkii.botania.common.item.BotaniaItems;

/**
 * 为网络通道和网络接口注册火花附着
 */
public class BDBotaniaPlugin
{
    public static void registerItemCapBlackList()
    {
        ItemCapInteractionBlackList.addToBlackList(BotaniaItems.manaMirror);
    }

    public static void registerCapability(RegisterCapabilitiesEvent event)
    {
        event.registerBlockEntity(
                BotaniaCapabilityCompat.sparkAttachable(),
                BDBlockEntities.NET_PATHWAY_BLOCK_ENTITY.get(),
                (be, side) -> {
                    DimensionsNet net = be.getNet();
                    if (net != null)
                    {
                        return new ManaUnifiedStorageHandler(net.getUnifiedStorage(), new CapCtx(be.getLevel(), be.getBlockPos(), be));
                    }
                    return null;
                }
        );

        event.registerBlockEntity(
                BotaniaCapabilityCompat.sparkAttachable(),
                BDBlockEntities.NET_INTERFACE_BLOCK_ENTITY.get(),
                (be, side) -> {
                    return new ManaStackTypedHandler(be.getStackHandler(), new CapCtx(be.getLevel(), be.getBlockPos(), be));
                }
        );
    }
}
