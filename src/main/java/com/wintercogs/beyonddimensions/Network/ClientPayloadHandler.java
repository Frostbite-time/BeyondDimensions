package com.wintercogs.beyonddimensions.Network;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Menu.*;
import com.wintercogs.beyonddimensions.Menu.Slot.AbstractStackTypedSlot;
import com.wintercogs.beyonddimensions.Menu.Slot.SlotGroupSync;
import com.wintercogs.beyonddimensions.Packet.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;
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

                        // 按住shift时锁定排序
                        if(!menu.hasShiftDown)
                            menu.updateViewerStorage();
                        else
                            menu.updateOnlyCountAndNewViewer();
                    }

                }

        );
    }

    public void handleCallSeverClickPacket(final CallSeverClickPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {

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

    public void handleEnergyStoragePacket(final EnergyStoragePacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();

                    if(player.containerMenu instanceof NetEnergyMenu menu)
                    {
                        menu.resumeRemoteUpdates(); // 虽然本地端这个好像没有用处
                        menu.loadStorage(packet.energyCap(), packet.energyStored(),packet.energySpeedState());
                        return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
                    }
                }

        );
    }


    public void handleRecipeFillC2SPacket(final RecipeFillC2SPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {

                }

        );
    }

    public void handleClickTransferCraftButtonPacket(final ClickTransferCraftButtonPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {

                }

        );
    }


    public void handleCraftReturnPacket(final CraftReturnPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();

                    if(player.containerMenu instanceof DimensionsCraftMenu menu)
                    {
                        menu.firstCraftReturnDir = packet.dir();
                    }
                }
        );
    }

    public void handleBatchTransferPacket(final BatchTransferPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {

                }
        );
    }

    public void handlePickBlockFromNetPacket(final PickBlockFromNetPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {

                }
        );
    }

    public void handlePutHandItemToNetPacket(final PutHandItemToNetPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {

                }
        );
    }

    public void handleOrderedStackTypedSlotPacket(final OrderedStackTypedSlotPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();
                    if(player.containerMenu instanceof AbstractContainerMenu menu)
                    {
                        if(menu.slots.get(packet.slotId()) instanceof AbstractStackTypedSlot slot)
                        {
                            slot.loadChange(packet.slotIndex(), packet.stack(), packet.newAmount());
                        }
                    }
                }
        );
    }

    public void handleSetSlotDirectlyPacket(final SetSlotDirectlyPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();
                    if(player.containerMenu instanceof AbstractContainerMenu menu)
                    {
                        if(menu.slots.get(packet.slotId()) instanceof AbstractStackTypedSlot slot)
                        {
                            slot.setStackDirectly(packet.stack());
                        }
                    }
                }
        );
    }

    public void handleDisorderedSlotGroupSyncPacket(final DisorderedSlotGroupSyncPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();
                    if(player.containerMenu instanceof BDBaseMenu menu)
                    {
                        SlotGroupSync sync = menu.slotGroupSyncs.get(packet.groupId());
                        if(sync != null)
                        {
                            sync.loadChange(packet.stacks(),packet.changedCounts());
                            sync.afterLoadChange();

                        }
                    }
                }
        );
    }
}
