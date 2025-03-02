package com.wintercogs.beyonddimensions.Network;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.DataBase.Storage.ItemStorage;
import com.wintercogs.beyonddimensions.Menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.Menu.NetControlMenu;
import com.wintercogs.beyonddimensions.Packet.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

import java.util.ArrayList;


public class ClientPayloadHandler
{
    private static final Logger LOGGER = LogUtils.getLogger();

    // 实现单例
    private static final ClientPayloadHandler INSTANCE = new ClientPayloadHandler();

    public static ClientPayloadHandler getInstance() {
        return INSTANCE;
    }



    public void handleOpenNetGuiPacket(final OpenNetGuiPacket packet,final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    LOGGER.info(packet.uuid());
                }
        );
    }

    public void handleItemStoragePacket(final ItemStoragePacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();
                    DimensionsNetMenu menu;
                    if (!(player.containerMenu instanceof DimensionsNetMenu))
                    {
                        return; // 当接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
                    }
                    menu = (DimensionsNetMenu) player.containerMenu;
                    for(int i = 0; i<packet.itemStacks().size(); i++)
                    {
                        ItemStorage itemStorage = menu.itemStorage;
                        if (itemStorage.getStorage().size() > packet.indexs().get(i))
                            itemStorage.getStorage().set(packet.indexs().get(i), packet.itemStacks().get(i));
                        else if(itemStorage.getStorage().size() == packet.indexs().get(i))
                            itemStorage.getStorage().add(packet.indexs().get(i), packet.itemStacks().get(i));
                        else
                        {
                            //将size到Index-1之间的位置填充为空，然后填充Index位置
                            // 扩展列表直到 targetIndex - 1，并填充 null
                            while (itemStorage.getStorage().size() < packet.indexs().get(i)) {
                                itemStorage.getStorage().add(ItemStack.EMPTY);  // 填充空值
                            }
                            itemStorage.getStorage().add(packet.indexs().get(i), packet.itemStacks().get(i));
                        }
                    }
                    if(packet.end())
                    {
                        // 收到结束信号，更新视图，重建索引
                        menu.updateViewerStorage();
                        menu.buildIndexList(new ArrayList<>(menu.viewerItemStorage.getStorage()));
                        menu.resumeRemoteUpdates();
                    }
                }

        );
    }

    public void handleCallSeverStoragePacket(final CallSeverStoragePacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {

                }

        );
    }

    public void handleSyncItemStoragePacket(final SyncItemStoragePacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();
                    DimensionsNetMenu menu;
                    if (!(player.containerMenu instanceof DimensionsNetMenu))
                    {
                        return; // 当接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
                    }
                    menu = (DimensionsNetMenu) player.containerMenu;
                    ItemStorage clientStorage = menu.itemStorage;
                    int i = 0;
                    for(ItemStack remoteItem : packet.itemStacks())
                    {
                        // 如果当前存储存在此物品
                        if(clientStorage.hasItemStackType(remoteItem))
                        {
                            if(packet.changedCounts().get(i) > 0)
                            {
                                clientStorage.insertItem(remoteItem.copyWithCount(packet.changedCounts().get(i)),false);
                            }
                            else
                            {
                                clientStorage.extractItem(remoteItem.copyWithCount(-packet.changedCounts().get(i)),false);
                            }
                        }
                        else // 如果当前存储不存在此物品
                        {
                            if(packet.changedCounts().get(i) > 0)
                            {
                                clientStorage.insertItem(remoteItem.copyWithCount(packet.changedCounts().get(i)),false);
                            }
                        }
                        i++; // 一次遍历完毕后索引自增
                    }
                    menu.updateViewerStorage();
                }

        );
    }

    public void handleCallSeverClickPacket(final CallSeverClickPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();
                    DimensionsNetMenu menu;
                    if (!(player.containerMenu instanceof DimensionsNetMenu))
                    {
                        return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
                    }
                    menu = (DimensionsNetMenu) player.containerMenu;
                    menu.isHanding = false;
                }

        );
    }

    public void handleCallServerPlayerInfoPacket(final CallServerPlayerInfoPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {

                }

        );
    }

    public void handlePlayerPermissionInfoPacket(final PlayerPermissionInfoPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();
                    NetControlMenu menu;
                    if (!(player.containerMenu instanceof NetControlMenu))
                    {
                        return;
                    }
                    menu = (NetControlMenu) player.containerMenu;
                    menu.loadPlayerInfo(packet.infoMap());
                }

        );
    }

    public void handleNetControlActionPacket(final NetControlActionPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {

                }

        );
    }
}
