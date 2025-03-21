package com.wintercogs.beyonddimensions.Network;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.Menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.Menu.NetControlMenu;
import com.wintercogs.beyonddimensions.Menu.NetEnergyMenu;
import com.wintercogs.beyonddimensions.Menu.NetInterfaceBaseMenu;
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

    public void handleStoragePacket(final StoragePacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();
                    if (player.containerMenu instanceof DimensionsNetMenu menu)
                    {
                        for(int i = 0; i<packet.stacks().size(); i++)
                        {
                            // unifiedStorage为基于物品的存储方案，只关心物品种类与数量。因此可以避开索引的远端同步需求
                            UnifiedStorage unifiedStorage = menu.unifiedStorage;
                            unifiedStorage.insert(packet.stacks().get(i),false);
                        }
                        if(packet.end())
                        {
                            //viewerStorage基于unifiedStorage构建。是故其位置与unifiedStorage保持完全的正确。同样避开对索引的远端同步需求
                            menu.updateViewerStorage();
                            menu.buildIndexList(new ArrayList<>(menu.viewerUnifiedStorage.getStorage()));
                            menu.resumeRemoteUpdates();
                        }
                        return; // 当接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
                    }

                    if(player.containerMenu instanceof NetInterfaceBaseMenu menu)
                    {
                        for(int i = 0; i<packet.stacks().size(); i++)
                        {
                            IStackTypedHandler unifiedStorage = menu.storageHandler;
                            if (unifiedStorage.getStorage().size() > packet.indexs().get(i))
                                unifiedStorage.setStackDirectly(packet.indexs().get(i), packet.stacks().get(i));
                            else if(unifiedStorage.getStorage().size() == packet.indexs().get(i))
                                unifiedStorage.addStackToIndexDirectly(packet.indexs().get(i), packet.stacks().get(i));
                            else
                            {
                                //将size到Index-1之间的位置填充为空，然后填充Index位置
                                // 扩展列表直到 targetIndex - 1，并填充 null
                                while (unifiedStorage.getStorage().size() < packet.indexs().get(i)) {
                                    unifiedStorage.addStackDirectly(new ItemStackType(ItemStack.EMPTY));  // 填充空值
                                }
                                unifiedStorage.addStackToIndexDirectly(packet.indexs().get(i), packet.stacks().get(i));
                            }
                        }
                        if(packet.end())
                        {
                            // 收到结束信号，更新视图，重建索引
                            menu.updateViewerStorage();
                            menu.buildIndexList(new ArrayList<>(menu.viewerStorageHandler.getStorage()));
                            menu.resumeRemoteUpdates();
                        }
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

    public void handleSyncItemStoragePacket(final SyncStoragePacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();
                    if (player.containerMenu instanceof DimensionsNetMenu menu)
                    {
                        UnifiedStorage clientStorage = menu.unifiedStorage;
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
                        IStackTypedHandler clientStorage = menu.storageHandler;
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
