package com.wintercogs.beyonddimensions.Network;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.Menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.Packet.OpenNetGuiPacket;
import com.wintercogs.beyonddimensions.Packet.ScrollGuiPacket;
import com.wintercogs.beyonddimensions.Packet.SearchAndButtonGuiPacket;
import com.wintercogs.beyonddimensions.Packet.SlotIndexPacket;
import net.minecraft.world.entity.player.Player;
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

    public void handleScrollGuiPacket(final ScrollGuiPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {

                }

        );
    }

    public void handleSlotIndexPacket(final SlotIndexPacket packet, final IPayloadContext context)
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
                    menu.loadIndexList(packet.slotIndexList());
                }

        );
    }

    public void handleSearchAndButtonGuiPacket(final SearchAndButtonGuiPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {

                }

        );
    }
}
