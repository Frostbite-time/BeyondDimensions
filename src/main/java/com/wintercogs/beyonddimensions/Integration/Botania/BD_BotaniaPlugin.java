package com.wintercogs.beyonddimensions.Integration.Botania;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.ManaStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.ManaUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.Api.Util.CapCtx;
import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.Menu.Slot.ItemCapInteractionBlackList;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.common.item.BotaniaItems;

// 为网络通道和网络接口注册火花附着
public class BD_BotaniaPlugin
{
    public static void registerItemCapBlackList()
    {
        ItemCapInteractionBlackList.addToBlackList(BotaniaItems.manaMirror);
    }

    public static void registerCapability(RegisterCapabilitiesEvent event)
    {
        event.registerBlockEntity(
                BotaniaForgeCapabilities.SPARK_ATTACHABLE,
                ModBlockEntities.NET_PATHWAY_BLOCK_ENTITY.get(),
                (be, side) -> {
                    DimensionsNet net = be.getNet();
                    if(net != null)
                    {
                        return new ManaUnifiedStorageHandler(net.getUnifiedStorage(), new CapCtx(be.getLevel(), be.getBlockPos(), be));
                    }
                    return null;
                }
        );

        event.registerBlockEntity(
                BotaniaForgeCapabilities.SPARK_ATTACHABLE,
                ModBlockEntities.NET_INTERFACE_BLOCK_ENTITY.get(),
                (be, side) -> {
                    return new ManaStackTypedHandler(be.getStackHandler(), new CapCtx(be.getLevel(), be.getBlockPos(), be));
                }
        );
    }
}
