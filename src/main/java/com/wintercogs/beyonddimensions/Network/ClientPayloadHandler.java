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
import java.util.UUID;


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
                    if(packet.end())
                    {
                        // 数据初始化完成 同步到显示
                        for(StoredItemStack storedItemStack : menu.itemStorage.getItemStorage())
                        {
                            menu.viewerItemStorage.addItem(new StoredItemStack(storedItemStack));
                        }
                        Thread.ofVirtual().start(()->{
                            Minecraft.getInstance().execute(() -> menu.buildIndexList(new ArrayList<>(menu.viewerItemStorage.getItemStorage())));
                        });
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
                    DimensionsItemStorage clientStorage = menu.itemStorage;
                    int i = 0;
                    for(StoredItemStack remoteItem : packet.storedItemStacks())
                    {
                        // 如果当前存储存在此物品
                        if(clientStorage.hasStoredItemStackType(remoteItem))
                        {
                            if(packet.changedCounts().get(i) > 0)
                            {
                                clientStorage.addItem(remoteItem.getItemStack(),packet.changedCounts().get(i));
                            }
                            else // 移除操作务必调用remove方法以移除0存储
                            {
                                clientStorage.removeItem(remoteItem.getItemStack(),-packet.changedCounts().get(i));
                            }
                        }
                        else // 如果当前存储不存在此物品
                        {
                            if(packet.changedCounts().get(i) > 0)
                            {
                                clientStorage.addItem(remoteItem.getItemStack(),packet.changedCounts().get(i));
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
}
