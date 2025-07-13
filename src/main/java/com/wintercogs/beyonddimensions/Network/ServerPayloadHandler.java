package com.wintercogs.beyonddimensions.Network;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataComponents.Custom.ItemStackContents;
import com.wintercogs.beyonddimensions.DataComponents.ModDataComponents;
import com.wintercogs.beyonddimensions.GUI.NetMenuType;
import com.wintercogs.beyonddimensions.Item.Custom.NetTerminalItem;
import com.wintercogs.beyonddimensions.Menu.*;
import com.wintercogs.beyonddimensions.Packet.*;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
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
                        NetMenuType targetMenu = packet.target();
                        if(targetMenu == NetMenuType.NET_CRAFT_MENU)
                        {
                            player.openMenu(new SimpleMenuProvider(
                                    (containerId, playerInventory, _player) -> new DimensionsCraftMenu(DimensionsCraftMenu.Dimensions_Craft_Menu.get(),containerId, playerInventory, net,null,null),
                                    Component.translatable("menu.title.beyonddimensions.dimensionnetmenu")
                            ));
                        }
                        else if(targetMenu == NetMenuType.NET_MENU)
                        {
                            player.openMenu(new SimpleMenuProvider(
                                    (containerId, playerInventory, _player) -> new DimensionsNetMenu(DimensionsNetMenu.Dimensions_Net_Menu.get(),containerId, playerInventory, net),
                                    Component.translatable("menu.title.beyonddimensions.dimensionnetmenu")
                            ));
                        }
                        else if(targetMenu == NetMenuType.NET_CRAFT_TERMINAL)
                        {
                            ItemStack terminalStack = null;
                            if(player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof NetTerminalItem)
                                terminalStack = player.getItemInHand(InteractionHand.MAIN_HAND);
                            else if(player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof NetTerminalItem)
                                terminalStack = player.getItemInHand(InteractionHand.OFF_HAND);
                            else
                            {
                                for(ItemStack itemStack : player.getInventory().items)
                                {
                                    if(itemStack.getItem() instanceof NetTerminalItem)
                                    {
                                        terminalStack = itemStack;
                                        break;
                                    }

                                }

                                if(terminalStack == null && BeyondDimensions.CuriosLoaded)
                                {
                                    terminalStack = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                                            .flatMap(iCuriosItemHandler ->
                                                    iCuriosItemHandler.findFirstCurio(itemStack ->
                                                            itemStack.getItem() instanceof NetTerminalItem &&
                                                                    itemStack.has(ModDataComponents.NET_ID_DATA) &&
                                                                    itemStack.get(ModDataComponents.NET_ID_DATA) >= 0
                                                    )
                                            )
                                            .map(slotResult -> slotResult.stack())
                                            .orElse(null);
                                }
                            }

                            if(terminalStack != null)
                            {
                                if(terminalStack.get(ModDataComponents.CRAFT_SLOTS)==null)
                                    terminalStack.set(ModDataComponents.CRAFT_SLOTS, new ItemStackContents(NonNullList.withSize(9,ItemStack.EMPTY)));

                                NetTerminalItem.contextMap.put(player, new NetTerminalItem.MenuTriggerContext(InteractionHand.MAIN_HAND, terminalStack));
                                player.openMenu((NetTerminalItem)terminalStack.getItem());
                            }
                        }
                    }

                }
        );

    }

    public void handleItemStoragePacket(final StoragePacket packet, final IPayloadContext context)
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
                        menu.customClickHandler(packet.slotIndex(),packet.clickItem(),packet.button(),packet.shiftDown());
                        menu.broadcastChanges();
                        // 这里发包不是让客户端执行操作，而是解除锁定
                        PacketDistributor.sendToPlayer((ServerPlayer) player,new CallSeverClickPacket(1, new ItemStackType(ItemStack.EMPTY),1,false));
                        return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
                    }
                    if(player.containerMenu instanceof NetInterfaceBaseMenu menu)
                    {
                        menu.customClickHandler(packet.slotIndex(),packet.clickItem(),packet.button(),packet.shiftDown());
                        menu.broadcastChanges();
                        // 这里发包不是让客户端执行操作，而是解除锁定
                        PacketDistributor.sendToPlayer((ServerPlayer) player,new CallSeverClickPacket(1, new ItemStackType(ItemStack.EMPTY),1,false));
                        return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
                    }

                }

        );
    }

    public void handleCallServerPlayerInfoPacket(final CallServerPlayerInfoPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();
                    NetControlMenu menu;
                    if (!(player.containerMenu instanceof NetControlMenu))
                    {
                        return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
                    }
                    menu = (NetControlMenu) player.containerMenu;
                    menu.sendPlayerInfo();
                }

        );
    }

    public void handlePlayerPermissionInfoPacket(final PlayerPermissionInfoPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {

                }

        );
    }

    public void handleNetControlActionPacket(final NetControlActionPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();
                    NetControlMenu menu;
                    if (!(player.containerMenu instanceof NetControlMenu))
                    {
                        return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
                    }
                    menu = (NetControlMenu) player.containerMenu;
                    menu.handlePlayerAction(packet.receiver(),packet.action());
                }

        );
    }

    public void handleSyncFlagPacket(final SyncFlagPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();
                    NetInterfaceBaseMenu menu;
                    if (!(player.containerMenu instanceof NetInterfaceBaseMenu))
                    {
                        return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
                    }
                    menu = (NetInterfaceBaseMenu) player.containerMenu;
                    //menu.handlePlayerAction(packet.receiver(),packet.action());
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
                        menu.be.popMode = packet.popMode();
                        return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
                    }
                    if(player.containerMenu instanceof NetEnergyMenu menu)
                    {
                        menu.popMode = packet.popMode();
                        menu.be.popMode = packet.popMode();
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
                    Player player = context.player();

                    if(player.containerMenu instanceof NetInterfaceBaseMenu menu)
                    {
                        menu.setFlagSlot(packet.index(),packet.clickStack(),packet.flagStack());
                        menu.broadcastChanges();
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
                    //  服务端作为数据来源留空
                }

        );
    }

    public void handleRecipeFillC2SPacket(final RecipeFillC2SPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();

                    if(player.containerMenu instanceof DimensionsCraftMenu menu)
                    {
                        //服务端处理示意
                        //1.解析数组
                        //2.为每一个槽位在背包和存储中寻找资源填入
                        menu.transferRecipe(packet.inputs());


                    }
                }

        );
    }

    public void handleClickTransferCraftButtonPacket(final ClickTransferCraftButtonPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();

                    if(player.containerMenu instanceof DimensionsCraftMenu menu)
                    {
                        //服务端处理示意
                        //1.解析数组
                        //2.为每一个槽位在背包和存储中寻找资源填入
                        menu.cleanCraftSlots(packet.toStorage());


                    }
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
                    if(packet.clickStack() instanceof ItemStackType clickItem)
                    {
                        Player player = context.player();

                        if(player.containerMenu instanceof DimensionsNetMenu menu)
                        {
                            // 批量转移到存储
                            if(packet.dirToStorage())
                            {
                                for(Slot invSlot : menu.slots)
                                {
                                    if(menu.inventoryStartIndex<=invSlot.index&& invSlot.index<menu.inventoryEndIndex)
                                    {
                                        if(ItemStack.isSameItemSameComponents(clickItem.getStack(), invSlot.getItem()))
                                            menu.customClickHandler(invSlot.index, new ItemStackType(invSlot.getItem()), 0, true);
                                    }
                                }
                            }
                            //到背包 暂时留空，以后如果需要再写
                            else
                            {

                            }

                            menu.broadcastChanges();
                        }
                    }
                }
        );
    }

    public void handlePickBlockFromNetPacket(final PickBlockFromNetPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();
                    if(!player.getMainHandItem().isEmpty()) return;
                    DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
                    if(net == null) return;
                    UnifiedStorage storage = net.getUnifiedStorage();

                    ItemStackType target = null;
                    for(IStackType stack : storage.getStorage())
                    {
                        if(stack instanceof ItemStackType itemStackType)
                        {
                            if(itemStackType.getStack().getItem() == packet.targetStack().getItem())
                            {
                                target = (ItemStackType) itemStackType.copyWithCount(itemStackType.getVanillaMaxStackSize());
                                break;
                            }
                        }
                    }

                    if(target != null)
                    {
                        ItemStack extract = ((ItemStackType) storage.extract(target,false)).copyStack();
                        player.setItemInHand(InteractionHand.MAIN_HAND,extract);
                    }
                }
        );
    }

    public void handlePutHandItemToNetPacket(final PutHandItemToNetPacket packet, final IPayloadContext context)
    {
        context.enqueueWork(
                () ->
                {
                    Player player = context.player();
                    if(player.getMainHandItem().isEmpty()) return;
                    DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
                    if(net == null) return;
                    UnifiedStorage storage = net.getUnifiedStorage();
                    IStackType remaining = storage.insert(new ItemStackType(player.getMainHandItem()),false);
                    player.getMainHandItem().setCount((BDMath.clampLongToInt(remaining.getStackAmount())));
                }
        );
    }

}
