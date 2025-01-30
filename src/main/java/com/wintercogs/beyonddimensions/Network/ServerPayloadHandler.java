package com.wintercogs.beyonddimensions.Network;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.Menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Packet.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

public class ServerPayloadHandler
{
    private static final Logger LOGGER = LogUtils.getLogger();
    // 实现单例
    private static final ServerPayloadHandler INSTANCE = new ServerPayloadHandler();

    public static ServerPayloadHandler getInstance()
    {
        return INSTANCE;
    }


    public void handleOpenNetGuiPacket(final OpenNetGuiPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    //获取玩家上下文
                    Player player = context.player();

                    DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
                    if (net != null)
                    {
                        LOGGER.info("玩家存在维度空间:{}，尝试打开GUI", net.getId());
                        context.player().openMenu(new SimpleMenuProvider(
                                (containerId, playerInventory, _player) -> new DimensionsNetMenu(containerId, playerInventory, net, new SimpleContainerData(1)),
                                Component.translatable("menu.title.beyonddimensions.dimensionnetmenu")
                        ));
                    } else
                    {
                        LOGGER.info("当前玩家无维度空间，正在创建......");
                        String netId = DimensionsNet.buildNewNetName(player);
                        String numId = netId.replace("BDNet_", "");
                        DimensionsNet newNet = player.getServer().getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(new SavedData.Factory<>(DimensionsNet::create, DimensionsNet::load), netId);
                        newNet.setId(Integer.parseInt(numId));
                        newNet.setOwner(player.getUUID());
                        newNet.addPlayer(player.getUUID());
                        newNet.setDirty();
                        LOGGER.info("成功创建维度空间，id:{}。/n尝试打开GUI", netId);
                    }

                }
        );

    }

    public void handleScrollGuiPacket(final ScrollGuiPacket packet, final IPayloadContext context)
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
                    if (packet.offset() > 0)
                    {
                        menu.lineData--;
                    } else
                    {
                        menu.lineData++;
                    }
                    //ScrollTo会处理lineData小于0的情况 并通知客户端翻页
                    menu.ScrollTo();
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
                        return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
                    }
                    menu = (DimensionsNetMenu) player.containerMenu;
                    menu.loadIndexList(packet.slotIndexList());
                    // 服务端确认索引后将索引送回客户端应用
                    PacketDistributor.sendToPlayer((ServerPlayer) player,packet);
                }

        );
    }

    public void handleSearchAndButtonGuiPacket(final SearchAndButtonGuiPacket packet, final IPayloadContext context)
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
                    menu.loadSearchText(packet.searchText());
                    menu.loadButtonState(packet.buttonStateMap());
                    //menu.buildIndexList();
                }

        );
    }

    public void handleScrollLinedataPacket(final ScrollLinedataPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {

                }

        );
    }

    public void handleItemStoragePacket(final ItemStoragePacket packet, final IPayloadContext context)
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
                    Player player = context.player();
                    DimensionsNetMenu menu;
                    if (!(player.containerMenu instanceof DimensionsNetMenu))
                    {
                        return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
                    }
                    menu = (DimensionsNetMenu) player.containerMenu;
                    menu.sendStorage();
                }

        );
    }

    public void handleSyncItemStoragePacket(final SyncItemStoragePacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {

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
                    menu.customClickHandler(packet.slotIndex(),packet.clickItem(),packet.button(),packet.shiftDown(),false);
                    menu.broadcastChanges();
                    // 这里发包不是让客户端执行操作，而是解除锁定
                    PacketDistributor.sendToPlayer((ServerPlayer) player,new CallSeverClickPacket(1, ItemStack.EMPTY,1,false));
                }

        );
    }
}
