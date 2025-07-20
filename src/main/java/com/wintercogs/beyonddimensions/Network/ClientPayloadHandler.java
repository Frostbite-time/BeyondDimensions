package com.wintercogs.beyonddimensions.Network;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.Menu.BDBaseMenu;
import com.wintercogs.beyonddimensions.Menu.NetControlMenu;
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

    public void handleCallSeverClickPacket(final CallSeverClickPacket packet, final IPayloadContext context)
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

    public void handleQuickDataTagPacket(final QuickDataTagPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();
                    if(player.containerMenu instanceof BDBaseMenu menu)
                    {
                        menu.readQuickDataTag(packet.tag());
                    }
                }
        );
    }

    public void handleToggleMagnetPacket(final ToggleMagnetPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {

                }
        );
    }
}
