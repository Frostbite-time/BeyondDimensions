package com.wintercogs.beyonddimensions.Network;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.DataBase.DimensionsItemStorage;
import com.wintercogs.beyonddimensions.DataBase.StoredItemStack;
import com.wintercogs.beyonddimensions.Menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.Packet.*;
import net.minecraft.client.Minecraft;
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

    public void handleScrollLinedataPacket(final ScrollLinedataPacket packet, final IPayloadContext context)
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
                    menu.lineData = packet.lineData();
                    menu.maxLineData = packet.maxLineData();
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
                        LOGGER.info("客户端报告：未检测到菜单");
                        return; // 当接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
                    }
                    menu = (DimensionsNetMenu) player.containerMenu;
                    for(int i = 0;i<packet.storedItemStacks().size();i++)
                    {
                        DimensionsItemStorage itemStorage = menu.itemStorage;
                        if (itemStorage.getItemStorage().size() > packet.indexs().get(i))
                            itemStorage.getItemStorage().set(packet.indexs().get(i), packet.storedItemStacks().get(i));
                        else if(itemStorage.getItemStorage().size() == packet.indexs().get(i))
                            itemStorage.getItemStorage().add(packet.indexs().get(i), packet.storedItemStacks().get(i));
                        else
                        {
                            //将size到Index-1之间的位置填充为空，然后填充Index位置
                            // 扩展列表直到 targetIndex - 1，并填充 null
                            while (itemStorage.getItemStorage().size() < packet.indexs().get(i)) {
                                itemStorage.getItemStorage().add(new StoredItemStack(ItemStack.EMPTY));  // 填充空值
                            }
                            itemStorage.getItemStorage().add(packet.indexs().get(i), packet.storedItemStacks().get(i));
                        }
                    }
                    LOGGER.info("客户端报告：当前存储大小:{}",menu.itemStorage.getItemStorage().size());
                    if(packet.end())
                    {
                        Thread.ofVirtual().start(()->{
                            Minecraft.getInstance().execute(menu::buildIndexList);
                        });
                        //menu.buildIndexList();
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
}
