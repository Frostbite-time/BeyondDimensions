package com.wintercogs.beyonddimensions.Network;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.Menu.NetControlMenu;
import com.wintercogs.beyonddimensions.Menu.NetEnergyMenu;
import com.wintercogs.beyonddimensions.Menu.NetInterfaceBaseMenu;
import com.wintercogs.beyonddimensions.Packet.*;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;


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

    public void handleStoragePacket(final StoragePacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {

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

    public void handleSyncItemStoragePacket(final SyncStoragePacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();
                    if (player.containerMenu instanceof DimensionsNetMenu menu)
                    {
                        IStackTypedHandler clientStorage = menu.storage;
                        int i = 0;
                        for(IStackType remoteStack : packet.stacks())
                        {
                            // 如果当前存储存在此物品
                            if(clientStorage.hasStackType(remoteStack))
                            {
                                if(packet.changedCounts().get(i) > 0)
                                {
                                    clientStorage.insert(remoteStack.copyWithCount(packet.changedCounts().get(i)),false);
                                }
                                else
                                {
                                    clientStorage.extract(remoteStack.copyWithCount(-packet.changedCounts().get(i)),false);
                                }
                            }
                            else // 如果当前存储不存在此物品
                            {
                                if(packet.changedCounts().get(i) > 0)
                                {
                                    clientStorage.insert(remoteStack.copyWithCount(packet.changedCounts().get(i)),false);
                                }
                            }
                            i++; // 一次遍历完毕后索引自增
                        }
                        menu.updateViewerStorage();
                    }
                    if(player.containerMenu instanceof NetInterfaceBaseMenu menu)
                    {
                        IStackTypedHandler clientStorage = menu.storage;
                        int i = 0;
                        for(IStackType remoteStack : packet.stacks())
                        {
                            if(packet.changedCounts().get(i) > 0)
                            {
                                clientStorage.insert(packet.targetIndex().get(i),remoteStack.copyWithCount(packet.changedCounts().get(i)),false);
                            }
                            else
                            {
                                clientStorage.extract(packet.targetIndex().get(i),-packet.changedCounts().get(i),false);
                            }
                            i++; // 一次遍历完毕后索引自增
                        }

                        menu.updateViewerStorage();
                    }

                }

        );
    }

    public void handleCallSeverClickPacket(final CallSeverClickPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();
                    if (player.containerMenu instanceof DimensionsNetMenu menu)
                    {
                        menu.isHanding = false;
                        return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
                    }
                    if(player.containerMenu instanceof NetInterfaceBaseMenu menu)
                    {
                        menu.isHanding = false;
                    }

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

    public void handleSyncFlagPacket(final SyncFlagPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {

                    Player player = context.player();
                    if(player.containerMenu instanceof NetInterfaceBaseMenu menu)
                    {
                        IStackTypedHandler clientStorage = menu.flagStorage;
                        int i = 0;
                        for(IStackType remoteStack : packet.flags())
                        {
                            clientStorage.setStackDirectly(packet.targetIndex().get(i),remoteStack.copyWithCount(1));
                            i++; // 一次遍历完毕后索引自增
                        }

                        menu.updateViewerStorage();
                    }


                }

        );
    }

    public void handlePopModeButtonPacket(final PopModeButtonPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();

                    if(player.containerMenu instanceof NetInterfaceBaseMenu menu)
                    {
                        menu.popMode = packet.popMode();
                        return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
                    }
                    if(player.containerMenu instanceof NetEnergyMenu menu)
                    {
                        menu.popMode = packet.popMode();
                        return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
                    }
                }

        );
    }

    public void handleFlagSlotSetPacket(final FlagSlotSetPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {

                }

        );
    }

    public void handleEnergyStoragePacket(final EnergyStoragePacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();

                    if(player.containerMenu instanceof NetEnergyMenu menu)
                    {
                        menu.resumeRemoteUpdates(); // 虽然本地端这个好像没有用处
                        menu.loadStorage(packet.energyCap(), packet.energyStored());
                        return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
                    }
                }

        );
    }
}
